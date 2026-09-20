import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from triage import freeze, read_card, template, validate, validate_card, write_json


class TriageContractTest(unittest.TestCase):
    def populated_body(self):
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
        return body

    def test_populated_body_is_valid_for_running_card(self):
        self.assertEqual([], validate(self.populated_body(), "running"))

    def test_template_preserves_issue_specific_inputs_only(self):
        body = template(
            138,
            "G1-Issue138-Triage",
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
        self.assertIn("BLOCKED_WITHOUT_OPEN_POLICY_FINDING", validate(body, "blocked"))
        self.assertIn("BLOCKED_WITHOUT_DECISION_REQUEST", validate(body, "blocked"))

    def test_blocked_card_accepts_structured_pending_decision(self):
        body = self.populated_body()
        body["blocker"] = {"kind": "policy-decision"}
        body["policy_findings"] = [
            {"id": "PF-001", "problem": "Authorization owner is unspecified.", "evidence": ["docs/requirement/p2.md#authorization"], "status": "open"}
        ]
        body["decision_requests"] = [
            {
                "id": "DR-001",
                "problem": "Authorization owner must be selected.",
                "why": "Implementation behavior differs by owner.",
                "evidence": ["docs/requirement/p2.md#authorization"],
                "options": [{"id": "A", "choice": "Seller", "impact": "Seller owns authorization."}],
                "decision_owner": "user",
                "decision_status": "pending",
                "approved_change": None,
            }
        ]
        self.assertEqual([], validate(body, "blocked"))

    def test_document_decision_requires_valid_locators(self):
        body = self.populated_body()
        body["document_impact"]["architecture"]["locators"] = []
        self.assertIn("MISSING_DOCUMENT_LOCATOR:architecture", validate(body, "running"))

    def test_open_graph_gate_requires_frozen_resolved_plan(self):
        body = self.populated_body()
        body["build_task_graph"]["allowed"] = True
        self.assertIn("OPEN_GRAPH_GATE_WITHOUT_FROZEN_PLAN", validate(body, "running"))

        body["planning_state"] = "frozen"
        body["document_impact"]["ADR"]["decision"] = "blocked"
        self.assertIn("OPEN_GRAPH_GATE_WITH_BLOCKED_DOCUMENT", validate(body, "running"))

        body["document_impact"]["ADR"]["decision"] = "no-change"
        body["policy_findings"] = [
            {"id": "PF-001", "problem": "Policy is unresolved.", "evidence": ["docs/requirement/p2.md#policy"], "status": "open"}
        ]
        self.assertIn("OPEN_GRAPH_GATE_WITH_POLICY_FINDING", validate(body, "running"))

    def test_frozen_resolved_plan_opens_graph_gate(self):
        body = freeze(self.populated_body())
        self.assertEqual([], validate(body, "running"))
        self.assertIn("OPEN_GRAPH_GATE_IN_INVALID_STATUS", validate(body, "triage"))

        body["goal"] = "The frozen plan was changed."
        self.assertIn("FROZEN_BODY_DIGEST_MISMATCH", validate(body, "running"))

    def test_decision_request_requires_structured_options(self):
        body = self.populated_body()
        body["decision_requests"] = [
            {
                "id": "DR-001",
                "problem": "A policy decision is required.",
                "why": "Behavior depends on the selected policy.",
                "evidence": ["docs/requirement/p2.md#policy"],
                "options": [{}],
                "decision_owner": "user",
                "decision_status": "pending",
                "approved_change": None,
            }
        ]
        errors = validate(body, "running")
        self.assertIn("MISSING_DECISION_REQUEST:0_OPTION:0_ID", errors)
        self.assertIn("MISSING_DECISION_REQUEST:0_OPTION:0_CHOICE", errors)
        self.assertIn("MISSING_DECISION_REQUEST:0_OPTION:0_IMPACT", errors)

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

    @patch("triage.subprocess.run")
    def test_read_card_uses_official_read_only_cli_surface(self, run):
        run.return_value.returncode = 0
        run.return_value.stdout = json.dumps({"task": {"id": "t_94686adc"}})

        self.assertEqual({"id": "t_94686adc"}, read_card("hermes", "issue-138", "t_94686adc"))
        run.assert_called_once_with(
            ["hermes", "kanban", "--board", "issue-138", "show", "t_94686adc", "--json"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )

    def test_generated_draft_must_be_written_under_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = Path(".temp") / "triage" / "issue-138" / "draft.json"
            with patch("triage.Path.cwd", return_value=root):
                write_json(output, {"ok": True})
            self.assertEqual({"ok": True}, json.loads((root / output).read_text(encoding="utf-8")))
            with self.assertRaises(ValueError):
                write_json(Path("draft.json"), {"ok": True})
            with self.assertRaises(ValueError):
                write_json(Path(".temp") / ".." / "escaped.json", {"ok": True})


if __name__ == "__main__":
    unittest.main()
