---
name: implementation-workflow
description: Use when implementing a PM-authored backend Impl card.
version: 0.2.0
author: "Amaazon project"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, testing, kanban, checkpoint]
    related_skills: [codebase-memory-mcp, semble-search, java-springboot, java-junit]
requires_toolsets: [kanban]
---

# Backend Implementation Workflow

PM이 작성한 self-contained Impl card를 backend code와 test로 구현하고 같은 card의 PM
checkpoint로 넘긴다. 시작 전에 다음 reference를 읽는다.

- [`references/execution-contract.md`](references/execution-contract.md): PM/Coder 권한,
  검증, blocker와 lifecycle 경계
- [`references/implementation-card-contract.md`](references/implementation-card-contract.md):
  card body, Coder handoff와 PM checkpoint schema

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
4. Git branch와 status를 읽는다. 시작 worktree에 staged, unstaged 또는 untracked path가 하나라도
   있으면 덮어쓰기·commit·stash·reset·clean하지 않고 `kanban_block(kind="needs_input")`으로
   중단한다.
5. Card가 불완전하거나 서로 모순되면 task body를 고치지 않고 정확한 누락·충돌과 필요한 PM
   조치를 block reason에 기록한다.

### 2. Source investigation

1. `implementation_context.entry_points`에서 production source와 기존 test를 읽고 정의와 실제
   usage를 추적한다. Entry point는 allowlist가 아니므로 필수 caller, consumer, DTO, mapping,
   adapter와 test propagation을 확인한다.
2. 위치가 불명확하면 `semble-search`로 후보를 찾고 실제 파일에서 확인한다. Cross-domain
   caller/callee 또는 public seam 영향은 `codebase-memory-mcp`로 찾되 source, test와
   `package-info.java`에서 재확인한다. MCP가 unavailable이면 bounded local search로 계속한다.
3. 기존 code가 card의 `implementation_context.current_behavior`와 달라도 card의 제품 의미를
   바꾸지 않는다. 차이로 인해 acceptance 범위나 public contract가 달라지면 사실·근거·영향을
   기록하고 block한다.
4. Card에 확정되지 않은 business, authorization, transaction/consistency, error, API, event 또는
   cross-domain 의미가 필요하면 발명하지 않고 `kanban_block(kind="needs_input")`으로 PM에 돌린다.

### 3. Implement code and tests

1. `effective_behavior`와 `acceptance_criteria`를 만족하는 가장 작은 production change를 구현한다.
2. 구현에 필수적인 caller, DTO, mapping, adapter, persistence와 test를 같은 task에서 전파한다.
3. 각 focused verification의 `test_level`과 `required_scenarios`를 따라 가장 작은 적절한 test를
   작성하거나 수정한다. Success와 요구된 rejection/failure path를 포함한다.
4. Existing convention, public contract와 module boundary를 보존한다. Card 밖 refactor,
   optimization, abstraction 또는 unrelated defect repair를 섞지 않는다.
5. Requirement, architecture, ADR, glossary, `current-state.md`, Profile/workflow와 사용자 문서를
   수정하지 않는다. 문서 영향은 나중에 handoff로만 보고한다.

### 4. Two-phase verification

모든 Gradle 작업은 `gradle-mcp`로 실행한다. Terminal, shell 또는 IDE에서 `gradle`, `gradlew`,
`gradlew.bat`을 실행하지 않는다.

1. **Focused verification**
   - 변경한 behavior에 대응하는 test FQCN을 확인한다.
   - Card의 focused verification별 success와 rejection/failure scenario를 실행한다.
   - Acceptance ID마다 하나 이상의 passing focused verification을 연결한다.
2. **Full backend verification**
   - 모든 focused verification이 통과한 뒤 card의 `full_backend_verification`에 따라 backend 전체
     Gradle `test` task를 실행한다.
   - Frontend, npm과 browser 검증은 실행하지 않는다.
3. 어느 단계든 실패하면 원인을 수정하고 영향받은 focused verification부터 다시 실행한 뒤 full
   backend verification을 반복한다. 전체 test suite가 통과할 때까지 handoff하지 않는다.
4. Test 삭제·비활성화, assertion 약화, validator 우회 또는 검증 범위 축소로 통과시키지 않는다.
5. `gradle-mcp`가 unavailable하거나 외부 prerequisite 때문에 전체 test를 실행할 수 없으면
   우회하지 않고 `kanban_block(kind="capability")`으로 중단한다.

### 5. PM checkpoint handoff

1. Git status와 diff를 읽고 모든 changed path가 card scope 또는 필수 propagation인지 확인한다.
   문서 path나 설명할 수 없는 path가 있으면 review를 요청하지 않는다.
2. Canonical `backend-implementation-handoff-v1` metadata를 작성한다. Actual changed paths,
   acceptance별 focused verification, full backend result, documentation impact와 residual risk만
   기록한다. Raw output, credential, prompt와 reasoning은 기록하지 않는다.
3. 한국어 summary와 metadata로 `kanban_request_review(reviewer="project-manager")`를 호출한다.
   Coder가 `kanban_complete`를 호출하거나 commit하지 않는다.
4. `kanban_show`로 task가 `review`이고 PM reviewer handoff와 metadata가 저장됐는지 read-back한다.
   실패하면 원인과 재개 조건을 남기고 block한다.

PM은 review 상태에서 diff와 검증 evidence를 확인하고 commit한다. Result commit SHA, committed
paths와 clean worktree를 read-back한 뒤에만 PM이 card를 `done`으로 만든다.

### 6. Changes requested

PM이 같은 card에 changes를 요청하면 path/symbol, observed problem, expected result, allowed scope와
verification을 읽는다. 정보가 부족하면 추측하지 않고 clarification을 요청한다. 구체적인 finding만
수정한 뒤 focused verification과 full backend verification을 모두 다시 실행하고 새 handoff를
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