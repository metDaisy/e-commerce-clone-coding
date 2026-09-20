# create-triage 구현 계획

## 목적

모든 새 leaf Issue에 대해 Coder graph를 만들기 전에 PM-owned triage card를 생성한다.
Triage card는 구현 아이디어·후보 task 경계·문서 영향·정책 문제·build-task-graph
handoff를 기록한다. 실행 가능한 Coder contract는 작성하지 않는다.

## 상태와 소유

Kanban plugin의 native 상태를 사용한다.

```text
triage → running planning → frozen running graph gate → done
          └──────────────→ blocked → running planning
```

- 초기 상태는 `triage`다.
- `todo`와 `ready`는 native promotion이 필요한 runtime에서만 사용한다.
- 정책·요구사항 문제가 있으면 `blocked`로 전이한다.
- `needs-input`은 native 상태가 아니라 body의 `blocker.kind`와 decision request로 표현한다.
- active leaf Issue당 active triage card는 하나만 허용한다.
- 완료된 body는 freeze한다. Triage는 Coder 입력은 아니지만 graph authoring 중 first eligible Impl을
  `todo`로 유지하는 native scheduling parent다.

## Admission

Kanban mutation 전에 다음을 확인한다.

- 선택된 leaf Issue와 ancestor lineage
- clean repository와 planning HEAD
- approved requirement
- `current-state.md` freshness가 `fresh`인지 여부
- native Kanban plugin의 task create/read-back/status transition surface

`current-state.md`가 `stale` 또는 `insufficient`이면 card를 실행 가능한 triage로
진행하지 않고 `update-current-state`로 routing한다. Triage는 `current-state.md`를
수정하지 않는다.

## Body schema

triage body의 정본은 JSON이다. `scripts/triage.py template`은 Issue 번호·title·URL,
planning/current-state SHA와 PM이 이미 읽은 locator만 받아 `.temp/triage/issue-<n>/`
아래 draft를 만든다. 요구사항을 스스로 추측하거나 GitHub·Kanban을 변경하지 않는다.
`scripts/triage.py validate <body.json> --status <status>`가 형식·상태 일관성을
검사한다. 사람에게 정책 판단을 보여야 할 때는 linked `service-planning` card의
Markdown body를 사용한다.

필수 영역:

- Issue identity와 planning baseline
- fresh current-state read-only snapshot
- goal, scope, out-of-scope
- current behavior와 desired behavior
- implementation idea
- candidate vertical slices와 dependency
- verification direction
- requirement, architecture, ADR, glossary, ERD, index의 document impact
- policy findings와 user decision request
- build-task-graph handoff

문서 영향 판정은 `update | no-change | not-applicable | blocked`다.
`no-change`는 검토했지만 수정하지 않는다는 뜻이며, `current-state`는 별도
`read-only` 항목이다.

## 절차

1. freshness·clean tree·Issue·requirement를 read-back한다.
2. 동일 Issue의 active triage card를 찾고 있으면 재사용한다.
3. `.temp`의 Issue-specific draft body를 작성하고 deterministic validator를 통과시킨다.
4. 없으면 validated JSON body를 포함해 `Issue #<number> triage`를 native `triage` 상태로 생성한다.
   생성 뒤 native `kanban_show`로 read-back하고, 필요하면 read-only
   `triage.py validate-card --task-id <actual-id> --board <board>`로 재검증한다.
5. PM이 native claim 후 `running`에서 문서와 requirement를 대조한다.
6. 문제가 없으면 `build_task_graph.allowed: true`로 기록하고 body를 freeze하되 `running` gate로 유지한다.
7. 정책·요구사항 결정이 필요하면 `blocked`로 전환하고 linked service-planning card를 만든다.
8. 사용자 결정, requirement 수정, `sync-docs` 결과, Issue read-back 후 같은 card를 재개한다.
9. triage body를 freeze하고 `build-task-graph`에 provenance이자 first Impl의 scheduling parent로 넘긴다.
10. `build-task-graph`가 전체 graph를 검증한 뒤 Triage를 완료하고 exactly-one-ready를 read-back한다.

## service-planning handoff

PM은 business policy를 간단히 결정하지 않는다. 다음을 별도 card로 만든다.

```text
Issue #<number> service-planning
```

service-planning card는 triage card와 linked relationship을 가지며, 완료 후 기존
triage card를 `running`으로 재개한다. 정책 승인 전에는 requirement·derived docs·Issue를
확정 상태로 수정하지 않는다.

## 완료 기준

### 문제 없음

- 모든 관련 문서가 disposition을 가짐
- unresolved policy finding 없음
- current-state freshness read-back 완료
- `build_task_graph.allowed: true`
- frozen triage body와 native `running` gate read-back
- graph 검증 뒤 `build-task-graph`가 수행한 native `done`과 first Impl `ready` read-back

### 문제 있음

- triage가 `blocked`
- blocker와 decision request가 body에 있음
- service-planning card가 linked/read-back됨
- 사용자 결정 전 graph 생성 금지

### 문제 해결 후

- 사용자 결정 read-back
- requirement·affected docs·Issue sync read-back
- body 재검증
- triage freeze와 `running` graph gate read-back
- build-task-graph handoff 가능

## 검증 도구

`scripts/triage.py`는 JSON draft 생성과 필수 field·document impact·current-state
read-only·status gate 규칙을 deterministic하게 검사한다. `validate-card`만 공식 CLI로
Kanban을 read-only 조회한다. Kanban mutation은 PM의 native `kanban_*` tool만 수행한다.

## 구현 산출물

- `SKILL.md`
- `references/triage-contract.md`
- `scripts/triage.py`
- `scripts/test_triage.py`
- `WORKFLOW-DESIGN.md`, `build-task-graph/SKILL.md`와 board contract의 handoff 정렬
