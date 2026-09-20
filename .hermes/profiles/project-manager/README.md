# Amaazon Project Manager

Project Manager(PM)는 Issue가 **승인된 요구사항에서 검증된 구현과 merge까지** 안전하게
이동하도록 조율하는 역할입니다. PM은 코드를 직접 구현하거나 독립 Reviewer의 판단을 대신하지
않습니다. 대신 무엇을 구현할지, 어떤 근거가 필요한지, 다음 작업이 무엇인지 명확한 계약으로
만듭니다.

## PM이 하는 일

- Issue, 사용자 승인 requirement, 현재 구현 상태를 바탕으로 delivery 범위를 정리합니다.
- 정책이나 요구사항이 부족·모순된 경우 선택지와 영향을 사용자에게 제시합니다.
- Coder가 추가 문서 탐색이나 재기획 없이 구현할 수 있는 task card를 작성합니다.
- Kanban의 task, evidence, checkpoint, review, PR·CI·merge 흐름을 연결합니다.
- 외부 상태와 검증 결과를 read-back한 사실만 완료로 보고합니다.

PM은 business policy, API 의미, authorization, consistency, error semantics를 독자적으로
결정하지 않습니다. 이 결정이 필요하면 작업을 멈추고 사용자 결정을 요청합니다.

## High-level workflow

상황별 Skill 선택 기준은 [`skills/index.md`](skills/index.md)를 참고합니다. 이 index는 탐색만
담당하며 실행 절차와 계약은 각 Skill이 소유합니다.

```text
Issue 선택
  → requirement + fresh current-state 확인
  → create-triage
  → 정책/문서 문제 해결 및 read-back
  → build-task-graph
  → Coder 구현과 PM checkpoint
  → root review
  → current-state 갱신, PR·CI·merge, Issue 종료
```

### 1. Issue와 계획 기준 확인

PM은 구현할 leaf Issue, 사용자 승인 requirement, fresh `current-state.md`, clean working tree를
확인합니다. requirement는 목표 동작의 기준이고, current-state는 검증된 현재 구현의 요약입니다.

### 2. `create-triage`: 구현 계획 정리

PM은 Issue의 구현 아이디어, 후보 작업 경계, 문서 영향, 정책 질문을 triage card에 정리합니다.
정책 또는 요구사항의 빈틈은 사용자 결정과 문서 동기화가 끝날 때까지 `blocked`로 유지합니다.
문제가 해소되면 triage를 고정하고 다음 단계로 넘깁니다.

자세한 설명: [`skills/create-triage/README.md`](skills/create-triage/README.md)

### 3. `build-task-graph`: 실행 계약 작성

PM은 완료된 triage를 Coder별 self-contained card, aggregate Review, Decision, Summary로
바꿉니다. initial Impl은 Review1/Summary의 parent이고, Review finding에서 생긴 work는 append-only로
다음 Review와 Summary에 연결합니다. Summary는 native direct parent read-back으로 membership을 확인하는
immutable finalization card입니다. 모든 신규 card를 dispatch 불가능한 상태로 수동 생성·read-back한
뒤, PM이 첫 Impl 또는 no-Impl aggregate Review 하나만 activation합니다.

`new`는 G1 graph를 만들고, `requirement-rework`은 완료 contract를 무효화하는 승인 requirement
revision에만 G{N+1}을 만듭니다. `review-rework`은 Review completion metadata의 per-finding verdict를
읽어 same-G corrective Impl, context re-review, 또는 blocked Decision을 append합니다. `done` card는
immutable history이고, obsolete unfinished card만 archive합니다.

자세한 설명: [`skills/build-task-graph/README.md`](skills/build-task-graph/README.md)

### 4. 구현·검토·종료

Coder는 자신에게 배정된 card 계약으로 구현·검증하고, PM은 evidence와 commit boundary를 확인해
다음 작업을 진행시킵니다. 모든 child가 끝나면 Reviewer가 root-review contract를 기준으로
aggregate review를 수행합니다. 승인 뒤 PM은 최종 구현 상태를 기록하고 PR, CI, merge, Issue
종료를 read-back합니다.

## 책임의 경계

| 역할 | 주된 책임 |
|---|---|
| Project Manager | 계획, task graph, Kanban routing, evidence 확인, checkpoint, release lifecycle |
| Coder | self-contained task card의 구현과 검증 |
| Reviewer | 독립적인 code/spec/architecture 검토 |
| 사용자 | business policy와 요구사항의 최종 결정 |

## 현재 지원 상태

| 기능 | 상태 |
|---|---|
| Issue triage와 정책/문서 blocker routing | 지원 |
| 새 delivery와 수동 native graph authoring procedure | 지원 |
| requirement-rework graph schema·diff helper·validator | 지원 |
| review-rework persisted schema·validator | 지원 |
| backend Impl card helper/validator | 지원 |
| backend Impl handoff·changes-request·checkpoint validator | 지원 |
| graph-level v6 helper/validator | 지원 |
| native Triage gate·single-ready promotion E2E | 검증 |
| native worker 종료·Review finding·Summary·idempotent recovery E2E | 검증 |
| Profile-scoped Codebase Memory connection·tool discovery | 검증 |

## Workflow reference

- [PM workflow design](WORKFLOW-DESIGN.md): 역할 경계, handoff, lifecycle, 다른 Profile 설계에 재사용할 기준
- [PM terminology](TERMINOLOGY.md): workflow에서 쓰는 개념과 문서 책임

## 설치와 구성

```text
hermes profile install ./.hermes/profiles/project-manager --name project-manager --alias
```

`capabilities.yaml`은 installer가 자동 적용하지 않습니다. 프로젝트 bootstrap이 Profile 설치 뒤
capability policy를 적용합니다. Distribution에는 credential, endpoint, memory, session,
`state.db`, machine-specific path를 포함하지 않습니다.
