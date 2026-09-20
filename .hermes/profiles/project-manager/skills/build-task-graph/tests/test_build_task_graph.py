import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SKILL_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SKILL_DIR / "scripts"))
from build_task_graph import template, validate, write_json  # noqa: E402
from graph_contract import requirement_diff, validate_graph  # noqa: E402


class BackendImplementationCardContractTest(unittest.TestCase):
    fixture_path = SKILL_DIR / "tests" / "fixtures" / "valid-new-delivery.json"

    def valid_card(self):
        return json.loads(self.fixture_path.read_text(encoding="utf-8"))

    def invalid_card(self):
        path = SKILL_DIR / "tests" / "fixtures" / "invalid-missing-sections.json"
        return json.loads(path.read_text(encoding="utf-8"))

    def test_valid_backend_implementation_card_passes(self):
        self.assertEqual([], validate(self.valid_card()))

    def test_missing_required_sections_fail(self):
        errors = validate(self.invalid_card())
        self.assertIn("MISSING_EFFECTIVE_BEHAVIOR", errors)
        self.assertIn("MISSING_FULL_BACKEND_VERIFICATION", errors)

    def test_template_is_fact_only_and_requires_authoring(self):
        card = template(138, "https://github.com/example/repository/issues/138")
        self.assertEqual("backend-implementation-card-v1", card["schema"])
        self.assertNotIn("assignee", card)
        self.assertNotIn("baseline_sha", card)
        self.assertIn("MISSING_GOAL", validate(card))

    def test_rejects_native_and_planning_fields_anywhere_in_body(self):
        for field in ("baseline_sha", "planning_sha", "assignee", "status", "dependency", "workspace"):
            with self.subTest(field=field):
                card = self.valid_card()
                card["implementation_context"][field] = "forbidden"
                self.assertIn(f"FORBIDDEN_FIELD:{field}", validate(card))

    def test_requires_every_contract_dimension(self):
        card = self.valid_card()
        del card["contracts"]["transaction_consistency"]
        self.assertIn("MISSING_CONTRACT:transaction_consistency", validate(card))

    def test_requires_applicable_contract_rules(self):
        card = self.valid_card()
        card["contracts"]["api"] = {"applicable": True, "rules": []}
        self.assertIn("MISSING_CONTRACT_RULES:api", validate(card))

    def test_requires_non_applicable_contract_reason(self):
        card = self.valid_card()
        card["contracts"]["event"] = {"applicable": False}
        self.assertIn("MISSING_NOT_APPLICABLE_REASON:event", validate(card))

    def test_rejects_duplicate_ids(self):
        mutations = (
            ("effective_behavior", "DUPLICATE_BEHAVIOR_ID:behavior-product-create"),
            ("acceptance_criteria", "DUPLICATE_ACCEPTANCE_ID:AC-PRODUCT-CREATE"),
            ("focused_verification", "DUPLICATE_VERIFICATION_ID:FV-PRODUCT-CREATE"),
        )
        for field, error in mutations:
            with self.subTest(field=field):
                card = self.valid_card()
                card[field].append(copy.deepcopy(card[field][0]))
                self.assertIn(error, validate(card))

    def test_rejects_unknown_acceptance_reference(self):
        card = self.valid_card()
        card["focused_verification"][0]["acceptance_ids"] = ["AC-UNKNOWN"]
        self.assertIn("UNKNOWN_ACCEPTANCE_ID:FV-PRODUCT-CREATE:AC-UNKNOWN", validate(card))

    def test_requires_complete_acceptance_coverage(self):
        card = self.valid_card()
        card["focused_verification"][0]["acceptance_ids"] = []
        errors = validate(card)
        self.assertIn("MISSING_ACCEPTANCE_IDS:FV-PRODUCT-CREATE", errors)
        self.assertIn("UNCOVERED_ACCEPTANCE:AC-PRODUCT-CREATE", errors)

    def test_rejects_invalid_test_level(self):
        card = self.valid_card()
        card["focused_verification"][0]["test_level"] = "e2e"
        self.assertIn("INVALID_TEST_LEVEL:FV-PRODUCT-CREATE", validate(card))

    def test_malformed_verification_values_return_errors_instead_of_crashing(self):
        card = self.valid_card()
        verification = card["focused_verification"][0]
        verification["test_level"] = []
        verification["required_scenarios"] = [{}]
        errors = validate(card)
        self.assertIn("INVALID_TEST_LEVEL:FV-PRODUCT-CREATE", errors)
        self.assertIn("MISSING_REQUIRED_SCENARIOS:FV-PRODUCT-CREATE", errors)

    def test_requires_gradle_mcp_full_backend_test(self):
        for field, value, error in (
            ("executor", "gradle", "INVALID_FULL_BACKEND_EXECUTOR"),
            ("task", "check", "INVALID_FULL_BACKEND_TASK"),
        ):
            with self.subTest(field=field):
                card = self.valid_card()
                card["full_backend_verification"][field] = value
                self.assertIn(error, validate(card))

    def test_rejects_frontend_verification_fields(self):
        card = self.valid_card()
        card["full_backend_verification"]["npm"] = "test"
        self.assertIn("UNEXPECTED_FULL_BACKEND_FIELD:npm", validate(card))

    def test_rejects_legacy_top_level_fields(self):
        card = self.valid_card()
        card["planning"] = {"state_freshness": "fresh"}
        self.assertIn("UNEXPECTED_CARD_FIELD:planning", validate(card))

    def test_rejects_non_backend_contract_dimensions(self):
        card = self.valid_card()
        card["contracts"]["ui_flow"] = {
            "applicable": False,
            "not_applicable_reason": "Frontend is out of scope.",
        }
        self.assertIn("UNEXPECTED_CONTRACT:ui_flow", validate(card))

    def test_rejects_noncanonical_nested_fields(self):
        mutations = (
            (("issue",), "title", "UNEXPECTED_ISSUE_FIELD:title"),
            (("effective_behavior", 0), "prompt", "UNEXPECTED_BEHAVIOR_FIELD:0:prompt"),
            (("implementation_context",), "prompt", "UNEXPECTED_CONTEXT_FIELD:prompt"),
            (("implementation_context", "entry_points", 0), "prompt", "UNEXPECTED_ENTRY_POINT_FIELD:0:prompt"),
            (("contracts", "api"), "prompt", "UNEXPECTED_CONTRACT_FIELD:api:prompt"),
            (("acceptance_criteria", 0), "prompt", "UNEXPECTED_ACCEPTANCE_FIELD:0:prompt"),
            (("focused_verification", 0), "executor", "UNEXPECTED_VERIFICATION_FIELD:0:executor"),
            (("traceability", 0), "prompt", "UNEXPECTED_TRACE_FIELD:0:prompt"),
            (("traceability", 0, "source"), "prompt", "UNEXPECTED_TRACE_SOURCE_FIELD:0:prompt"),
        )
        for path, field, error in mutations:
            with self.subTest(path=path, field=field):
                card = self.valid_card()
                target = card
                for part in path:
                    target = target[part]
                target[field] = "unexpected"
                self.assertIn(error, validate(card))

    def test_rejects_task_id_traceability_locator(self):
        card = self.valid_card()
        card["traceability"][0]["source"] = {"kind": "task", "task_id": "t_123"}
        self.assertIn("INVALID_TRACE_KIND:0", validate(card))

    def test_requires_korean_descriptive_text(self):
        for goal in (
            "An authenticated seller can create a product.",
            "An authenticated seller can create a product 한.",
        ):
            with self.subTest(goal=goal):
                card = self.valid_card()
                card["goal"] = goal
                self.assertIn("DESCRIPTION_NOT_KOREAN:goal", validate(card))

    def test_traceability_uses_only_canonical_repository_locator(self):
        card = self.valid_card()
        card["traceability"][0]["source"] = {
            "kind": "issue",
            "url": "https://github.com/example/repository/issues/138",
            "heading": "상품 등록",
        }
        self.assertIn("INVALID_TRACE_KIND:0", validate(card))

        card = self.valid_card()
        card["traceability"][0]["source"] = {
            "kind": "repository",
            "path": "docs/requirement/p2/product.md",
            "lines": "10-20",
        }
        errors = validate(card)
        self.assertIn("UNEXPECTED_TRACE_SOURCE_FIELD:0:lines", errors)
        self.assertIn("MISSING_TRACE_HEADING:0", errors)

    def test_rejects_boolean_issue_number(self):
        card = self.valid_card()
        card["issue"]["number"] = True
        self.assertIn("INVALID_ISSUE_NUMBER", validate(card))

    def test_write_json_is_scoped_to_project_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = Path(".temp") / "task-graphs" / "issue-138" / "impl-1.json"
            with patch("build_task_graph.Path.cwd", return_value=root):
                write_json(output, {"ok": True})
            self.assertEqual({"ok": True}, json.loads((root / output).read_text(encoding="utf-8")))
            with self.assertRaises(ValueError):
                write_json(Path("impl-1.json"), {"ok": True})
            with self.assertRaises(ValueError):
                write_json(Path(".temp") / ".." / "escaped.json", {"ok": True})
            self.assertFalse((root / "escaped.json").exists())

    def test_cli_validates_fixture(self):
        completed = subprocess.run(
            [sys.executable, str(SKILL_DIR / "scripts" / "build_task_graph.py"), "validate", str(self.fixture_path)],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual({"valid": True, "errors": []}, json.loads(completed.stdout))

    def test_cli_writes_template_under_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            completed = subprocess.run(
                [
                    sys.executable,
                    str(SKILL_DIR / "scripts" / "build_task_graph.py"),
                    "template",
                    "--issue",
                    "138",
                    "--issue-url",
                    "https://github.com/example/repository/issues/138",
                    "--output",
                    ".temp/task-graphs/issue-138/impl-1.json",
                ],
                cwd=directory,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, completed.returncode, completed.stderr)
            output = Path(directory) / ".temp/task-graphs/issue-138/impl-1.json"
            self.assertEqual("backend-implementation-card-v1", json.loads(output.read_text(encoding="utf-8"))["schema"])


class TaskGraphContractTest(unittest.TestCase):
    valid_path = SKILL_DIR / "tests" / "fixtures" / "valid-graph-draft.json"

    def valid_graph(self):
        return json.loads(self.valid_path.read_text(encoding="utf-8"))

    def review_rework_graph(self):
        graph = self.valid_graph()
        triage, impl1, review1, summary = graph["cards"]
        triage["status"] = impl1["status"] = review1["status"] = "done"
        impl2 = copy.deepcopy(impl1)
        impl2.update({"key": "impl-2", "title": "G1-Issue138-Impl2", "status": "ready", "parents": ["review-1"]})
        review2 = copy.deepcopy(review1)
        review2.update({"key": "review-2", "title": "G1-Issue138-Review2", "status": "todo", "parents": ["impl-2"]})
        review2["body"]["implementation_card_keys"] = ["impl-1", "impl-2"]
        summary["parents"] = ["impl-1", "review-1", "impl-2", "review-2"]
        graph["cards"] = [triage, impl1, review1, impl2, review2, summary]
        graph.update(
            {
                "mode": "review-rework",
                "lineage": {"prior_generation": 1, "prior_summary": "summary", "archived_unfinished_keys": []},
                "source_review": {
                    "review_key": "review-1",
                    "dispositions": [
                        {
                            "finding_id": "F-1",
                            "verdict": "correction-required",
                            "appended_card_keys": ["impl-2", "review-2"],
                        }
                    ],
                    "idempotency": {
                        "impl-2": "review-1:F-1:correction",
                        "review-2": "review-1:F-1:review",
                    },
                },
                "ready_candidate": "impl-2",
            }
        )
        return graph

    def test_valid_graph_draft_passes(self):
        self.assertEqual([], validate_graph(self.valid_graph(), validate_implementation=validate))

    def test_invalid_graph_fixture_detects_lineage_topology_ready_and_archive(self):
        path = SKILL_DIR / "tests" / "fixtures" / "invalid-graph-topology.json"
        errors = validate_graph(json.loads(path.read_text(encoding="utf-8")), validate_implementation=validate)
        self.assertIn("TITLE_LINEAGE_MISMATCH:impl-1", errors)
        self.assertIn("SUMMARY_MEMBERSHIP_MISMATCH", errors)
        self.assertIn("DRAFT_HAS_READY_CARD", errors)
        self.assertIn("ARCHIVE_STATUS_MISMATCH:triage", errors)
        self.assertIn("ARCHIVE_TYPE_FORBIDDEN:triage", errors)

    def test_native_phase_requires_exact_ready_candidate_and_done_triage(self):
        graph = self.valid_graph()
        graph["cards"][0]["status"] = "done"
        graph["cards"][1]["status"] = "ready"
        self.assertEqual([], validate_graph(graph, phase="native", validate_implementation=validate))

    def test_implemented_behavior_requires_inherited_evidence(self):
        graph = self.valid_graph()
        behavior = graph["behaviors"][0]
        behavior.update({"state": "implemented", "disposition": "inherited", "evidence": []})
        self.assertIn("MISSING_INHERITED_EVIDENCE:behavior-product-create", validate_graph(graph))

    def test_requirement_rework_requires_revision_and_next_generation(self):
        graph = self.valid_graph()
        graph.update({"mode": "requirement-rework", "generation": 1})
        self.assertIn("REWORK_REQUIRES_NEXT_GENERATION", validate_graph(graph))
        self.assertIn("REVISED_REQUIREMENT_NOT_OBJECT", validate_graph(graph))
        self.assertIn("MISSING_REWORK_LINEAGE", validate_graph(graph))

    def test_planned_behavior_must_resolve_to_matching_implementation(self):
        graph = self.valid_graph()
        graph["behaviors"][0]["implementation_card_key"] = "ghost"
        self.assertIn("BEHAVIOR_IMPL_NOT_FOUND:behavior-product-create", validate_graph(graph))
        graph = self.valid_graph()
        graph["cards"][1]["body"]["effective_behavior"][0]["outcome"] = "다른 동작을 구현한다."
        self.assertIn("BEHAVIOR_IMPL_MISMATCH:behavior-product-create", validate_graph(graph))

    def test_rejects_cycle_and_invalid_topology(self):
        graph = self.valid_graph()
        graph["cards"][1]["parents"] = ["review-1"]
        self.assertTrue(any(error.startswith("DEPENDENCY_CYCLE:") for error in validate_graph(graph)))
        graph = self.valid_graph()
        graph["cards"][2]["parents"] = ["summary"]
        self.assertIn("INVALID_REVIEW_PARENTS:review-1", validate_graph(graph))

    def test_phase_rejects_non_todo_active_cards(self):
        graph = self.valid_graph()
        graph["cards"][2]["status"] = "done"
        self.assertIn("DRAFT_ACTIVE_STATUS:review-1", validate_graph(graph))
        graph["cards"][0]["status"] = "done"
        graph["cards"][1]["status"] = "ready"
        self.assertIn("NATIVE_ACTIVE_STATUS:review-1", validate_graph(graph, phase="native"))

    def test_archive_declaration_is_bidirectional(self):
        graph = self.valid_graph()
        graph["cards"][2]["status"] = "archived"
        self.assertIn("ARCHIVED_CARD_UNDECLARED:review-1", validate_graph(graph))

    def test_review_references_are_resolved(self):
        graph = self.valid_graph()
        graph["cards"][2]["body"]["implementation_card_keys"] = ["ghost"]
        self.assertIn("REVIEW_IMPL_NOT_FOUND:review-1:ghost", validate_graph(graph))
        graph = self.valid_graph()
        graph["cards"][2]["body"]["inherited_behavior_ids"] = ["behavior-product-create"]
        self.assertIn("REVIEW_INHERITED_BEHAVIOR_INVALID:review-1:behavior-product-create", validate_graph(graph))

    def test_title_type_and_assignee_must_match_native_identity(self):
        graph = self.valid_graph()
        graph["cards"][1]["title"] = "G1-Issue138-Review9"
        graph["cards"][1]["assignee"] = ""
        errors = validate_graph(graph)
        self.assertIn("TITLE_CARD_TYPE_MISMATCH:impl-1", errors)
        self.assertIn("MISSING_ASSIGNEE:impl-1", errors)

    def test_review_rework_requires_same_generation_source_provenance(self):
        graph = self.valid_graph()
        graph.update(
            {
                "mode": "review-rework",
                "lineage": {"prior_generation": 1, "prior_summary": "t_summary", "archived_unfinished_keys": []},
                "source_review": {
                    "review_key": "review-1",
                    "dispositions": [{"finding_id": "F-1", "verdict": "correction-required", "appended_card_keys": ["impl-1"]}],
                    "idempotency": {"impl-1": "review-1:F-1:correction"},
                },
            }
        )
        self.assertNotIn("MISSING_SOURCE_REVIEW", validate_graph(graph))
        graph["source_review"] = None
        self.assertIn("MISSING_SOURCE_REVIEW", validate_graph(graph))

    def test_all_implemented_graph_promotes_review_without_impl(self):
        graph = self.valid_graph()
        behavior = graph["behaviors"][0]
        behavior.update({"state": "implemented", "disposition": "inherited", "evidence": ["source:test"]})
        behavior.pop("implementation_card_key")
        graph["cards"] = [card for card in graph["cards"] if card["key"] != "impl-1"]
        review = next(card for card in graph["cards"] if card["key"] == "review-1")
        review["parents"] = ["triage"]
        review["body"]["implementation_card_keys"] = []
        review["body"]["inherited_behavior_ids"] = ["behavior-product-create"]
        summary = next(card for card in graph["cards"] if card["key"] == "summary")
        summary["parents"] = ["review-1"]
        graph["ready_candidate"] = "review-1"
        self.assertEqual([], validate_graph(graph))
        graph["cards"][0]["status"] = "done"
        review["status"] = "ready"
        self.assertEqual([], validate_graph(graph, phase="native"))

    def test_review_rework_validates_historical_and_appended_graph(self):
        graph = self.valid_graph()
        triage, impl1, review1, summary = graph["cards"]
        triage["status"] = impl1["status"] = review1["status"] = "done"
        impl2 = copy.deepcopy(impl1)
        impl2.update({"key": "impl-2", "title": "G1-Issue138-Impl2", "status": "todo", "parents": ["review-1"]})
        review2 = copy.deepcopy(review1)
        review2.update({"key": "review-2", "title": "G1-Issue138-Review2", "status": "todo", "parents": ["impl-2"]})
        review2["body"]["implementation_card_keys"] = ["impl-1", "impl-2"]
        summary["parents"] = ["impl-1", "review-1", "impl-2", "review-2"]
        graph["cards"] = [triage, impl1, review1, impl2, review2, summary]
        graph.update(
            {
                "mode": "review-rework",
                "lineage": {"prior_generation": 1, "prior_summary": "summary", "archived_unfinished_keys": []},
                "source_review": {
                    "review_key": "review-1",
                    "dispositions": [
                        {
                            "finding_id": "F-1",
                            "verdict": "correction-required",
                            "appended_card_keys": ["impl-2", "review-2"],
                        }
                    ],
                    "idempotency": {
                        "impl-2": "review-1:F-1:correction",
                        "review-2": "review-1:F-1:review",
                    },
                },
                "ready_candidate": "impl-2",
            }
        )
        self.assertEqual([], validate_graph(graph))
        impl2["status"] = "ready"
        self.assertEqual([], validate_graph(graph, phase="native"))

    def test_review_rework_rejects_null_candidate_with_appended_impl(self):
        graph = self.review_rework_graph()
        graph["ready_candidate"] = None
        errors = validate_graph(graph, phase="native")
        self.assertIn("REVIEW_REWORK_ACTIVATION_MISMATCH", errors)

    def test_decision_only_review_rework_has_no_ready_card(self):
        graph = self.review_rework_graph()
        triage, impl1, review1, _, _, summary = graph["cards"]
        decision = {
            "key": "decision-1",
            "title": "G1-Issue138-Decision1",
            "card_type": "decision",
            "assignee": "project-manager",
            "status": "blocked",
            "parents": ["review-1"],
            "body": {
                "schema": "policy-decision-card-v1",
                "source_review_key": "review-1",
                "finding_ids": ["F-1"],
                "question": "사용자가 정책을 결정해야 한다.",
                "decision_owner": "user",
            },
        }
        summary["parents"] = ["impl-1", "review-1", "decision-1"]
        graph["cards"] = [triage, impl1, review1, decision, summary]
        graph["source_review"] = {
            "review_key": "review-1",
            "dispositions": [
                {"finding_id": "F-1", "verdict": "decision-required", "appended_card_keys": ["decision-1"]}
            ],
            "idempotency": {"decision-1": "review-1:F-1:decision"},
        }
        graph["ready_candidate"] = None
        self.assertEqual([], validate_graph(graph, phase="native"))

    def test_summary_rejects_extra_parent(self):
        graph = self.valid_graph()
        graph["cards"][-1]["parents"].append("triage")
        self.assertIn("SUMMARY_MEMBERSHIP_MISMATCH", validate_graph(graph))

    def test_implementation_requires_review_child(self):
        graph = self.valid_graph()
        graph["cards"][2]["parents"] = ["triage"]
        self.assertIn("IMPLEMENTATION_WITHOUT_REVIEW_CHILD:impl-1", validate_graph(graph))

    def test_all_implemented_graph_rejects_unnecessary_impl(self):
        graph = self.valid_graph()
        behavior = graph["behaviors"][0]
        behavior.update({"state": "implemented", "disposition": "inherited", "evidence": ["source:test"]})
        behavior.pop("implementation_card_key")
        self.assertIn("IMPLEMENTATION_BEHAVIOR_COVERAGE", validate_graph(graph))

    def test_graph_identity_requires_url_sha_and_unique_title(self):
        graph = self.valid_graph()
        graph["issue"].pop("url")
        graph["requirement_basis"]["revision"] = "not-a-sha"
        graph["cards"][2]["title"] = graph["cards"][1]["title"]
        errors = validate_graph(graph)
        self.assertIn("INVALID_ISSUE_URL", errors)
        self.assertIn("INVALID_REQUIREMENT_BASIS_REVISION", errors)
        self.assertTrue(any(error.startswith("DUPLICATE_CARD_TITLE:") for error in errors))

    def test_requirement_diff_is_stable_and_keeps_behavior_decision_manual(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(["git", "init", "-q"], cwd=root, check=True)
            subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=root, check=True)
            subprocess.run(["git", "config", "user.name", "Test"], cwd=root, check=True)
            requirement = root / "requirement.md"
            requirement.write_text("# 정책\n기존 동작\n", encoding="utf-8")
            subprocess.run(["git", "add", "requirement.md"], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "base"], cwd=root, check=True)
            base = subprocess.run(["git", "rev-parse", "HEAD"], cwd=root, check=True, text=True, capture_output=True).stdout.strip()
            requirement.write_text("# 정책\n변경된 동작\n", encoding="utf-8")
            subprocess.run(["git", "add", "requirement.md"], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "revised"], cwd=root, check=True)
            revised = subprocess.run(["git", "rev-parse", "HEAD"], cwd=root, check=True, text=True, capture_output=True).stdout.strip()
            first = requirement_diff(root, base, revised, ["requirement.md"])
            second = requirement_diff(root, base, revised, ["requirement.md"])
            self.assertEqual(first["hunks"], second["hunks"])
            self.assertEqual([], first["behavior_deltas"])
            self.assertEqual("human-review-required", first["behavior_delta_status"])

    def test_cli_validates_graph_fixture(self):
        completed = subprocess.run(
            [sys.executable, str(SKILL_DIR / "scripts" / "build_task_graph.py"), "validate-graph", str(self.valid_path)],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual({"valid": True, "errors": []}, json.loads(completed.stdout))


if __name__ == "__main__":
    unittest.main()
