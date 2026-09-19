---
name: run-impl-card
description: Use when admitting and running a PM-authored backend Impl card through its PM checkpoint.
version: 0.5.0
author: "Amaazon project"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, testing, kanban, checkpoint]
    related_skills: [implement]
requires_toolsets: [kanban]
---

# Run Impl Card

PM이 작성한 self-contained Impl card를 backend code와 test로 구현하고 같은 card의 PM
checkpoint로 넘긴다. 시작 전에 다음 reference를 읽는다.

- [`references/execution-contract.md`](references/execution-contract.md): Coder 입력, 권한,
  검증, blocker와 handoff 경계
- [`references/implementation-card-contract.md`](references/implementation-card-contract.md):
  card body 소비, Coder handoff와 changes-request input schema

Repository의 `AGENTS.md`가 강제 규칙을, `docs/testing-guide.md`가 Java test 작성 규칙을
소유한다. Requirement와 Triage는 Coder의 입력이 아니다.

## Procedure

### 1. Task admission

1. Dispatcher가 제공한 현재 task를 `kanban_show`로 읽는다. Native task ID, assignee,
   `running` status, workspace, parent handoff와 body를 read-back한다.
2. Body가 valid JSON이며 `schema: backend-implementation-card-v1`,
   `card_type: implementation`인지 확인한다. Canonical contract의 required field와 reference가
   모두 존재해야 한다.
3. Card의 goal, effective behavior, scope, exclusions, contracts, acceptance, focused verification와
   full backend verification을 읽는다. Traceability locator는 PM 근거이며 requirement를 다시
   해석하라는 지시가 아니다.
4. Git branch와 status 및 native run history를 읽어 initial run과 changes-requested rework를 구분한다.
   Initial run은 staged, unstaged 또는 untracked path가 하나라도 있으면 덮어쓰기·commit·stash·reset·
   clean하지 않고 `kanban_block(kind="needs_input")`으로 중단한다. Rework run은 latest native
   `request-changes` transition, 직전 `review_requested` handoff와 correlated change-request comment를
   먼저 검증하고, dirty paths가 직전 handoff의 `changed_paths`와 정확히 같을 때만 이어서 작업한다.
   다른 dirty path 또는 ID 불일치는 block한다.
5. Card가 불완전하거나 서로 모순되면 task body를 고치지 않고 정확한 누락·충돌과 필요한 PM
   조치를 block reason에 기록한다.

### 2. Implementation execution

1. `implement` Skill을 load하고 implementation map, test-first seam, 최소 구현, focused
   loop, bounded simplification, full backend verification과 self-review 절차를 순서대로 수행한다.
2. 기존 code와 card의 `implementation_context.current_behavior`가 달라 acceptance나 public contract가
   변하면 제품 의미를 다시 해석하지 않고 근거와 영향으로 block한다.
3. 모든 Gradle 작업은 `gradle-mcp`로만 실행한다. Terminal, shell 또는 IDE의 Gradle 실행,
   test 약화·비활성화, validator 우회와 검증 범위 축소는 금지한다.
4. Requirement, architecture, ADR, glossary, `current-state.md`, Profile/workflow와 사용자 문서를
   수정하지 않는다. 문서 영향은 handoff로만 보고한다.

### 3. PM checkpoint handoff

1. Git status와 diff를 읽고 모든 changed path가 card scope 또는 필수 propagation인지 확인한다.
   문서 path나 설명할 수 없는 path가 있으면 review를 요청하지 않는다.
2. 이번 요청에 새 `handoff_id`를 부여하고 canonical `backend-implementation-handoff-v1` metadata를
   작성한다. Actual changed paths,
   acceptance별 focused verification, full backend result, documentation impact와 residual risk만
   기록한다. Raw output, credential, prompt와 reasoning은 기록하지 않는다.
3. 한국어 summary와 metadata로 `kanban_request_review(reviewer="project-manager")`를 호출한다.
   Coder가 `kanban_complete`를 호출하거나 commit하지 않는다.
4. `kanban_show`로 task가 `review`이고 PM reviewer handoff와 metadata가 저장됐는지 read-back한다.
   실패하면 원인과 재개 조건을 남기고 block한다.

PM은 review 상태에서 diff와 검증 evidence를 확인하고 commit한다. Result commit SHA, committed
paths와 clean worktree를 read-back한 뒤에만 PM이 card를 `done`으로 만든다.

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