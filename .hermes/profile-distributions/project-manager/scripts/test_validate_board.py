import unittest

from validate_board import validate_board


HEAD = "a" * 40
SNAPSHOT = "b" * 40
REPO = "C:/repo"


def body(*, stale=False, current=None, contract_version="board-contract-v3", extra="", source=None):
    current = current or (SNAPSHOT if stale else HEAD)
    source_block = source or """source:
      kind: repository
      path: docs/requirement.md
      heading: 목표"""
    return f"""contract_version: {contract_version}
task_type: implementation
issue: #20
issue_url: https://github.com/example/repository/issues/20
current_state_sha: {current}
planning_head_sha: {HEAD}
state_freshness: {'stale' if stale else 'fresh'}
decomposition_depth: 1
dependency_path_length: 0
workspace_kind: dir
workspace_path: {REPO}
assignee: prototype-coder

evidence:
  - type: goal
    {source_block}
    claim: 미디어 업로드 계약을 구현한다.
  - type: state
    source:
      kind: repository
      path: docs/current-state.md
      lines: 1-2
    claim: 현재 구현에는 미디어 업로드가 없다.
scope:
- 미디어 업로드를 구현한다.
out_of_scope:
- 상품 연결은 구현하지 않는다.
acceptance_criteria:
- id: AC-MEDIA-1
  outcome: 정상 파일을 업로드할 수 있다.
acceptance_coverage:
- acceptance_id: AC-MEDIA-1
  check_ids: check-media
verification:
- check_id: check-media
  kind: behavioral
  CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD={REPO}
  EXPECT: process exit code 0이며 지정한 테스트가 통과한다.
review_handoff_contract:
  action: request-review
  required_metadata: verified_sha, validator, check_id, result, changed_paths, residual_risk
  reviewer_readback: show, runs, comments
{extra}"""


def envelope(task_id, task_body, *, status="ready", parents=None, children=None, runs=None, assignee="prototype-coder"):
    return {
        "task": {
            "id": task_id,
            "title": "미디어 계약 구현",
            "body": task_body,
            "status": status,
            "assignee": assignee,
            "workspace_kind": "dir",
            "workspace_path": REPO,
        },
        "parents": parents or [],
        "children": children or [],
        "comments": [],
        "events": [],
        "runs": runs or [],
    }


def source_loader(_sha, path):
    sources = {
        "docs/requirement.md": "# 목표\n미디어 업로드 계약\n",
        "docs/current-state.md": "# 현재 상태\n미디어 업로드 없음\n",
    }
    if path not in sources:
        raise FileNotFoundError(path)
    return sources[path]


def reconciliation_body():
    result = body(stale=True).replace("task_type: implementation", "task_type: reconciliation")
    result = result.replace("assignee: prototype-coder", "assignee: project-manager")
    result = result.replace(
        "- check_id: check-media\n"
        "  kind: behavioral\n"
        "  CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD=C:/repo\n"
        "  EXPECT: process exit code 0이며 지정한 테스트가 통과한다.\n"
        "review_handoff_contract:\n"
        "  action: request-review\n"
        "  required_metadata: verified_sha, validator, check_id, result, changed_paths, residual_risk\n"
        "  reviewer_readback: show, runs, comments\n",
        "- check_id: check-head\n"
        "  kind: state\n"
        f"  CHECK: git -C {REPO} rev-parse HEAD\n"
        f"  EXPECT: process exit code 0이며 출력이 {HEAD}와 일치한다.\n"
        "- check_id: check-snapshot\n"
        "  kind: state\n"
        f"  CHECK: git -C {REPO} show {HEAD}:docs/current-state.md\n"
        f"  EXPECT: process exit code 0이며 문서가 {SNAPSHOT}을 기준 SHA로 기록한다.\n"
        "reconciliation_result_contract:\n"
        "  evidence: comment.reconciliation_result\n"
        "  required_fields: planning_head_sha, current_state_sha, gap_status, production_sources, test_sources, migration_sources, module_boundaries, unknowns, next_action\n",
    )
    return result


