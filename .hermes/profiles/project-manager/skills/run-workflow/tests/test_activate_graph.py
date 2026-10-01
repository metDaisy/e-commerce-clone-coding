import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
SKILLS_DIR = SCRIPT_DIR.parent.parent
sys.path.insert(0, str(SCRIPT_DIR.parent / "scripts"))
sys.path.insert(0, str(SKILLS_DIR / "create-triage" / "scripts"))

from activate_graph import activate
from triage import freeze, template


class ActivateGraphTest(unittest.TestCase):
    def triage_body(self):
        body = template(
            138,
            "G1-Issue138-Triage",
            "https://github.com/example/repo/issues/138",
            "a" * 40,
            "b" * 40,
            ["docs/requirement/p2/index.md#issue-138"],
            ["docs/current-state.md#p2"],
        )
        body.update(
            {
                "goal": "Seller flow behavior is ready for task graph authoring.",
                "scope": ["P2 Seller behavior"],
                "out_of_scope": ["Unrelated P2 flows"],
                "current_behavior": "Current state is summarized by the snapshot.",
                "desired_behavior": "Requirement behavior is materialized into a task graph.",
                "implementation_idea": "Create vertical slices after document review.",
                "candidate_task_slices": ["Seller API slice"],
                "verification_direction": ["Focused integration test"],
            }
        )
        for document, entry in body["document_impact"].items():
            entry["decision"] = "no-change"
            entry["locators"] = [f"docs/{document.lower()}.md#reviewed"]
            entry["reason"] = f"{document} was reviewed and needs no update."
        return freeze(body)

    def graph_and_readback(self):
        fixture = SKILLS_DIR / "build-task-graph" / "tests" / "fixtures" / "valid-graph-draft.json"
        graph = json.loads(fixture.read_text(encoding="utf-8"))
        ids = {card["key"]: f"t_{index + 1}" for index, card in enumerate(graph["cards"])}
        triage_body = json.dumps(self.triage_body(), ensure_ascii=False)
        readback = []
        for card in graph["cards"]:
            body = triage_body if card["card_type"] == "triage" else json.dumps(card["body"], ensure_ascii=False)
            readback.append(
                {
                    "task": {
                        "id": ids[card["key"]],
                        "title": card["title"],
                        "assignee": card["assignee"],
                        "status": card["status"],
                        "body": body,
                    },
                    "parents": [ids[parent] for parent in card["parents"]],
                }
            )
        return graph, readback

    def runner_for(self, readback, *, auto_ready_after_triage_completion=False):
        by_id = {entry["task"]["id"]: copy.deepcopy(entry) for entry in readback}
        calls = []

        def runner(command, **kwargs):
            calls.append(command)
            action = command[6]
            if action == "show":
                task_id = command[7]
                return subprocess.CompletedProcess(command, 0, json.dumps(by_id[task_id]), "")
            if action == "complete":
                task_ids = command[7:]
                if task_ids[:1] == ["--result"]:
                    task_ids = task_ids[2:]
                for task_id in task_ids:
                    by_id[task_id]["task"]["status"] = "done"
                    if auto_ready_after_triage_completion and task_id == "t_1":
                        by_id["t_2"]["task"]["status"] = "ready"
                return subprocess.CompletedProcess(command, 0, "", "")
            if action == "promote":
                by_id[command[7]]["task"]["status"] = "ready"
                return subprocess.CompletedProcess(command, 0, "", "")
            raise AssertionError(command)

        return runner, calls

    def invoke(self, graph, readback, runner, *, apply):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            graph_path = root / "graph.json"
            readback_path = root / "readback.json"
            graph_path.write_text(json.dumps(graph, ensure_ascii=False), encoding="utf-8")
            readback_path.write_text(json.dumps(readback, ensure_ascii=False), encoding="utf-8")
            return activate(
                graph_path,
                readback_path,
                "t_1",
                [],
                "impl-1",
                "hermes",
                "activation-test",
                apply=apply,
                runner=runner,
            )

    def test_preflight_is_read_only_and_valid(self):
        graph, readback = self.graph_and_readback()
        runner, calls = self.runner_for(readback)
        report = self.invoke(graph, readback, runner, apply=False)
        self.assertTrue(report["valid"])
        self.assertFalse(report["mutated"])
        self.assertTrue(all(command[6] == "show" for command in calls))

    def test_failed_preflight_never_completes_or_promotes(self):
        graph, readback = self.graph_and_readback()
        readback[0]["task"]["status"] = "running"
        runner, calls = self.runner_for(readback)
        report = self.invoke(graph, readback, runner, apply=True)
        self.assertFalse(report["valid"])
        self.assertIn("TRIAGE_NOT_NATIVE_TRIAGE_OR_DONE", report["errors"])
        self.assertFalse(report["mutated"])
        self.assertTrue(all(command[6] == "show" for command in calls))

    def test_apply_completes_triage_before_single_promotion(self):
        graph, readback = self.graph_and_readback()
        runner, calls = self.runner_for(readback)
        report = self.invoke(graph, readback, runner, apply=True)
        self.assertTrue(report["valid"])
        self.assertTrue(report["mutated"])
        mutations = [command[6] for command in calls if command[6] != "show"]
        self.assertEqual(["complete", "promote"], mutations)


    def test_apply_accepts_target_auto_promoted_by_triage_completion(self):
        graph, readback = self.graph_and_readback()
        runner, calls = self.runner_for(readback, auto_ready_after_triage_completion=True)

        report = self.invoke(graph, readback, runner, apply=True)

        self.assertTrue(report["valid"])
        mutations = [command[6] for command in calls if command[6] != "show"]
        self.assertEqual(["complete"], mutations)


if __name__ == "__main__":
    unittest.main()
