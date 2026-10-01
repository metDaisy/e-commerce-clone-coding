---
name: run-impl-card
description: Use when admitting and running a PM-authored backend Impl card through its PM checkpoint.
version: 0.9.0
author: "Amaazon project"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, testing, kanban, checkpoint]
    related_skills: [implement]
requires_toolsets: [kanban]
---

# Run Implementation Card

PM이 작성한 self-contained Impl card를 backend code와 test로 구현하고 같은 card의 PM
checkpoint로 넘긴다. Profile의 `SOUL.md`가 안정적인 Coder 정체성과 권한 경계를 소유한다. 시작 전에
[`references/implementation-card-contract.md`](references/implementation-card-contract.md)를 읽고 card
body 소비, Coder handoff와 changes-request input schema를 확인한다.

Repository의 `AGENTS.md`가 강제 규칙을, `docs/backend-development-guide.md`와
`docs/backend-test-guide.md`가 backend 구현·test convention을 소유한다. Requirement와 Triage는
Coder의 입력이 아니다.

## Procedure

### 1. Task admission

1. Dispatcher가 제공한 현재 task를 `kanban_show`로 읽는다. Native task ID, assignee,
   `running` status, parent handoff, comments와 body를 read-back한다. Latest valid PM-owned
   `backend-implementation-admission-v1` comment에서 actual task ID, Issue, card schema와 create-intent
   workspace를 읽고, 이를 dispatcher가 시작한 actual process cwd/worktree identity와 비교한다. Native
   `kanban_show` task 자체가 workspace를 반환한다고 가정하지 않는다.
2. Body가 valid JSON이며 legacy `backend-implementation-card-v2` 또는 current
   `backend-implementation-card-v3`,
   `card_type: implementation`인지 확인한다. Canonical contract의 required field와 reference가
   모두 존재해야 한다.
3. Card의 goal, effective behavior, scope, exclusions, contracts, acceptance, focused verification와
   full backend verification을 읽는다. 모든 focused/full gate의 literal `runner`, `CHECK`, `CWD`, `EXPECT`를
   read-back하고 `CHECK`가 actual FQCN selector를 갖지 않거나 `EXPECT`가 success-only oracle이 아니면 실행 전
   `needs_input`으로 block한다. Traceability locator는 PM 근거이며 requirement를 다시 해석하라는 지시가 아니다.
4. Git branch와 status 및 native run history를 읽어 initial run, changes-requested rework와
   restart recovery를 구분한다. Initial run은 staged, unstaged 또는 untracked path가 하나라도 있으면
   덮어쓰기·commit·stash·reset·clean하지 않고 `kanban_block(kind="needs_input")`으로 중단한다.
   Restart recovery는 latest matching admission comment의 valid `restart-task-v1`이 현재 task ID, assignee,
   workspace, Issue와 exact dirty path를 모두 결합하고 body가 정상 Impl contract일 때만 dirty 상태를
   인수한다. Rework run은 latest native
   `request-changes` transition, 직전 `review_requested` handoff와 correlated change-request comment를
   먼저 검증하고, dirty paths가 직전 handoff의 `changed_paths`와 정확히 같을 때만 이어서 작업한다.
   다른 dirty path 또는 ID 불일치는 block한다.
5. Card가 불완전하거나 서로 모순되면 task body를 고치지 않고 정확한 누락·충돌과 필요한 PM
   조치를 block reason에 기록한다.

완료 기준: actual task/run/cwd, PM admission comment, immutable card schema, 실행 mode와 허용된 worktree
상태가 모두 일치한다. 하나라도 다르면 `implement`를 로드하기 전에 block한다.

### 2. Implementation execution

1. `implement` Skill을 load하고 admitted mode와 함께 implementation map, test-first seam, 최소 구현, focused
   loop, bounded simplification, full backend verification과 self-review 절차를 순서대로 수행한다.
2. 기존 code와 card의 `implementation_context.current_behavior`가 달라 acceptance나 public contract가
   변하면 제품 의미를 다시 해석하지 않고 근거와 영향으로 block한다.
3. `external_system.applicable=true`이면 consumer port에서 adapter, producer public surface와 required
   unavailable/error semantics까지 실제 실행 경로를 확인한다. 항상 `Optional.empty()`, default value 또는
   unsupported 결과를 반환하는 placeholder adapter는 query/mutation/merge acceptance의 pass 근거가 아니다.
   Producer public contract가 absent/partial이면 해당 behavior를 구현 완료로 포장하지 않고 PM review requested
   comment와 `kanban_block(kind="needs_input")`으로 올린다.
4. 모든 Gradle 작업은 `gradle-mcp`로만 실행한다. Terminal, shell 또는 IDE의 Gradle 실행,
   test 약화·비활성화, validator 우회와 검증 범위 축소는 금지한다.
