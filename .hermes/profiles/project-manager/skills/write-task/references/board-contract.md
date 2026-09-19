---
name: board-contract
version: 3.0.0
---

# Board Contract

> **Legacy board-contract-v3.** 현재 PM workflow나 backend Impl card의 authority가 아니다.
> Graph는 `build-task-graph/references/board-contract.md`, backend Impl body와 handoff는
> `.hermes/profiles/coder/skills/run-impl-card/references/` contract를 따른다.

이 문서는 archived `board-contract-v3` fixture와 `scripts/validate_board.py`의 과거 판정 규칙만
설명한다. Current graph나 task를 생성·수정하는 기준으로 사용하지 않는다.

## Authority

이 문서는 v3 fixture의 persisted task body, planning identity, verification/handoff 형식과
legacy validator 조건만 설명한다. Current workflow와 충돌하면 `build-task-graph`와 Coder
implementation contract를 따른다.

## Contract boundary

이 문서는 저장된 task body와 validator가 판정하는 graph 불변조건만 소유한다.
`write-task/SKILL.md`가 생성 순서와 discovery procedure를, `SOUL.md`가 PM lifecycle·
권한·승인·실패 routing을 소유한다. 이 문서의 예시는 schema와 판정 기준을 설명할 뿐,
실행 절차를 대체하지 않는다.

validator exit code `1`은 contract finding, `2`는 수집·Git 등 infrastructure failure다.
둘 다 promotion과 완료 보고를 금지한다.

### Graph invariants

- draft fixture는 `todo` 또는 `blocked`만 포함하고 `ready`는 0개다.
- native create의 자동 promotion 여부는 사실로 read-back하며, zero-ready 상태를 가정하지 않는다.
- post gate는 의도한 executable leaf 하나와 완료된 parent를 요구한다.
- dependency body에는 native에서 read-back한 실제 `t_<hex>` ID만 사용한다.
- task 생성 후 `planning_head_sha`가 바뀌면 미완료 task를 수동 재승격하지 않는다.

```yaml
lifecycle_flags:
  fixture_zero_ready_draft_required: true
  native_zero_ready_draft_required: false
  native_post_required: true
```

## Planning identity와 snapshot freshness

`planning_head_sha`는 validator 실행 시 repository `HEAD`와 일치해야 한다. Task 생성 후 HEAD가
바뀌면 unfinished task를 실행·수동 재승격하지 않고 supported lifecycle로 archive/recreate한다.

`current_state_sha`는 `docs/current-state.md`가 직접 확인한 application snapshot SHA다. 문서 갱신
commit은 inspection SHA에 포함되지 않을 수 있으므로, v3은 두 SHA의 literal equality만으로 stale을
판정하지 않는다.

다음 중 하나면 snapshot은 `fresh`다.

1. `current_state_sha == planning_head_sha`.
2. `current_state_sha`가 `planning_head_sha`의 ancestor이고, 두 SHA 사이 변경 경로에 다음
   application-covered path가 하나도 없다.

```text
.github/
amaazon-front/
build.gradle
config/
gradle/
settings.gradle
src/
```

위 path가 변경됐거나 snapshot SHA가 planning SHA의 ancestor가 아니면 `stale`이며, board에는
`reconciliation` 또는 `investigation` task 하나만 생성한다. 요구사항·문서·`.hermes/`·agent setup
script만 변경된 경우는 application snapshot을 stale로 만들지 않는다. 요구사항 변경은 task author가
목표와 acceptance contract를 다시 읽어 반영해야 하지만, 그 자체로 구현 상태 snapshot을 stale로
판정하지 않는다.

Snapshot/planning SHA 불일치는 reconciliation task의 입력일 수 있으나 자기 자신을 block할 사유가
아니다. Reconciliation 완료 후 실제 gap이 있으면 fresh implementation graph를 새로 생성한다. gap이
없으면 implementation task를 만들지 않고 Issue/완료 근거를 정리한다. Native unfinished body update에
의존하지 않는다.

## Body identity

모든 task body는 다음을 포함한다.

