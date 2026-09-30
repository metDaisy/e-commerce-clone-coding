import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from triage import freeze, read_card, template, validate, validate_card, validate_decision_alignment, write_json


class TriageContractTest(unittest.TestCase):
    def minimum_capability(self):
        return {
            "contract_owner": "offer",
            "public_surface": "OfferQueryApi",
            "request_or_event": "offerId query",
            "response_or_projection": "OfferSnapshot",
            "not_found_and_error_semantics": "OFFER-001 is preserved.",
            "consistency_and_transaction_semantics": "The query does not expose producer persistence.",
            "verification_plan": "Producer and consumer contract tests verify the real seam.",
        }

    def approved_follow_up(self):
        return {
            "producer": "Implement the offer-owned query bridge.",
            "consumer": "Implement the cart-owned adapter.",
            "integration_verification": "Run producer-consumer integration tests.",
        }

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

    def test_populated_body_is_valid_for_native_triage_card(self):
        self.assertEqual([], validate(self.populated_body(), "triage"))

    def test_absent_cross_domain_contract_requires_open_finding_and_decision(self):
        body = self.populated_body()
        body["cross_domain_contracts"] = [
            {
                "consumer_module": "cart",
                "producer_module": "offer",
                "relationship": "consumer",
                "capability": "현재 판매 가능 Offer 조회",
                "status": "absent-or-partial",
                "evidence": ["docs/requirement/p3/p3-cart.md#조회"],
                "public_contract": None,
                "producer_evidence": None,
                "minimum_capability": None,
                "follow_up": None,
                "policy_finding_id": "PF-001",
                "decision_request_id": "DR-001",
                "not_applicable_reason": None,
            }
        ]
        self.assertIn("MISSING_CROSS_DOMAIN_CONTRACT:0_DECISION_LIFECYCLE", validate(body, "running"))

        body["policy_findings"] = [
            {"id": "PF-001", "problem": "Offer public contract가 없다.", "evidence": ["docs/requirement/p9/p9-index.md#범위"], "status": "open"}
        ]
        body["decision_requests"] = [
            {
                "id": "DR-001", "problem": "Offer 조회 public contract를 결정한다.", "why": "Cart 불변식이 현재 Offer 사실을 필요로 한다.",
                "evidence": ["docs/requirement/p3/p3-cart.md#조회"], "options": [{"id": "A", "choice": "named interface query", "impact": "동기 조회 seam을 제공한다."}],
                "decision_owner": "user", "decision_status": "pending", "approved_change": None,
            }
        ]
        body["blocker"] = {"kind": "cross-domain-contract"}
        self.assertEqual([], validate(body, "triage"))

    def test_approved_minimum_contract_remains_absent_until_producer_is_real(self):
        body = self.populated_body()
        body["policy_findings"] = [
            {"id": "PF-001", "problem": "Offer public contract implementation is absent.", "evidence": ["docs/requirement/p9/p9-index.md#범위"], "status": "resolved"}
        ]
        body["decision_requests"] = [
            {
                "id": "DR-001", "problem": "Offer query public contract is selected.", "why": "Cart needs current Offer facts.",
                "evidence": ["docs/requirement/p3/p3-cart.md#조회"], "options": [{"id": "A", "choice": "named interface query", "impact": "동기 조회 seam을 제공한다."}],
                "decision_owner": "user", "decision_status": "approved", "approved_change": "OfferQueryApi 최소 계약을 승인한다.",
            }
        ]
        body["cross_domain_contracts"] = [
            {
                "consumer_module": "cart", "producer_module": "offer", "relationship": "consumer",
                "capability": "현재 판매 가능 Offer 조회", "status": "absent-or-partial",
                "evidence": ["docs/requirement/p3/p3-cart.md#조회"], "public_contract": None,
                "producer_evidence": None, "minimum_capability": self.minimum_capability(),
                "follow_up": self.approved_follow_up(), "policy_finding_id": "PF-001",
                "decision_request_id": "DR-001", "not_applicable_reason": None,
            }
        ]
        self.assertEqual([], validate(freeze(body), "triage"))

    def test_published_contract_requires_real_producer_evidence(self):
        body = self.populated_body()
        body["cross_domain_contracts"] = [
            {
                "consumer_module": "catalog", "producer_module": "seller", "relationship": "consumer",
                "capability": "활성 Seller 조회", "status": "published",
                "evidence": ["docs/current-state.md#백엔드 구조"], "public_contract": "SellerQueryApi.isActiveSeller",
                "producer_evidence": [], "minimum_capability": self.minimum_capability(), "follow_up": None,
                "policy_finding_id": None, "decision_request_id": None, "not_applicable_reason": None,
            }
        ]
        self.assertIn("MISSING_CROSS_DOMAIN_CONTRACT:0_PRODUCER_EVIDENCE", validate(body, "running"))

    def test_decision_card_must_match_approved_triage_request(self):
        body = self.populated_body()
        body["decision_requests"] = [
            {
                "id": "DR-001", "problem": "Offer query public contract is selected.", "why": "Cart needs current Offer facts.",
                "evidence": ["docs/requirement/p3/p3-cart.md#조회"], "options": [{"id": "A", "choice": "named interface query", "impact": "동기 조회 seam을 제공한다."}],
                "decision_owner": "user", "decision_status": "approved", "approved_change": "OfferQueryApi 최소 계약을 승인한다.",
            }
        ]
        decision = {
            "schema": "policy-decision-card-v1", "decision_request_id": "DR-001", "decision_status": "pending",
            "approved_change": None,
        }
        self.assertIn("DECISION_STATUS_MISMATCH", validate_decision_alignment(body, decision))
        decision.update({"decision_status": "approved", "approved_change": "OfferQueryApi 최소 계약을 승인한다.",
                         "minimum_capability": self.minimum_capability(), "follow_up": self.approved_follow_up()})
        self.assertEqual([], validate_decision_alignment(body, decision))

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

    def test_triage_rejects_blocked_native_status(self):
        body = self.populated_body()
        body["build_task_graph"]["allowed"] = True
        self.assertIn("TRIAGE_MUST_REMAIN_NATIVE_TRIAGE", validate(body, "blocked"))

    def test_native_triage_accepts_structured_pending_decision(self):
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
        self.assertEqual([], validate(body, "triage"))

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
        self.assertEqual([], validate(body, "triage"))
        self.assertIn("OPEN_GRAPH_GATE_IN_INVALID_STATUS", validate(body, "running"))

        body["goal"] = "The frozen plan was changed."
        self.assertIn("FROZEN_BODY_DIGEST_MISMATCH", validate(body, "triage"))

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
