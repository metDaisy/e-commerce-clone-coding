import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TEST_DIR = Path(__file__).resolve().parent
SKILL_DIR = TEST_DIR.parent
PROFILE_DIR = SKILL_DIR.parents[1]
sys.path.insert(0, str(SKILL_DIR / "scripts"))
from checkpoint import (  # noqa: E402
    validate_change_request,
    validate_checkpoint,
    validate_handoff,
)


class CheckpointContractTest(unittest.TestCase):
    def card(self):
        fixture = (
            PROFILE_DIR
            / "skills"
            / "build-task-graph"
            / "tests"
            / "fixtures"
            / "valid-new-delivery.json"
        )
        return json.loads(fixture.read_text(encoding="utf-8"))

    def handoff(self):
        return {
            "schema": "backend-implementation-handoff-v1",
            "handoff_id": "handoff-7f3d87b2",
            "source_run_id": 16,
            "implemented_behavior_ids": ["behavior-product-create"],
            "changed_paths": [
                "src/main/java/example/product/ProductService.java",
                "src/test/java/example/product/ProductServiceTest.java",
            ],
            "acceptance_results": [
                {
                    "acceptance_id": "AC-PRODUCT-CREATE",
                    "result": "pass",
                    "focused_verification_ids": ["FV-PRODUCT-CREATE"],
                }
            ],
            "focused_verification_results": [
                {
                    "verification_id": "FV-PRODUCT-CREATE",
                    "executor": "gradle-mcp",
                    "tasks": ["test"],
                    "tests": ["example.product.ProductRegistrationIntegrationTest"],
                    "test_levels": ["integration"],
                    "scenario_results": [
                        {"scenario": "유효한 입력의 성공", "result": "pass"},
                        {"scenario": "유효하지 않은 입력의 거절", "result": "pass"},
                    ],
                    "result": "pass",
                }
            ],
            "full_backend_verification_result": {
                "executor": "gradle-mcp",
                "task": "test",
                "result": "pass",
            },
            "documentation_impact": {"detected": False, "details": []},
            "residual_risks": [],
        }

    def checkpoint(self):
        return {
            "schema": "backend-implementation-checkpoint-v1",
            "source_handoff_id": "handoff-7f3d87b2",
            "result": "pass",
            "committed_paths": self.handoff()["changed_paths"],
            "commit_sha": "a" * 40,
            "focused_verification_readback": self.handoff()["focused_verification_results"],
            "full_backend_verification_readback": "pass",
            "verification_run_id": 17,
            "documentation_impact_resolution": {
                "status": "not-applicable",
                "changed_paths": [],
                "commit_sha": None,
            },
            "clean_worktree": True,
        }

    def review_run(self):
        return {
            "run_id": 17,
            "status": "running",
            "assignee": "project-manager",
            "source_status": "review",
        }

    def test_valid_handoff_passes(self):
        self.assertEqual([], validate_handoff(self.card(), self.handoff()))

    def test_handoff_requires_exact_behavior_and_acceptance_coverage(self):
        handoff = self.handoff()
        handoff["implemented_behavior_ids"] = []
        handoff["acceptance_results"] = []
        errors = validate_handoff(self.card(), handoff)
        self.assertIn("BEHAVIOR_COVERAGE_MISMATCH", errors)
        self.assertIn("ACCEPTANCE_COVERAGE_MISMATCH", errors)

    def test_handoff_requires_exact_scenario_coverage(self):
        handoff = self.handoff()
        handoff["focused_verification_results"][0]["scenario_results"].pop()
        self.assertIn(
            "SCENARIO_COVERAGE_MISMATCH:FV-PRODUCT-CREATE",
            validate_handoff(self.card(), handoff),
        )

    def test_handoff_rejects_invalid_card_before_deriving_coverage(self):
        card = {"schema": "wrong", "effective_behavior": None}

        errors = validate_handoff(card, self.handoff())

        self.assertTrue(any(error.startswith("INVALID_CARD:") for error in errors))

    def test_handoff_rejects_document_changed_paths(self):
        handoff = self.handoff()
        handoff["changed_paths"].append("docs/architecture.md")
        self.assertIn("DOCUMENT_PATH_IN_CHANGED_PATHS:docs/architecture.md", validate_handoff(self.card(), handoff))

    def test_handoff_rejects_nonliteral_or_unsafe_changed_paths(self):
        for unsafe_path in (
            "/tmp/Product.java",
            "C:/repo/Product.java",
            "src/../docs/architecture.yaml",
            ":(glob)**/*.java",
            "--all",
            "--pathspec-from-file=paths.txt",
            "Docs/architecture.java",
            "src\\main\\Product.java",
            "src/main/java/example/...",
            "src/**/*.java",
        ):
            with self.subTest(path=unsafe_path):
                handoff = self.handoff()
                handoff["changed_paths"] = [unsafe_path]
                errors = validate_handoff(self.card(), handoff)
                self.assertTrue(
                    any(
                        error.startswith(("INVALID_CHANGED_PATH:", "DOCUMENT_PATH_IN_CHANGED_PATHS:"))
                        for error in errors
                    )
                )

    def test_valid_change_request_passes(self):
        request = {
            "schema": "backend-implementation-change-request-v1",
            "source_handoff_id": "handoff-7f3d87b2",
            "source_review_run_id": 17,
            "findings": [
                {
                    "finding_id": "PM-CHK-1",
                    "path": "src/main/java/example/product/ProductService.java",
                    "symbol": "ProductService.create",
                    "observed_problem": "재고 검증이 누락됐다.",
                    "expected_result": "음수 재고를 기존 validation error로 거절한다.",
                    "allowed_scope": ["상품 생성 입력 검증과 관련 테스트만 수정한다."],
                    "verification": ["FV-PRODUCT-CREATE", "full-backend"],
                }
            ],
        }
        self.assertEqual([], validate_change_request(self.card(), self.handoff(), self.review_run(), request))

    def test_change_request_rejects_incomplete_finding(self):
        request = {
            "schema": "backend-implementation-change-request-v1",
            "source_handoff_id": "handoff-7f3d87b2",
            "source_review_run_id": 17,
            "findings": [{"finding_id": "PM-CHK-1"}],
        }
        self.assertIn(
            "MISSING_FINDING_PATH:0",
            validate_change_request(self.card(), self.handoff(), self.review_run(), request),
        )

    def test_change_request_rejects_unsafe_or_document_finding_path(self):
        for unsafe_path in ("C:/outside/file.java", "/tmp/file.java", "docs/spec.md", "--all"):
            with self.subTest(path=unsafe_path):
                request = {
                    "schema": "backend-implementation-change-request-v1",
                    "source_handoff_id": "handoff-7f3d87b2",
                    "source_review_run_id": 17,
                    "findings": [
                        {
                            "finding_id": "PM-CHK-1",
                            "path": unsafe_path,
                            "symbol": "ProductService.create",
                            "observed_problem": "재고 검증이 누락됐다.",
                            "expected_result": "음수 재고를 거절한다.",
                            "allowed_scope": ["상품 생성 검증"],
                            "verification": ["FV-PRODUCT-CREATE", "full-backend"],
                        }
                    ],
                }
                self.assertTrue(
                    any(
                        error.startswith("INVALID_FINDING_PATH:0:")
                        for error in validate_change_request(self.card(), self.handoff(), self.review_run(), request)
                    )
                )

    def test_change_request_is_bound_to_handoff_run_and_card_verification(self):
        request = {
            "schema": "backend-implementation-change-request-v1",
            "source_handoff_id": "stale",
            "source_review_run_id": 99,
            "findings": [
                {
                    "finding_id": "PM-CHK-1",
                    "path": "src/main/java/example/product/ProductService.java",
                    "symbol": "ProductService.create",
                    "observed_problem": "재고 검증이 누락됐다.",
                    "expected_result": "음수 재고를 거절한다.",
                    "allowed_scope": ["상품 생성 검증"],
                    "verification": ["UNKNOWN"],
                }
            ],
        }
        errors = validate_change_request(self.card(), self.handoff(), self.review_run(), request)
        self.assertIn("SOURCE_HANDOFF_MISMATCH", errors)
        self.assertIn("SOURCE_REVIEW_RUN_MISMATCH", errors)
        self.assertIn("FINDING_VERIFICATION_MISMATCH:0", errors)

    def test_valid_checkpoint_passes(self):
        self.assertEqual([], validate_checkpoint(self.card(), self.handoff(), self.review_run(), self.checkpoint()))

    def test_checkpoint_requires_exact_committed_paths_and_clean_tree(self):
        checkpoint = self.checkpoint()
        checkpoint["committed_paths"] = checkpoint["committed_paths"][:1]
        checkpoint["clean_worktree"] = False
        errors = validate_checkpoint(self.card(), self.handoff(), self.review_run(), checkpoint)
        self.assertIn("COMMITTED_PATHS_MISMATCH", errors)
        self.assertIn("WORKTREE_NOT_CLEAN", errors)

    def test_checkpoint_requires_matching_handoff_identity(self):
        checkpoint = self.checkpoint()
        checkpoint["source_handoff_id"] = "handoff-stale"

        self.assertIn("SOURCE_HANDOFF_MISMATCH", validate_checkpoint(self.card(), self.handoff(), self.review_run(), checkpoint))

    def test_checkpoint_requires_pm_verification_run(self):
        checkpoint = self.checkpoint()
        checkpoint["verification_run_id"] = 0
        errors = validate_checkpoint(self.card(), self.handoff(), self.review_run(), checkpoint)
        self.assertIn("MISSING_PM_VERIFICATION_RUN_ID", errors)

        checkpoint = self.checkpoint()
        checkpoint["verification_run_id"] = 99
        errors = validate_checkpoint(self.card(), self.handoff(), self.review_run(), checkpoint)
        self.assertIn("PM_VERIFICATION_RUN_MISMATCH", errors)

    def test_checkpoint_requires_pm_focused_verification_coverage(self):
        checkpoint = self.checkpoint()
        checkpoint["focused_verification_readback"] = []
        errors = validate_checkpoint(self.card(), self.handoff(), self.review_run(), checkpoint)
        self.assertTrue(any(error.startswith("PM_FOCUSED_READBACK:") for error in errors))

    def test_handoff_requires_declared_minimum_test_level(self):
        handoff = self.handoff()
        handoff["focused_verification_results"][0]["test_levels"] = ["unit"]

        self.assertIn(
            "TEST_LEVEL_NOT_SATISFIED:FV-PRODUCT-CREATE",
            validate_handoff(self.card(), handoff),
        )

    def test_checkpoint_requires_documentation_impact_resolution(self):
        handoff = self.handoff()
        handoff["documentation_impact"] = {
            "detected": True,
            "details": ["architecture 문서에 새 module seam을 반영해야 한다."],
        }
        unresolved = self.checkpoint()
        self.assertIn(
            "DOCUMENTATION_IMPACT_UNRESOLVED",
            validate_checkpoint(self.card(), handoff, self.review_run(), unresolved),
        )
        resolved = self.checkpoint()
        resolved["documentation_impact_resolution"] = {
            "status": "resolved",
            "changed_paths": ["docs/architecture.md"],
            "commit_sha": "b" * 40,
        }
        self.assertEqual([], validate_checkpoint(self.card(), handoff, self.review_run(), resolved))
        resolved["documentation_impact_resolution"]["commit_sha"] = resolved["commit_sha"]
        self.assertIn(
            "DOCUMENTATION_COMMIT_NOT_DISTINCT",
            validate_checkpoint(self.card(), handoff, self.review_run(), resolved),
        )

    def test_checkpoint_rejects_incomplete_source_handoff(self):
        handoff = {"handoff_id": "handoff-7f3d87b2", "changed_paths": ["src/X.java"]}

        errors = validate_checkpoint(self.card(), handoff, self.review_run(), self.checkpoint())

        self.assertTrue(any(error.startswith("INVALID_HANDOFF:") for error in errors))

    def test_validator_does_not_mutate_inputs(self):
        card = self.card()
        handoff = self.handoff()
        checkpoint = self.checkpoint()
        before = copy.deepcopy((card, handoff, checkpoint))
        validate_handoff(card, handoff)
        validate_checkpoint(card, handoff, self.review_run(), checkpoint)
        self.assertEqual(before, (card, handoff, checkpoint))

    def test_cli_validates_handoff(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            card_path = root / "card.json"
            handoff_path = root / "handoff.json"
            card_path.write_text(json.dumps(self.card(), ensure_ascii=False), encoding="utf-8")
            handoff_path.write_text(json.dumps(self.handoff(), ensure_ascii=False), encoding="utf-8")
            completed = subprocess.run(
                [sys.executable, str(SKILL_DIR / "scripts" / "checkpoint.py"), "handoff", str(card_path), str(handoff_path)],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual({"valid": True, "errors": []}, json.loads(completed.stdout))

    def test_cli_validates_checkpoint_with_card_and_handoff(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = {}
            for name, payload in (
                ("card", self.card()),
                ("handoff", self.handoff()),
                ("review_run", self.review_run()),
                ("checkpoint", self.checkpoint()),
            ):
                paths[name] = Path(directory) / f"{name}.json"
                paths[name].write_text(json.dumps(payload), encoding="utf-8")
            completed = subprocess.run(
                [
                    sys.executable,
                    str(SKILL_DIR / "scripts" / "checkpoint.py"),
                    "checkpoint",
                    str(paths["card"]),
                    str(paths["handoff"]),
                    str(paths["review_run"]),
                    str(paths["checkpoint"]),
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual({"valid": True, "errors": []}, json.loads(completed.stdout))

    def test_cli_rejects_duplicate_json_keys(self):
        with tempfile.TemporaryDirectory() as directory:
            request_path = Path(directory) / "request.json"
            request_path.write_text('{"schema":"wrong","schema":"backend-implementation-change-request-v1"}', encoding="utf-8")
            card_path = Path(directory) / "card.json"
            handoff_path = Path(directory) / "handoff.json"
            run_path = Path(directory) / "run.json"
            card_path.write_text(json.dumps(self.card()), encoding="utf-8")
            handoff_path.write_text(json.dumps(self.handoff()), encoding="utf-8")
            run_path.write_text(json.dumps(self.review_run()), encoding="utf-8")
            completed = subprocess.run(
                [
                    sys.executable,
                    str(SKILL_DIR / "scripts" / "checkpoint.py"),
                    "change-request",
                    str(card_path),
                    str(handoff_path),
                    str(run_path),
                    str(request_path),
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(2, completed.returncode)
            self.assertIn("DUPLICATE_JSON_KEY:schema", completed.stdout)


if __name__ == "__main__":
    unittest.main()
