# Coder execution contract

이 문서는 Project Manager와 `implementation-coder` 사이의 backend 구현 계약을 소유한다.
PM은 이 계약을 만족하는 Impl card를 작성하고, Coder는 card를 실행하며, PM은 같은 card의
checkpoint를 검수한다. 세부 field shape는
[`implementation-card-contract.md`](implementation-card-contract.md)가 소유하고, 실행 순서는
[`../SKILL.md`](../SKILL.md)가 소유한다.

## 입력과 기준

Coder의 유일한 제품 동작 입력은 자신에게 배정된 self-contained Impl card다. Requirement,
Triage, 이전 대화와 다른 Profile의 Memory는 PM의 traceability 근거이며 Coder가 다시 해석할
입력이 아니다.

질문별 기준은 다음과 같다.

1. 구현할 동작과 완료 조건: Impl card의 `effective_behavior`, `contracts`,
   `acceptance_criteria`.
2. 현재 구현 사실: checkout된 production source, test, configuration, migration.
3. 구조와 작업 제약: `AGENTS.md`, architecture, ADR, package boundary, repository validator.
4. 실행 상태: native Kanban task/run/comment와 실제 Git 상태.

Card와 repository 제약이 양립하지 않거나 card의 현재 상태 설명이 실제 source와 달라
acceptance 범위가 바뀌면 Coder가 의미를 보정하지 않고 PM에 사실과 영향을 보고한다.

## 권한

Coder는 card의 동작을 구현하기 위한 내부 구현 방식, 테스트 위치와 수준, 기존 의미를
보존하는 국소 수정을 결정한다. 컴파일·동작·테스트에 필수적인 caller, consumer, DTO,
mapping, adapter와 test 전파는 scope 확장이 아니라 구현의 일부다.

다음은 Coder가 결정하지 않는다.

- 새로운 business behavior, authorization, transaction/consistency 또는 error semantics
- card에 없는 public API, event 또는 cross-domain contract
- acceptance criteria를 바꾸는 대안 설계
- unrelated refactor, optimization, abstraction 또는 defect repair
- requirement, architecture, ADR, glossary, `current-state.md`, Profile/workflow 문서의 수정
- commit, push, merge와 Impl card의 최종 `done` 전환

문서 영향은 수정하지 않고 handoff의 `documentation_impact`로 보고한다. Database migration,
configuration처럼 실행에 필요한 non-document artifact는 card scope에 포함된 경우 구현할 수 있다.

## 작업 및 검증

Coder는 production code와 해당 테스트를 함께 구현한다. 검증은 반드시 다음 순서다.

1. **Focused verification:** 변경 동작의 success와 필요한 rejection/failure path를 가장 작은
   적절한 unit, slice, repository, integration 또는 Modulith test로 실행한다.
2. **Full backend verification:** focused verification이 통과한 뒤 backend 전체 Gradle test
   suite를 실행한다.

모든 Gradle 호출은 `gradle-mcp`를 사용한다. 실패하면 원인을 수정하고 영향받은 focused
verification부터 다시 실행한 뒤 full backend verification을 반복한다. 테스트 삭제·비활성화,
assertion 약화, validator 우회로 성공을 만들지 않는다. 전체 테스트가 통과하기 전에는 PM
checkpoint를 요청하지 않는다.

## Blocker

예상하지 못한 dirty path, 안전하게 분리할 수 없는 기존 변경, 미정 policy, contract 충돌,
외부 prerequisite 부재, `gradle-mcp` 사용 불가 또는 허용 범위 밖 결정이 필요하면 native
`blocked`로 중단한다. 보고에는 확인한 사실, 근거, 영향과 재개에 필요한 결정 또는 조건을
포함한다.

단순한 구현 위치 부족은 blocker가 아니다. Coder가 source와 test를 조사해 해결한다.

## Handoff와 완료

Focused verification과 full backend verification이 모두 통과하면 Coder는
`kanban_request_review`로 같은 card의 PM checkpoint를 요청한다. 이 호출은 native lifecycle을
`running → review`로 전환한다. Handoff는 구현 동작, 변경 경로, acceptance별 결과, 실제 검증,
문서 영향과 residual risk를 보존한다.

PM은 review 상태에서 handoff, diff와 commit boundary를 확인하고 변경을 commit한다. PM이 result
commit SHA, committed paths와 clean working tree를 read-back한 뒤에만 card를 `done`으로
전환한다. 따라서 card lifecycle은 다음과 같다.

```text
ready → running → review → done
                   └────→ changes requested → ready/running
running/review → blocked → resumed source phase
```

Coder handoff는 구현 완료 주장이나 Reviewer의 aggregate verdict가 아니다. `done`은 PM checkpoint와
clean committed boundary가 확인되었다는 뜻이며, 독립 aggregate Review는 그 뒤 별도 card에서
수행한다.
