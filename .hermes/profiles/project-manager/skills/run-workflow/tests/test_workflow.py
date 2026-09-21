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
from workflow import (
    validate_base_sync,
    validate_implementation_admission,
    validate_release,
    validate_release_finding,
    validate_restart,
    validate_review_result,
    validate_state,
    validate_summary_admission,
)

SHA_A = "a" * 40
SHA_B = "b" * 40


class WorkflowContractTest(unittest.TestCase):
    def graph(self):
        path = PROFILE_DIR / "skills" / "build-task-graph" / "tests" / "fixtures" / "valid-graph-draft.json"
        return json.loads(path.read_text(encoding="utf-8"))

    def board(self):
        tasks = []
        ids = {"triage": "t_a1", "impl-1": "t_b2", "review-1": "t_c3", "summary": "t_d4"}
        for card in self.graph()["cards"]:
            status = "done" if card["key"] == "triage" else "ready" if card["key"] == "impl-1" else "todo"
            tasks.append(
                {
                    "key": card["key"],
                    "task_id": ids[card["key"]],
                    "title": card["title"],
                    "card_type": card["card_type"],
                    "assignee": card["assignee"],
                    "workspace": card["workspace"],
                    "status": status,
                    "parents": card["parents"],
                    "runs": [{"status": "completed"}] if status == "done" else [],
                }
            )
        return {"tasks": tasks}

    def review_result(self):
        return {
            "schema": "aggregate-review-result-v1",
            "review_card_key": "review-1",
            "review_task_id": "t_c3",
            "review_run_id": "run-review-2",
            "generation": 1,
            "result": "approved",
            "reviewed_checkpoints": [
                {
                    "implementation_card_key": "impl-1",
                    "implementation_task_id": "t_b2",
                    "checkpoint_sha": SHA_A,
                }
            ],
            "prior_findings": [
                {
                    "source_review_task_id": "t_9f",
                    "source_finding_id": "F-1",
                    "verdict": "correction-required",
                }
            ],
            "findings": [
                {
                    "finding_id": "F-2",
                    "verdict": "resolved",
                    "basis": "회귀 검증",
                    "observed_fact": "수정된 구현이 요구사항을 만족한다.",
                    "evidence": ["checkpoint:a"],
                    "impact": "F-1 차단이 해소되었다.",
                    "resolves": {"source_review_task_id": "t_9f", "source_finding_id": "F-1"},
                }
            ],
        }

    def release(self):
        return {
            "schema": "issue-release-result-v1",
            "issue": {"number": 138, "url": "https://github.com/example/repo/issues/138"},
            "delivery_branch": "p2/issue138",
            "default_branch": "main",
            "final_implementation_sha": SHA_A,
            "snapshot": {
                "inspection_sha": SHA_A,
                "docs_commit_sha": SHA_B,
                "changed_paths": ["docs/current-state.md"],
                "marker_removed": True,
                "clean_worktree": True,
            },
            "pull_request": {
                "number": 201,
                "url": "https://github.com/example/repo/pull/201",
                "base_branch": "main",
                "head_branch": "p2/issue138",
                "head_sha": SHA_B,
                "closes_issue_number": 138,
            },
            "ci": {"head_sha": SHA_B, "required_checks": [{"name": "test", "conclusion": "success"}]},
            "merge": {"merged": True, "merge_sha": "c" * 40, "merged_pr_number": 201, "head_sha": SHA_B},
            "issue_readback": {"state": "closed", "closed_by_pr_number": 201},
        }

    def test_runtime_state_derives_single_claim(self):
        errors, decision = validate_state(
            self.graph(), self.board(), {"branch": "p2/issue138", "head_sha": SHA_A, "clean_worktree": True}
        )
        self.assertEqual([], errors)
        self.assertEqual({"action": "claim", "task_id": "t_b2"}, decision["allowed_transition"])

    def test_runtime_state_rejects_multiple_ready_tasks(self):
        board = self.board()
        board["tasks"][2]["status"] = "ready"
        errors, _ = validate_state(
            self.graph(), board, {"branch": "p2/issue138", "head_sha": SHA_A, "clean_worktree": True}
        )
        self.assertIn("MULTIPLE_READY_TASKS", errors)
        self.assertIn("READY_PARENT_NOT_DONE:review-1", errors)

    def test_runtime_state_rejects_workspace_drift(self):
        board = self.board()
        board["tasks"][1]["workspace"] = "other"
        errors, _ = validate_state(
            self.graph(), board, {"branch": "p2/issue138", "head_sha": SHA_A, "clean_worktree": True}
        )
        self.assertIn("TASK_WORKSPACE_MISMATCH:impl-1", errors)

    def test_runtime_workspace_is_required_on_both_records(self):
        graph = self.graph()
        board = self.board()
        graph["cards"][0].pop("workspace")
        board["tasks"][0].pop("workspace")

        errors, _ = validate_state(
            graph,
            board,
            {"branch": "p2/issue138", "head_sha": SHA_A, "clean_worktree": True},
        )
        self.assertIn("MISSING_WORKSPACE:triage", errors)

    def test_review_result_requires_complete_resolution_chain(self):
        self.assertEqual([], validate_review_result(self.review_result(), self.graph()))
        invalid = self.review_result()
        invalid["findings"] = []
        self.assertIn("PRIOR_FINDING_DROPPED", validate_review_result(invalid, self.graph()))

    def test_review_approval_rejects_new_blocking_finding(self):
        invalid = self.review_result()
        invalid["findings"][0].update(verdict="correction-required", resolves=None)
        self.assertIn("APPROVED_WITH_BLOCKING_FINDING", validate_review_result(invalid, self.graph()))

    def test_summary_admission_requires_complete_parents_and_frozen_clean_sha(self):
        value = {
            "schema": "summary-admission-v1",
            "summary_task_id": "t_d4",
            "latest_review_task_id": "t_c3",
            "latest_review_run_id": "run-review-2",
            "latest_review_result": "approved",
            "direct_parent_task_ids": ["t_b2", "t_c3"],
            "done_parent_task_ids": ["t_b2", "t_c3"],
            "unresolved_finding_ids": [],
            "final_implementation_sha": SHA_A,
            "documentation_commit_shas": [],
            "repository_head_sha": SHA_A,
            "clean_worktree": True,
        }
        self.assertEqual([], validate_summary_admission(value))
        value["done_parent_task_ids"] = ["t_b2"]
        self.assertIn("SUMMARY_PARENT_NOT_DONE", validate_summary_admission(value))

    def test_summary_admission_preserves_code_sha_before_docs_only_commits(self):
        value = {
            "schema": "summary-admission-v1",
            "summary_task_id": "t_d4",
            "latest_review_task_id": "t_c3",
            "latest_review_run_id": "run-review-2",
            "latest_review_result": "approved",
            "direct_parent_task_ids": ["t_b2", "t_c3"],
            "done_parent_task_ids": ["t_b2", "t_c3"],
            "unresolved_finding_ids": [],
            "final_implementation_sha": SHA_A,
            "documentation_commit_shas": [SHA_B],
            "repository_head_sha": SHA_B,
            "clean_worktree": True,
        }

        self.assertEqual([], validate_summary_admission(value))

    def test_release_contract_binds_snapshot_pr_ci_merge_and_auto_close(self):
        self.assertEqual([], validate_release(self.release()))
        invalid = self.release()
        invalid["ci"]["head_sha"] = SHA_A
        invalid["issue_readback"]["closed_by_pr_number"] = None
        errors = validate_release(invalid)
        self.assertIn("CI_HEAD_SHA_MISMATCH", errors)
        self.assertIn("ISSUE_NOT_AUTO_CLOSED_BY_PR", errors)

    def test_release_rejects_non_docs_snapshot_commit_and_pending_check(self):
        invalid = self.release()
        invalid["snapshot"]["changed_paths"].append("src/Main.java")
        invalid["ci"]["required_checks"][0]["conclusion"] = "pending"
        errors = validate_release(invalid)
        self.assertIn("SNAPSHOT_COMMIT_NOT_DOCS_ONLY", errors)
        self.assertIn("CI_CHECK_NOT_SUCCESSFUL:0", errors)

    def test_restart_requires_single_fully_attributed_recovery_task(self):
        value = {
            "schema": "restart-task-v1",
            "issue": 138,
            "delivery_branch": "p2/issue138",
            "workspace": "scratch",
            "marker_baseline_sha": SHA_A,
            "marker_snapshot_sha": SHA_A,
            "current_head_sha": SHA_B,
            "dirty_paths": ["src/Main.java"],
            "path_attribution": [{"path": "src/Main.java", "issue": 138, "evidence": "marker scope", "attributed": True}],
            "recovery_task_id": "t_a1",
            "assignee": "coder",
            "active_recovery_task_ids": ["t_a1"],
            "allowed_scope": ["현재 dirty delta를 검증 가능한 checkpoint로 복원한다."],
            "implementation_card_schema": "backend-implementation-card-v1",
            "required_checkpoint_schema": "backend-implementation-checkpoint-v1",
        }
        self.assertEqual([], validate_restart(value))

        for unsafe in ("../outside", "/tmp/Main.java", "src\\Main.java", "src/**/*.java", "--all"):
            with self.subTest(path=unsafe):
                invalid = copy.deepcopy(value)
                invalid["dirty_paths"] = [unsafe]
                invalid["path_attribution"][0]["path"] = unsafe
                self.assertIn("INVALID_DIRTY_PATHS", validate_restart(invalid))
        value["path_attribution"][0]["attributed"] = False
        self.assertIn("UNATTRIBUTED_DIRTY_PATH:0", validate_restart(value))

    def test_implementation_admission_binds_workspace_and_task(self):
        admission = {
            "schema": "backend-implementation-admission-v1",
            "task_id": "t_a1",
            "issue": 138,
            "workspace": "scratch",
            "card_schema": "backend-implementation-card-v1",
            "restart": None,
        }

        self.assertEqual([], validate_implementation_admission(admission))

    def test_base_sync_requires_user_decision_for_semantic_conflict(self):
        value = {
            "schema": "base-sync-v1",
            "upstream_issue": 137,
            "downstream_issue": 138,
            "upstream_branch": "p2/issue137",
            "downstream_branch": "p2/issue138",
            "upstream_fixed_sha": SHA_A,
            "downstream_pre_sync_sha": SHA_B,
            "strategy": "merge",
            "strategy_owner": "project-manager",
            "strategy_rationale": "공개된 canonical branch history를 보존한다.",
            "changed_paths": ["src/Main.java"],
            "conflicted_paths": [],
            "semantic_impact": "decision-required",
            "decision_task_id": "t_f5",
            "affected_downstream_task_ids": ["t_b2"],
            "post_sync_sha": "c" * 40,
            "verification": {"executor": "gradle-mcp", "checks": ["test"], "result": "pass"},
            "clean_worktree": True,
        }
        self.assertEqual([], validate_base_sync(value))
        value["decision_task_id"] = None
        self.assertIn("MISSING_BASE_SYNC_DECISION_TASK", validate_base_sync(value))

    def test_release_finding_requires_route_for_rework(self):
        value = {
            "schema": "release-finding-v1",
            "source": "ci",
            "source_url": "https://github.com/example/repo/actions/runs/1",
            "target_pr": 201,
            "target_head_sha": SHA_B,
            "finding_id": "CI-test-1",
            "classification": "implementation-rework",
            "evidence": ["test job failed"],
            "routed_task_id": "t_b2",
            "idempotency_key": "pr201:head:CI-test-1",
        }
        self.assertEqual([], validate_release_finding(value))
        value["routed_task_id"] = None
        self.assertIn("MISSING_RELEASE_FINDING_ROUTE", validate_release_finding(value))

    def test_cli_rejects_duplicate_json_keys(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "release.json"
            path.write_text('{"schema":"wrong","schema":"issue-release-result-v1"}', encoding="utf-8")
            completed = subprocess.run(
                [sys.executable, str(SKILL_DIR / "scripts" / "workflow.py"), "validate-release", str(path)],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(2, completed.returncode)
            self.assertIn("DUPLICATE_JSON_KEY:schema", completed.stdout)

    def test_distribution_exposes_only_run_workflow_name_and_existing_files(self):
        distribution = (PROFILE_DIR / "distribution.yaml").read_text(encoding="utf-8")
        capabilities = (PROFILE_DIR / "capabilities.yaml").read_text(encoding="utf-8")
        self.assertIn("skills/run-workflow/SKILL.md", distribution)
        self.assertIn("skills/run-workflow/scripts/workflow.py", distribution)
        self.assertIn("skills/run-workflow/scripts/checkpoint.py", distribution)
        self.assertFalse((SKILL_DIR / "workflow.py").exists())
        self.assertFalse((SKILL_DIR / "checkpoint.py").exists())
        self.assertIn("- run-workflow", capabilities)
        retired_name = "controll" + "-task-graph"
        for path in PROFILE_DIR.rglob("*"):
            if path.is_file() and path.suffix in {".md", ".py", ".yaml"}:
                self.assertNotIn(retired_name, path.read_text(encoding="utf-8"), str(path))
        owned = [line.strip()[2:] for line in distribution.splitlines() if line.strip().startswith("- ")]
        for relative in owned:
            self.assertTrue((PROFILE_DIR / relative).is_file(), relative)


if __name__ == "__main__":
    unittest.main()
