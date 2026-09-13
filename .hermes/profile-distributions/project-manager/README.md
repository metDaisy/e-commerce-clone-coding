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

PM은 완료된 triage를 Coder별 self-contained card와 전체 root-review contract로 바꿉니다. 각
card에는 범위, 제외 범위, 필요한 행위·상태·오류·API/UI/저장소 계약, acceptance criteria,
검증 방법이 포함됩니다.

현재 v0.1은 새 delivery의 JSON draft를 만들고 검증합니다. native atomic graph-create capability가
아직 없으면 실제 Kanban card를 부분적으로 만들지 않고, 검증된 draft와 정확한 blocker를 남깁니다.

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
| 새 delivery task graph JSON draft 및 deterministic validation | 지원 |
| native atomic task graph 생성 | runtime capability 대기 |
| requirement-rework / review-rework graph | 후속 구현 |

## 설치와 구성

```text
hermes profile install ./.hermes/profile-distributions/project-manager --name project-manager --alias
```

`capabilities.yaml`은 installer가 자동 적용하지 않습니다. 프로젝트 bootstrap이 Profile 설치 뒤
capability policy를 적용합니다. Distribution에는 credential, endpoint, memory, session,
`state.db`, machine-specific path를 포함하지 않습니다.
