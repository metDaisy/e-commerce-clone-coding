# build-task-graph

`build-task-graph`는 완료된 triage와 승인된 requirement를 바탕으로, Coder가 다른 문서를
다시 찾아 해석하지 않아도 구현할 수 있는 **self-contained task graph 초안**을 만드는
Project Manager Skill입니다.

각 Coder card는 하나의 독립적으로 검증 가능한 결과를 설명하고, root-review card는
전체 delivery를 검토할 공통 계약을 설명합니다.

## 언제 사용하나요?

다음 조건이 충족된 뒤 사용합니다.

- 선택된 leaf Issue가 있음
- `create-triage`가 완료되고 graph authoring을 허용함
- requirement가 사용자 승인 상태임
- `current-state.md`가 fresh 상태임
- planning baseline과 working tree admission이 확인됨

## 동작 과정

```text
완료된 triage
  + 승인된 requirement
  + fresh current-state
  + leaf Issue
       ↓
PM이 Coder child와 root-review 계약 작성
       ↓
JSON draft 검증
       ↓
atomic native graph-create capability 확인
       ↓
그래프 생성 또는 blocked 보고
```

1. **입력 확인** — Issue lineage, triage handoff, requirement, current-state, planning 기준을
   다시 확인합니다.
2. **작업 분해** — triage의 후보 작업을 그대로 복사하지 않고, 승인된 문서 근거로 가장 작은
   완전한 Coder 작업 단위로 정리합니다.
3. **Coder 계약 작성** — 각 child에 목표, 범위, 제외 범위, actor/authorization, API·UI·저장소
   영향, 오류·상태 의미, acceptance criteria와 검증 방법을 명시합니다.
4. **검토 계약 작성** — root-review에 모든 child의 공통 결과, 경계 불변조건, 제외 범위와
   검토 질문을 기록합니다.
5. **초안 검증** — JSON validator가 evidence, acceptance coverage, dependency cycle, assignee,
   root 구성, 처음 실행 가능한 child가 하나인지 등을 확인합니다.

## v0.1의 범위

현재는 `new-delivery`만 지원합니다.

- 지원: 새 leaf Issue의 최초 delivery graph 초안
- 보류: requirement 변경 rework, Reviewer finding rework
- 보류: native Kanban graph 생성

특히 현재 runtime에 **atomic graph-create capability**가 없으면 이 Skill은 검증된 JSON draft를
남기고 `blocked`를 보고합니다. card를 하나씩 만들어 임시로 연결하지 않습니다. 부분 graph가
Dispatcher에 노출되어 잘못 claim되는 것을 막기 위한 경계입니다.

## 결과

검증된 draft는 다음을 포함합니다.

- `implementation-coder`에게 배정될 self-contained child card들
- `reviewer-general`에게 배정될 root-review card
- child 간 dependency와 하나의 초기 eligible child
- requirement 기반 `goal` evidence와 current-state 기반 `state` evidence
- 행동 검증에 연결된 acceptance criteria

실제 native task ID와 ready 상태는 future atomic creator가 graph 전체를 한 번에 저장한 뒤에만
생깁니다.

## 다음 단계와 관련 Skill

```text
create-triage
  → build-task-graph
  → controll-task-graph
```

- `create-triage`: Issue 계획과 정책/문서 문제를 정리합니다.
- `build-task-graph`: 실행 가능한 Coder/Reviewer contract 초안을 만듭니다.
- `controll-task-graph`: 구현 checkpoint, review routing, PR·merge lifecycle을 제어합니다.

정확한 JSON field, validator 규칙과 실행 절차는 `SKILL.md`와
[`references/board-contract.md`](references/board-contract.md)를 따릅니다.
