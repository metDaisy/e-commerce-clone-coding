# create-triage

`create-triage`는 새 leaf Issue를 바로 구현 task로 나누기 전에, **무엇을 만들고 무엇이 아직 결정되지 않았는지** 정리하는 Project Manager 절차입니다.

이 Skill의 산출물은 Coder에게 전달하는 구현 지시가 아닙니다. PM이 다음 단계에서
`build-task-graph`로 self-contained 구현 card를 작성할 수 있게 하는 **Issue별 계획 기록**입니다.

## 언제 사용하나요?

새 leaf Issue를 시작할 때 사용합니다.

시작 전에 PM은 Issue 범위, 승인된 requirement, fresh `current-state.md`, clean working tree를
확인합니다. 같은 Issue에 진행 중인 triage card가 있으면 새로 만들지 않고 그 card를 이어서
사용합니다.

## 동작 과정

```text
Issue와 요구사항 확인
  → 현재 구현 상태와 문서 영향 확인
  → triage card에 구현 아이디어·후보 작업 경계·알려진 의존성 기록
  → 정책 또는 요구사항의 모순·누락 판단
  → 완료 또는 사용자 결정 대기
```

1. **계획 작성** — 구현 목표, 범위, 제외 범위, 현재/목표 동작과 후보 작업 단위를 정리합니다.
2. **문서 영향 확인** — requirement, architecture, ADR, glossary 등에서 갱신이 필요한지 기록합니다.
3. **정책 문제 분리** — 구현에 영향을 주는 정책 모순이나 누락이 있으면 PM이 임의로 결정하지
   않고 `service-planning`과 사용자 결정을 요청합니다.
4. **동기화와 재개** — 사용자의 결정 뒤 requirement, 파생 문서, Issue가 동기화·read-back되면
   같은 triage를 재개합니다.
5. **완료와 handoff** — 미결 사항이 없으면 triage body를 고정하고
   `build_task_graph.allowed: true`로 넘깁니다.

## 결과

완료된 triage에는 다음 정보가 남습니다.

- Issue 목표와 후보 구현 방향
- 문서 영향과 확인 결과
- 정책 결정 요청 또는 해결 근거
- `build-task-graph`로 넘길 수 있는지 여부

Triage card는 Coder가 직접 읽는 계약은 아니지만, graph authoring 중 first eligible Impl을
`todo`로 유지하는 native scheduling parent입니다. PM은 이 기록과 승인된 문서를 바탕으로 Coder별
task card를 모두 만든 뒤 Triage를 완료해 첫 Impl 하나만 `ready`로 승격합니다.

## 막히는 경우

다음 상태에서는 triage를 완료하지 않습니다.

- `current-state.md`가 stale 또는 insufficient인 경우
- 요구사항 또는 정책이 구현에 필요한 결정을 제공하지 않는 경우
- requirement·문서·Issue 사이에 해결되지 않은 충돌이 있는 경우

이때 triage는 `blocked` 상태로 남고, 필요한 사용자 결정이나 문서 동기화 뒤에만 재개됩니다.

## 다음 단계

```text
create-triage 완료
  → build-task-graph
  → self-contained Coder child cards + root review contract
```

세부 JSON 구조와 Kanban 상태 전이는 `SKILL.md`와
[`references/triage-contract.md`](references/triage-contract.md)를 따릅니다.
