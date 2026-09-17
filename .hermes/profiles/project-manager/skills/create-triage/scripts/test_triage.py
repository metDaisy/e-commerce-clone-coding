import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from triage import template, validate, validate_card, write_json


class TriageContractTest(unittest.TestCase):
    def populated_body(self):
        body = template(
            138,
            "Issue #138 triage",
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
            entry["reason"] = f"{document} was reviewed and needs no update."
        return body

    def test_populated_body_is_valid_for_running_card(self):
        self.assertEqual([], validate(self.populated_body(), "running"))

    def test_template_preserves_issue_specific_inputs_only(self):
        body = template(
            138,
            "Issue #138 triage",
            "https://github.com/example/repo/issues/138",
            "a" * 40,
            "b" * 40,
            ["docs/requirement/p2/index.md#issue-138"],
            ["docs/current-state.md#p2"],
        )
        self.assertEqual(138, body["issue"]["number"])
        self.assertEqual(["docs/requirement/p2/index.md#issue-138"], body["document_impact"]["requirement"]["locators"])
        self.assertEqual(["docs/current-state.md#p2"], body["current_state"]["locators"])
        self.assertIn("MISSING_GOAL", validate(body, "triage"))

    def test_rejects_stale_snapshot(self):
        body = self.populated_body()
        body["current_state"]["freshness"] = "stale"
        self.assertIn("CURRENT_STATE_NOT_FRESH", validate(body, "running"))

    def test_blocked_card_requires_blocker_and_closed_graph_gate(self):
        body = self.populated_body()
        body["build_task_graph"]["allowed"] = True
        self.assertIn("BLOCKED_GRAPH_GATE", validate(body, "blocked"))
        self.assertIn("MISSING_BLOCKER", validate(body, "blocked"))

    def test_done_card_requires_open_graph_gate(self):
        self.assertIn("DONE_GRAPH_GATE", validate(self.populated_body(), "done"))

    def test_card_validation_uses_actual_native_id_and_json_body(self):
        body = self.populated_body()
        task = {
            "id": "t_94686adc",
            "status": "running",
            "assignee": "project-manager",
            "body": json.dumps(body),
        }
        self.assertEqual(
            [],
            validate_card(task, "t_94686adc", "running", "project-manager"),
        )

    def test_card_validation_rejects_non_json_body(self):
        task = {"id": "t_94686adc", "status": "running", "assignee": "project-manager", "body": "not-json"}
        self.assertIn("BODY_NOT_JSON", validate_card(task, "t_94686adc", "running", "project-manager"))

    def test_generated_draft_must_be_written_under_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = Path(".temp") / "triage" / "issue-138" / "draft.json"
            with patch("triage.Path.cwd", return_value=root):
                write_json(output, {"ok": True})
            self.assertEqual({"ok": True}, json.loads((root / output).read_text(encoding="utf-8")))
            with self.assertRaises(ValueError):
                write_json(Path("draft.json"), {"ok": True})


if __name__ == "__main__":
    unittest.main()
