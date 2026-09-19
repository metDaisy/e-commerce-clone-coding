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


if __name__ == "__main__":
    unittest.main()