```text
contract_version: board-contract-v3
task_type: reconciliation | investigation | implementation | commit | quality-review | coordinator | pr-create | finalization
issue: #<number>
issue_url: <literal URL>
current_state_sha: <literal inspection SHA>
planning_head_sha: <literal planning HEAD>
state_freshness: fresh | stale
decomposition_depth: 1..3
dependency_path_length: <native parent graph에서 계산한 값>
workspace_kind: dir | scratch | worktree
workspace_path: <native raw path>
assignee: <Profile>

evidence: <typed goal/state/constraint entries>
scope:
- <owned outcome>
out_of_scope:
- <explicit exclusion>
acceptance_criteria:
- id: AC-<stable-id>
  outcome: <observable Korean result>
acceptance_coverage:
- acceptance_id: AC-<stable-id>
  check_ids: <one or more check IDs>
verification:
- check_id: <stable check ID>
  kind: behavioral | structural | style | state | manual
  CHECK: <one executable operation with exact target and CWD>
  EXPECT: <success-specific condition>
```

Native parent가 있으면 body의 `depends_on_task_id` 또는 comma-separated
`depends_on_task_ids`가 같은 actual ID 집합을 가리킨다. `t01`, `step-1`, 제목 alias는
허용하지 않는다.

## Authoring invariants

Task body는 실제 multiline text로 작성한다. Human-readable title, `claim`, scope,
acceptance outcome, manual-review description, and explanatory text are Korean; literal IDs,
URLs, paths, SHAs, symbols, CLI commands, enum values, and structured keys remain literal.

Each `CHECK` is one executable operation. Every Gradle check records the intended `gradle-mcp`
request and discovered Java FQCNs; a shell Gradle command, unqualified test name, or prose test
target is invalid. Every persisted decision is source-backed and repository-relative. Credentials,
tokens, connection strings, raw tool output, prompts, and reasoning never enter a task body,
comment, metadata, or audit evidence.

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

## Reconciliation contract

Reconciliation/investigation task는 다음 두 state check를 포함한다.

```text
- check_id: check-head
  kind: state
  CHECK: git -C <workspace_path> rev-parse HEAD
  EXPECT: process exit code 0이며 출력이 <literal planning_head_sha>와 일치한다.
- check_id: check-snapshot
  kind: state
  CHECK: git -C <workspace_path> show <literal planning_head_sha>:docs/current-state.md
  EXPECT: process exit code 0이며 문서가 <literal current_state_sha>를 확인 기준 SHA로 기록한다.
```

`current_state_sha:docs/current-state.md`는 snapshot을 기록한 후속 문서 commit을 읽지 못할 수
있으므로 snapshot check의 Git object로 사용하지 않는다. `kanban ... list --json`은 저장된 body를
다시 출력할 뿐 reconciliation 결과를 증명하지 못하므로 verification CHECK로 사용하지 않는다.

Task 생성 시 실행 결과를 쓰지 않고 다음 handoff contract만 선언한다.

```yaml
reconciliation_result_contract:
  evidence: comment.reconciliation_result
  required_fields: planning_head_sha, current_state_sha, gap_status, production_sources, test_sources, migration_sources, module_boundaries, unknowns, next_action
```

실행 후 Project Manager는 committed production source, test, migration, module boundary를 직접
확인한 결과를 위 field로 comment에 기록한다. Repository HEAD가 planning SHA와 달라졌거나 독립적인
infrastructure/evidence 장애가 있을 때만 block한다.

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
`request-review` metadata/comment에 기록한다. 실행 전 body의 `review_handoff.metadata`는 invalid다.

수동 검토도 생성 시에는 `manual_review_contract`의 reviewer, subject, evidence,
`verdict_values: approved | request-changes | blocked`만 선언한다. 실제 verdict는 검토 후 native
comment/run/metadata에 기록하며 실행 전 body의 `manual_review.verdict`는 invalid다.

## Runtime producers

`runtime_bindings.producer_task_id: self`는 금지한다. 외부 값은 별도 producer task가 생성하고
durable comment/run/metadata field에 기록한다. 특히 `pr-create` task가 실제 `pr_number`와 PR
URL을 생산하고, 후속 `finalization` task가 이를 소비한다.