class BoardContractTest(unittest.TestCase):
    def codes(self, board, phase="post", snapshot_drift_paths=None):
        return {
            finding.code
            for finding in validate_board(
                board,
                source_loader,
                phase=phase,
                snapshot_drift_paths=snapshot_drift_paths,
            )
        }

    def test_valid_contract_passes(self):
        self.assertEqual([], validate_board([envelope("t_aaaaaaaa", body())], source_loader))

    def test_rejects_planning_sha_that_is_not_repository_head(self):
        board = [envelope("t_aaaaaaaa", body())]
        findings = validate_board(board, source_loader, repository_head=SNAPSHOT)
        self.assertIn("PLANNING_HEAD_MISMATCH", {finding.code for finding in findings})

    def test_v3_allows_harness_only_drift_after_inspection_snapshot(self):
        board = [
            envelope(
                "t_aaaaaaaa",
                body(
                    current=SNAPSHOT,
                    contract_version="board-contract-v3",
                ),
            )
        ]

        self.assertEqual(
            set(),
            self.codes(
                board,
                snapshot_drift_paths=lambda _snapshot, _planning: [
                    ".hermes/profile-distributions/project-manager/SOUL.md",
                    "scripts/setup-hermes.sh",
                ],
            ),
        )

    def test_v3_rejects_fresh_claim_when_covered_source_changed(self):
        board = [
            envelope(
                "t_aaaaaaaa",
                body(
                    current=SNAPSHOT,
                    contract_version="board-contract-v3",
                ),
            )
        ]

        self.assertIn(
            "SNAPSHOT_COVERAGE_DRIFT",
            self.codes(
                board,
                snapshot_drift_paths=lambda _snapshot, _planning: [
                    "src/main/java/io/example/CatalogProduct.java",
                ],
            ),
        )

    def test_v3_allows_stale_reconciliation_draft_when_covered_source_changed(self):
        body_text = reconciliation_body()
        board = [
            envelope(
                "t_aaaaaaaa",
                body_text,
                status="todo",
                assignee="project-manager",
            )
        ]

        self.assertEqual(
            set(),
            self.codes(
                board,
                phase="draft",
                snapshot_drift_paths=lambda _snapshot, _planning: [
                    "src/main/java/io/example/CatalogProduct.java",
                ],
            ),
        )

    def test_rejects_combined_heading_and_line_locator(self):
        board = [envelope("t_aaaaaaaa", body(source="source: docs/requirement.md#목표:L1-L2"))]
        self.assertIn("LOCATOR_FORMAT", self.codes(board))

    def test_rejects_missing_committed_heading(self):
        source = """source:
      kind: repository
      path: docs/requirement.md
      heading: 없는 제목"""
        board = [envelope("t_aaaaaaaa", body(source=source))]
        self.assertIn("LOCATOR_NOT_FOUND", self.codes(board))

    def test_rejects_legacy_locator_string(self):
        board = [envelope("t_aaaaaaaa", body(source="source: docs/requirement.md#목표"))]
        self.assertIn("LOCATOR_FORMAT", self.codes(board))

    def test_rejects_preexecution_review_result(self):
        extra = """review_handoff:
  metadata:
    verified_sha: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    result: fail
"""
        board = [envelope("t_aaaaaaaa", body(extra=extra))]
        self.assertIn("PREEXEC_REVIEW_EVIDENCE", self.codes(board))

    def test_rejects_self_runtime_binding(self):
        extra = """runtime_bindings:
  pr_number:
    producer_task_id: self
    evidence: comment.pr_number
"""
        board = [envelope("t_aaaaaaaa", body(extra=extra))]
        self.assertIn("RUNTIME_BINDING_SELF_PRODUCER", self.codes(board))

    def test_allows_machine_evidence_locator_for_external_runtime_binding(self):
        producer_body = body().replace("task_type: implementation", "task_type: pr-create")
        producer_body = producer_body.replace(
            "CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD=C:/repo",
            "CHECK: gh pr create --repo example/repository --title 제목 --body-file pr-body.md",
        )
        producer_body = producer_body.replace("kind: behavioral", "kind: state")
        producer_body += """\nproduces:
  pr_number:
    evidence: comment.pr_number
  pr_url:
    evidence: comment.pr_url
"""
        consumer_body = body().replace("dependency_path_length: 0", "dependency_path_length: 1")
        consumer_body += """\ndepends_on_task_id: t_aaaaaaaa
runtime_bindings:
  pr_number:
    producer_task_id: t_aaaaaaaa
    evidence: comment.pr_number
"""
        board = [
            envelope("t_aaaaaaaa", producer_body, status="done", children=["t_bbbbbbbb"], runs=[{"status": "succeeded"}]),
            envelope("t_bbbbbbbb", consumer_body, parents=["t_aaaaaaaa"]),
        ]
        self.assertEqual([], validate_board(board, source_loader))

    def test_rejects_runtime_binding_without_pr_create_contract(self):
        producer_body = body().replace("task_type: implementation", "task_type: pr-create")
        consumer_body = body().replace("dependency_path_length: 0", "dependency_path_length: 1")
        consumer_body += """\ndepends_on_task_id: t_aaaaaaaa
runtime_bindings:
  pr_number:
    producer_task_id: t_aaaaaaaa
    evidence: comment.pr_number
"""
        board = [
            envelope("t_aaaaaaaa", producer_body, status="done", children=["t_bbbbbbbb"], runs=[{"status": "succeeded"}]),
            envelope("t_bbbbbbbb", consumer_body, parents=["t_aaaaaaaa"]),
        ]
        self.assertIn("PR_PRODUCER_CONTRACT_MISSING", self.codes(board))

    def test_rejects_undeclared_runtime_operand(self):
        invalid = body().replace(
            "CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD=C:/repo",
            "CHECK: gh pr checks ${pr_number} --repo example/repository",
        )
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("RUNTIME_BINDING_MISSING", self.codes(board))

    def test_rejects_preexecution_manual_verdict(self):
        extra = """manual_review:
  reviewer: reviewer-general
  verdict: approved | request-changes | blocked
"""
        board = [envelope("t_aaaaaaaa", body(extra=extra))]
        self.assertIn("PREEXEC_MANUAL_REVIEW_EVIDENCE", self.codes(board))

    def test_requires_behavioral_acceptance_coverage(self):
        invalid = body().replace("kind: behavioral", "kind: structural")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("IMPLEMENTATION_BEHAVIOR_COVERAGE", self.codes(board))

    def test_stale_board_contains_only_reconciliation(self):
        first = body(stale=True).replace("task_type: implementation", "task_type: reconciliation")
        second = body(stale=True)
        board = [
            envelope("t_aaaaaaaa", first, status="ready", children=["t_bbbbbbbb"]),
            envelope("t_bbbbbbbb", second, status="blocked", parents=["t_aaaaaaaa"]),
        ]
        self.assertIn("STALE_GRAPH_NOT_TWO_PHASE", self.codes(board))

    def test_reconciliation_requires_result_contract(self):
        invalid = reconciliation_body().replace(
            "reconciliation_result_contract:\n"
            "  evidence: comment.reconciliation_result\n"
            "  required_fields: planning_head_sha, current_state_sha, gap_status, production_sources, test_sources, migration_sources, module_boundaries, unknowns, next_action\n",
            "",
        )
        board = [envelope("t_aaaaaaaa", invalid, assignee="project-manager")]
        self.assertIn("RECONCILIATION_RESULT_CONTRACT_MISSING", self.codes(board))

    def test_reconciliation_snapshot_check_reads_planning_tree(self):
        invalid = reconciliation_body().replace(
            f"show {HEAD}:docs/current-state.md",
            f"show {SNAPSHOT}:docs/current-state.md",
        )
        board = [envelope("t_aaaaaaaa", invalid, assignee="project-manager")]
        self.assertIn("RECONCILIATION_SNAPSHOT_CHECK_INVALID", self.codes(board))

    def test_reconciliation_head_check_names_literal_planning_sha(self):
        invalid = reconciliation_body().replace(
            f"EXPECT: process exit code 0이며 출력이 {HEAD}와 일치한다.",
            "EXPECT: process exit code 0이며 출력이 planning_head_sha와 일치한다.",
        )
        board = [envelope("t_aaaaaaaa", invalid, assignee="project-manager")]
        self.assertIn("RECONCILIATION_HEAD_CHECK_INVALID", self.codes(board))

    def test_reconciliation_rejects_tautological_board_list_check(self):
        invalid = reconciliation_body().replace(
            f"CHECK: git -C {REPO} show {HEAD}:docs/current-state.md",
            "CHECK: hermes --profile project-manager kanban --board issue-20 list --json",
        )
        board = [envelope("t_aaaaaaaa", invalid, assignee="project-manager")]
        self.assertIn("RECONCILIATION_TAUTOLOGICAL_CHECK", self.codes(board))

    def test_reconciliation_rejects_self_block_for_expected_snapshot_mismatch(self):
        board = [
            envelope(
                "t_aaaaaaaa",
                reconciliation_body(),
                assignee="project-manager",
                runs=[
                    {
                        "status": "blocked",
                        "outcome": "blocked",
                        "summary": "현재 planning HEAD와 snapshot SHA 불일치 조정이 선행되어야 한다.",
                    }
                ],
            )
        ]
        self.assertIn("RECONCILIATION_SELF_BLOCKED", self.codes(board))

    def test_rejects_english_only_human_text(self):
        invalid = body().replace("claim: 미디어 업로드 계약을 구현한다.", "claim: Implement media upload contract.")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("HUMAN_TEXT_NOT_KOREAN", self.codes(board))

    def test_rejects_dependency_alias_in_body(self):
        invalid = body(extra="depends_on_task_id: t01\n")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("TASK_REFERENCE_INVALID", self.codes(board))

    def test_rejects_done_task_with_only_failed_run(self):
        board = [envelope("t_aaaaaaaa", body(), status="done", runs=[{"status": "failed"}])]
        self.assertIn("FALSE_DONE", self.codes(board))

    def test_rejects_git_command_labeled_behavioral(self):
        invalid = body().replace(
            "CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD=C:/repo",
            "CHECK: git -C C:/repo status --porcelain",
        )
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("CHECK_KIND_MISMATCH", self.codes(board))

    def test_rejects_check_workspace_mismatch(self):
        invalid = body().replace("CWD=C:/repo", "CWD=C:/other")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("CHECK_WORKSPACE_MISMATCH", self.codes(board))

    def test_rejects_expect_without_success_condition(self):
        invalid = body().replace(
            "EXPECT: process exit code 0이며 지정한 테스트가 통과한다.",
            "EXPECT: 지정한 테스트 결과를 확인한다.",
        )
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("EXPECT_SUCCESS_CONDITION_MISSING", self.codes(board))

    def test_rejects_prose_check(self):
        invalid = body().replace(
            "CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaTest; CWD=C:/repo",
            "CHECK: 관련 테스트를 적절하게 검증한다",
        )
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("CHECK_PROSE_NOT_EXECUTABLE", self.codes(board))

    def test_post_phase_requires_one_ready_task(self):
        board = [envelope("t_aaaaaaaa", body(), status="blocked")]
        self.assertIn("READY_COUNT_INVALID", self.codes(board))

    def test_draft_phase_requires_zero_ready_tasks(self):
        blocked = [envelope("t_aaaaaaaa", body(), status="blocked")]
        ready = [envelope("t_aaaaaaaa", body(), status="ready")]
        self.assertNotIn("READY_COUNT_INVALID", self.codes(blocked, phase="draft"))
        self.assertIn("READY_COUNT_INVALID", self.codes(ready, phase="draft"))

    def test_ready_task_requires_done_dependencies(self):
        parent = body().replace("task_type: implementation", "task_type: commit")
        child = body().replace("dependency_path_length: 0", "dependency_path_length: 1")
        child += "depends_on_task_id: t_aaaaaaaa\n"
        board = [
            envelope("t_aaaaaaaa", parent, status="blocked", children=["t_bbbbbbbb"]),
            envelope("t_bbbbbbbb", child, parents=["t_aaaaaaaa"]),
        ]
        self.assertIn("READY_DEPENDENCY_UNSATISFIED", self.codes(board))

    def test_rejects_native_and_body_assignee_mismatch(self):
        invalid = body().replace("assignee: prototype-coder", "assignee: reviewer-general")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("ASSIGNEE_MISMATCH", self.codes(board))

    def test_rejects_invalid_state_freshness_enum(self):
        invalid = body().replace("state_freshness: fresh", "state_freshness: current")
        board = [envelope("t_aaaaaaaa", invalid)]
        self.assertIn("STATE_FRESHNESS_INVALID", self.codes(board))

    def test_quality_task_requires_freeze_and_manual_contracts(self):
        invalid = body().replace("task_type: implementation", "task_type: quality-review")
        board = [envelope("t_aaaaaaaa", invalid)]
        codes = self.codes(board)
        self.assertIn("QUALITY_FREEZE_PRODUCER_MISSING", codes)
        self.assertIn("MANUAL_REVIEW_CONTRACT_MISSING", codes)

    def test_rejects_empty_and_malformed_input(self):
        self.assertIn("EMPTY_BOARD", self.codes([]))
        self.assertIn("INPUT_SCHEMA_INVALID", self.codes([{}]))

    def test_rejects_duplicate_task_ids(self):
        task = envelope("t_aaaaaaaa", body())
        self.assertIn("DUPLICATE_TASK_ID", self.codes([task, task]))

    def test_cycle_with_runtime_binding_returns_finding_instead_of_crashing(self):
        first = body(extra="""runtime_bindings:
  value:
    producer_task_id: t_bbbbbbbb
    evidence: comment.value
""").replace("depends_on_task_id: t_00000000\n", "")
        first = first.replace("dependency_path_length: 0", "dependency_path_length: 1")
        second = body().replace("dependency_path_length: 0", "dependency_path_length: 1")
        board = [
            envelope("t_aaaaaaaa", first, parents=["t_bbbbbbbb"], children=["t_bbbbbbbb"]),
            envelope("t_bbbbbbbb", second, parents=["t_aaaaaaaa"], children=["t_aaaaaaaa"], status="blocked"),
        ]
        self.assertIn("DEPENDENCY_CYCLE", self.codes(board))


if __name__ == "__main__":
    unittest.main()
