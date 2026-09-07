# board-contract-v1

이 문서는 `project-manager`가 생성하는 Amaazon Kanban graph의 동결된 계약이다. 동일
버전의 board는 이 문서와 `scripts/validate_board.py`로만 판정한다. 새로운 규칙은 기존
board에 소급하지 않고 새 contract version과 regression test로 추가한다.

## 생성 단계

1. Issue와 `planning_head_sha`의 committed evidence를 읽는다.
2. native `show --json` envelope 형태의 임시 graph JSON을 만든다. 모든 task는 `todo` 또는
   `blocked`이며 `ready`는 0개다. 임시 ID는 파일 안에서만 canonical `t_<hex>` 형태로
   사용하며 board에 저장하지 않는다.
3. `validate_board.py --input <draft.json> --phase draft`가 exit code 0인지 확인한다.
4. prerequisite부터 생성하고 실제 ID를 read-back한 뒤 downstream body에 치환한다.
5. 모든 task를 `todo` 또는 `blocked`로 생성하고 dependency를 연결한 뒤
   `validate_board.py --board <slug> --phase draft`로 native read-back을 검사한다.
6. draft 검사가 exit code 0일 때만 dependency가 충족된 task 하나를 `ready`로 승격한다.
7. `validate_board.py --board <slug> --phase post`가 exit code 0일 때만 graph 생성을
   완료했다고 보고한다. `post`는 정확히 하나의 `ready`와 완료된 parent를 요구한다.

Validator exit code `1`은 contract finding, `2`는 수집·Git 등 infrastructure failure다. 둘 다
promotion과 완료 보고를 금지한다.

## Stale two-phase rule

`current_state_sha != planning_head_sha` 또는 `state_freshness: stale`이면 board에는
`reconciliation` 또는 `investigation` task 하나만 생성한다. 구현·품질·종료 task를 미리
만들지 않는다. Reconciliation 완료 후 확정된 committed SHA와 gap으로 새 fresh graph를
생성한다. Native unfinished body update에 의존하지 않는다.

## Body identity

모든 task body는 다음을 포함한다.

```text
contract_version: board-contract-v1
task_type: reconciliation | investigation | implementation | commit | quality-review | coordinator | pr-create | finalization
issue: #<number>
issue_url: <literal URL>
current_state_sha: <literal SHA>
planning_head_sha: <literal SHA>
state_freshness: fresh | stale
decomposition_depth: 1..3
dependency_path_length: <native parent graph에서 계산한 값>
workspace_kind: dir | scratch | worktree
workspace_path: <native raw path>
assignee: <Profile>
```

Native parent가 있으면 body의 `depends_on_task_id` 또는 comma-separated
`depends_on_task_ids`가 같은 actual ID 집합을 가리킨다. `t01`, `step-1`, 제목 alias는
허용하지 않는다.

## Evidence source

Legacy 문자열 locator는 금지한다. 각 source는 다음 중 하나다.

```yaml
source:
  kind: repository
  path: docs/requirement/example.md
  heading: 정확한 제목
```

```yaml
source:
  kind: repository
  path: docs/requirement/example.md
  lines: 10-20
```

```yaml
source:
  kind: issue
  url: https://github.com/owner/repository/issues/20
  heading: 완료 기준
```

```yaml
source:
  kind: task
  task_id: t_<actual-id>
```

Repository source는 `heading`과 `lines` 중 정확히 하나만 사용하고
`planning_head_sha`의 Git object에서 검증한다.

## Acceptance coverage

Implementation task의 모든 acceptance criterion은 하나 이상의 behavioral check에 연결된다.

```yaml
acceptance_criteria:
- id: AC-MEDIA-STATE
  outcome: 업로드 상태 전이가 요구사항을 만족한다.
acceptance_coverage:
- acceptance_id: AC-MEDIA-STATE
  check_ids: check-media-state
verification:
- check_id: check-media-state
  kind: behavioral
  CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaStateTest; CWD=C:/repo
  EXPECT: process exit code 0이며 지정한 테스트가 통과한다.
```

`ModularityTest`와 `git diff --check`는 각각 structural/style check이며 behavioral acceptance를
단독으로 충족하지 않는다.

## Review lifecycle

Task 생성 시에는 실행 후 값을 저장하지 않는다.

```yaml
review_handoff_contract:
  action: request-review
  required_metadata: verified_sha, validator, check_id, result, changed_paths, residual_risk
  reviewer_readback: show, runs, comments
```

실제 `verified_sha`, `result: pass | fail`, `changed_paths`는 검증 후 native
`request-review` metadata/comment에 기록한다. 실행 전 body의 `review_handoff.metadata`는
invalid다.

수동 검토도 생성 시에는 `manual_review_contract`의 reviewer, subject, evidence,
`verdict_values: approved | request-changes | blocked`만 선언한다. 실제 verdict는 검토 후 native comment/run/metadata에
기록하며 실행 전 body의 `manual_review.verdict`는 invalid다.

## Runtime producers

`runtime_bindings.producer_task_id: self`는 금지한다. 외부 값은 별도 producer task가
생성하고 durable comment/run/metadata field에 기록한다. 특히 `pr-create` task가 실제
`pr_number`와 PR URL을 생산하고, 후속 `finalization` task가 이를 소비한다.