5. Requirement, architecture, ADR, glossary, `current-state.md`, Profile/workflow와 사용자 문서를
   수정하지 않는다. 문서 영향은 handoff로만 보고한다.

### 2.5 Problem escalation to PM

구현·검증 중 문제가 생겨 Coder가 스스로 완료하거나 안전하게 다음 단계로 진행할 수 없으면,
이를 조용히 해결·무시하거나 불완전한 `request-review` handoff로 포장하지 않는다.

1. 현재 task에 관찰 사실, 영향, 재현 근거와 PM에게 필요한 결정·조치를 comment로 남긴다. Comment에는
   `PM review requested`를 명시한다.
2. PM의 판단·환경 조치·계약 확인이 필요하면 native `kanban_block(kind="needs_input")`으로 task를
   block하고 reason을 같은 내용으로 기록한다. `needs_input`은 별도 status가 아니라 `blocked` task의
   typed reason이다.
3. `request-review`는 필수 구현·focused verification·full backend verification이 모두 통과한 완료
   handoff에만 사용한다. 문제 발생을 PM에게 알리는 경로는 `blocked + needs_input + comment`이다.
4. PM이 문제를 read-back한 뒤 사용자 판단이 필요하다고 판정하면 PM-owned blocked Decision으로
   escalation한다. Coder는 business policy·authorization·consistency·public contract를 대신 결정하지
   않는다.

### 3. PM checkpoint handoff

1. Git status와 diff를 읽고 모든 changed path가 card scope 또는 필수 propagation인지 확인한다.
   문서 path나 설명할 수 없는 path가 있으면 review를 요청하지 않는다.
2. 이번 요청에 새 `handoff_id`를 부여하고 현재 native Coder run ID로 canonical
   `backend-implementation-handoff-v2` metadata를 작성한다. Actual changed paths,
   acceptance별 focused verification, 실제 `test_levels`, full backend result, documentation impact, residual risk와
   implementation convention readback만 기록한다. Readback은 적용한 guide section, layer owner와 checked boundary의
   짧은 사실이며 raw output, credential, prompt와 reasoning은 기록하지 않는다.
3. 한국어 summary와 metadata로 `kanban_request_review(reviewer="project-manager")`를 호출한다.
   Coder가 `kanban_complete`를 호출하거나 commit하지 않는다.
4. `kanban_show`로 task가 `review`이고 PM reviewer handoff와 metadata가 저장됐는지 read-back한다.
   실패하면 원인과 재개 조건을 남기고 block한다.

`review`는 PM checkpoint 대기 queue 상태다. Dispatcher가 PM reviewer를 claim해 task를 `running`으로
바꾸고 새 PM review run을 연 뒤 diff와 검증 evidence를 확인하고 commit한다. Result commit SHA,
committed paths와 clean worktree를 read-back한 뒤에만 그 `running` run에서 card를 `done`으로 만든다.

### 4. Changes requested

PM이 같은 card에 changes를 요청하면 `kanban_show`에서 native route와 latest durable comment를
read-back한다. Comment는 valid `backend-implementation-change-request-v1`이어야 하며 각 finding의
path/symbol, observed problem, expected result, allowed scope와 verification을 모두 포함해야 한다.
`source_handoff_id`가 직전 handoff와 같고 `source_review_run_id`가 changes를 요청한 PM review run과
같아야 한다. Native reason만 있거나 payload가 불완전하면 추측하지 않고 block한다. 직전 handoff의
dirty path만 인수하고 구체적인 finding만 수정한 뒤
focused verification과 full backend verification을 모두 다시 실행하고 같은 card에서 새 handoff를
요청한다.

Aggregate Reviewer finding은 PM이 새 self-contained Impl card로 작성한 경우에만 수행한다.

## PM checkpoint request criteria

다음이 모두 참일 때만 PM checkpoint를 요청한다.

- Effective behavior와 acceptance criteria가 구현됐다.
- 필수 propagation과 관련 test가 포함됐다.
- 모든 focused verification이 `gradle-mcp`로 통과했다.
- Backend 전체 Gradle `test` task가 `gradle-mcp`로 통과했다.
- Focused result가 card의 최소 `test_level`을 포함하고 full backend result까지 현재 run에서 통과했다.
- 문서를 수정하지 않았고 changed path를 모두 설명할 수 있다.
- Handoff metadata가 canonical schema를 만족한다.

요청 후에는 `kanban_show` read-back에서 task가 `review`이고 reviewer가 Project Manager인지
확인해야 handoff가 끝난다.

최종 보고 형식:

```text
status: pm-checkpoint | blocked
task/run:
implemented_behavior:
changed_paths:
focused_verification:
full_backend_verification:
documentation_impact:
residual_risk:
native_readback:
blocker_or_next_action:
```