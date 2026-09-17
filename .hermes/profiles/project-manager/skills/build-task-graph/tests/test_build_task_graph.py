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


class BuildTaskGraphContractTest(unittest.TestCase):
    fixture_path = SKILL_DIR / "tests" / "fixtures" / "valid-new-delivery.json"

    def valid_draft(self):
        return json.loads(self.fixture_path.read_text(encoding="utf-8"))

    def invalid_draft(self):
        path = SKILL_DIR / "tests" / "fixtures" / "invalid-missing-cards.json"
        return json.loads(path.read_text(encoding="utf-8"))

    def test_valid_new_delivery_fixture_passes(self):
        self.assertEqual([], validate(self.valid_draft()))

    def test_invalid_new_delivery_fixture_fails(self):
        self.assertIn("MISSING_CARDS", validate(self.invalid_draft()))

    def test_template_is_fact_only_and_requires_cards(self):
        draft = template(138, "Seller product registration", "https://github.com/example/repo/issues/138", "a" * 40, "b" * 40)
        self.assertEqual("new-delivery", draft["mode"])
        self.assertEqual([], draft["cards"])
        self.assertIn("MISSING_CARDS", validate(draft))

    def test_rejects_multiple_initially_eligible_children(self):
        draft = self.valid_draft()
        draft["cards"][1]["depends_on_card_keys"] = []
        self.assertIn("ELIGIBLE_CHILD_COUNT_INVALID", validate(draft))

    def test_rejects_non_behavioral_acceptance_coverage(self):
        draft = self.valid_draft()
        check = draft["cards"][0]["body"]["verification"][0]
        check["kind"] = "structural"
        self.assertIn("CARD:product-api:NON_BEHAVIORAL_COVERAGE:check-product-registration", validate(draft))

    def test_rejects_inapplicable_section_without_reason(self):
        draft = self.valid_draft()
        ui_flow = draft["cards"][0]["body"]["implementation"]["ui_flow"]
        ui_flow.pop("not_applicable_reason")
        self.assertIn("MISSING_NOT_APPLICABLE_REASON:product-api:ui_flow", validate(draft))

    def test_rejects_unknown_executor(self):
        draft = self.valid_draft()
        draft["cards"][0]["body"]["verification"][0]["execution"]["executor"] = "gradle"
        self.assertIn("CARD:product-api:INVALID_EXECUTOR:0", validate(draft))

    def test_rejects_body_assignee_mismatch(self):
        draft = self.valid_draft()
        draft["cards"][0]["body"]["assignee"] = "reviewer-general"
        self.assertIn("CARD:product-api:ASSIGNEE_MISMATCH", validate(draft))

    def test_write_json_is_scoped_to_project_temp(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = Path(".temp") / "task-graphs" / "issue-138" / "draft.json"
            with patch("build_task_graph.Path.cwd", return_value=root):
                write_json(output, {"ok": True})
            self.assertEqual({"ok": True}, json.loads((root / output).read_text(encoding="utf-8")))
            with self.assertRaises(ValueError):
                write_json(Path("draft.json"), {"ok": True})

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
                    "--title",
                    "Seller product registration",
                    "--issue-url",
                    "https://github.com/example/repo/issues/138",
                    "--planning-sha",
                    "a" * 40,
                    "--current-state-sha",
                    "b" * 40,
                    "--output",
                    ".temp/task-graphs/issue-138/draft.json",
                ],
                cwd=directory,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, completed.returncode, completed.stderr)
            output = Path(directory) / ".temp/task-graphs/issue-138/draft.json"
            self.assertEqual(138, json.loads(output.read_text(encoding="utf-8"))["issue"]["number"])


if __name__ == "__main__":
    unittest.main()
