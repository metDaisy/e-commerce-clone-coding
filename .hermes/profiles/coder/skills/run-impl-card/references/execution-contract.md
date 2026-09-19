# Coder execution contract

이 문서는 `coder`가 admitted backend Impl card를 실행하는 권한과 경계를 소유한다.
Card body를 소비하는 규칙과 Coder handoff shape는
[`implementation-card-contract.md`](implementation-card-contract.md)가 소유하고, 구체적인 실행 순서는
[`../SKILL.md`](../SKILL.md)가 소유한다.

PM의 card 작성, checkpoint review, commit, changes-request 작성과 task completion 절차는 PM
`build-task-graph`와 `controll-task-graph` 계약이 소유한다.

## Coder 입력

Coder가 구현할 제품 동작의 유일한 입력은 자신에게 배정되고 admission을 통과한 self-contained Impl
card다. Requirement, Triage, 이전 대화와 다른 Profile의 Memory는 PM의 traceability 근거이며 Coder가
다시 해석할 입력이 아니다.

Coder는 다음 기준을 사용한다.

1. 구현할 동작과 완료 조건: card의 `effective_behavior`, `contracts`, `acceptance_criteria`.
2. 현재 구현 사실: checkout된 production source, test, configuration, migration.
3. 구조와 작업 제약: `AGENTS.md`, architecture, ADR, package boundary, repository validator.
4. 실행 상태: native Kanban task/run/comment와 실제 Git 상태.

Card와 repository 제약이 양립하지 않거나 `implementation_context.current_behavior`가 실제 source와
달라 acceptance 또는 public contract가 변하면 의미를 보정하지 않고 block한다.

## Coder 권한

Coder는 card 동작을 구현하기 위한 내부 구현 방식, 테스트 위치와 수준, 기존 의미를 보존하는 국소
수정을 결정한다. 컴파일·동작·테스트에 필수적인 caller, consumer, DTO, mapping, adapter,
persistence와 test 전파는 구현의 일부다.

Coder는 다음을 결정하거나 수행하지 않는다.

- 새로운 business behavior, authorization, transaction/consistency 또는 error semantics
- card에 없는 public API, event 또는 cross-domain contract
- acceptance criteria를 바꾸는 대안 설계
- unrelated refactor, optimization, abstraction 또는 defect repair
- requirement, architecture, ADR, glossary, `current-state.md`, Profile/workflow 문서 수정
- commit, push, merge 또는 Impl card의 최종 `done` 전환

문서 영향은 수정하지 않고 handoff의 `documentation_impact`로 보고한다. Migration과 configuration처럼
실행에 필요한 non-document artifact는 card scope에 포함된 경우 구현할 수 있다.

## 실행 모드

- **Initial run:** card의 모든 behavior와 acceptance를 구현한다. 시작 시 dirty path가 하나라도 있으면
  보존한 채 block한다.
- **Changes-requested rework:** latest native transition과 직전 handoff에 결합된 valid change-request를
  확인한다. 직전 handoff의 dirty path만 인수하고 각 finding의 `path`, `symbol`, `allowed_scope` 안에서만
  수정한다. 원래 card의 모든 focused 및 full backend verification을 다시 실행한다.

## 검증

Production code와 관련 테스트를 함께 구현하고 다음 순서를 지킨다.

1. **Focused verification:** card의 success, rejection/failure, boundary와 persistence scenario를 가장
   작은 적절한 test level로 실행한다.
2. **Full backend verification:** focused verification이 모두 통과한 뒤 backend 전체 Gradle `test`를
   실행한다.

모든 Gradle 호출은 `gradle-mcp`만 사용한다. 실패 원인을 수정한 뒤 영향받은 focused verification부터
다시 실행하고 full backend verification을 반복한다. 테스트 삭제·비활성화, assertion 약화, validator
우회 또는 검증 축소로 성공을 만들지 않는다.

## Blocker

예상하지 못한 dirty path, 분리할 수 없는 기존 변경, 미정 policy, contract 충돌, 외부 prerequisite
부재, `gradle-mcp` 사용 불가 또는 허용 범위 밖 결정이 필요하면 native `blocked`로 중단한다. 확인한
사실, 근거, 영향과 재개에 필요한 owner·조건을 기록한다. 구현 위치가 불명확한 것만으로는 blocker가
아니며 source와 test를 조사한다.

## Handoff 경계

Focused verification과 full backend verification이 모두 통과하면 Coder는 canonical
`backend-implementation-handoff-v1`을 작성하고 `kanban_request_review(reviewer="project-manager")`로 같은
card의 checkpoint를 요청한다. Coder는 다음 사실을 read-back한다.

- task status가 `review`다.
- reviewer가 `project-manager`다.
- 이번 handoff metadata가 저장됐다.

이 시점은 Coder 실행의 끝이지 card의 `done`이나 commit 완료가 아니다. PM의 후속 checkpoint 절차를
Coder가 대신 수행하지 않는다.
