# sync-docs 구현 계획

## 목적

사용자 승인 requirement의 변경을 기준으로 `current-state.md`를 제외한 파생 문서와 GitHub delivery tracker를 일관되게 동기화한다. code와 requirement의 최종 의미를 PM이 새로 결정하지 않으며, 불일치는 사용자에게 올린다.

## mode

- `derived-docs`: architecture, ADR, glossary, ERD, index, testing/validator 문서처럼 requirement에서 파생된 repository 문서를 동기화한다.
- `issue-tracker`: GitHub Issue tree의 scope, body, status, dependency를 승인 requirement와 동기화하고 external state를 read-back한다.

두 mode는 독립적으로 실행할 수 있다. 같은 requirement 변경이 둘 다에 영향을 주면
`derived-docs`를 먼저 완료하고 `issue-tracker`를 수행한다. 어느 mode도
`current-state.md`를 수정하지 않는다.

## 사용할 때

- 사용자가 requirement 또는 business policy를 변경했을 때
- service-planning에서 사용자 결정이 requirement에 반영됐을 때
- build-task-graph가 관련 architecture/ADR/glossary/ERD/Issue의 불일치를 발견했을 때
- finalization 전 문서 readiness 확인이 필요할 때
- workflow 도입 시 reference baseline을 처음 세울 때

## 입력

- 사용자 승인 requirement 변경과 locator
- 관련 current-state section 및 필요한 committed evidence
- docs/index, architecture, ADR, domain glossary, domain ERD, testing/validator 문서
- GitHub Issue tree의 scope·status·dependency

## 절차 초안

1. 선택한 mode의 영향 대상(파생 문서 또는 tracker 항목)을 inventory한다.
2. 각 항목을 `no change`, `documentation update`, `tracker update`, `needs-input`으로 분류한다.
3. requirement가 의미를 제공하지 않는 architecture·policy 판단은 사용자에게 escalation한다.
4. 승인된 변경만 선택한 대상에 적용한다. `issue-tracker` mode의 GitHub mutation은 external state를 read-back한다.
5. `current-state.md`는 수정하지 않는다. 그 문서가 필요하면 `update-current-state`에 routing한다.

## 출력

- 변경된 문서·Issue와 각 변경 근거
- 변경하지 않은 문서와 그 이유
- 사용자의 결정이 필요한 불일치
- update-current-state 또는 build-task-graph의 후속 필요 여부

## 경계

- `docs/current-state.md`의 schema·snapshot은 소유하지 않는다.
- requirement 또는 policy를 자율적으로 수정하지 않는다.
- source/test/migration 구현을 하지 않는다.
- GitHub Issue를 code나 requirement보다 상위 source of truth로 취급하지 않는다.

## 완료 기준

관련 파생 문서와 tracker가 승인 requirement에 대해 일관되거나, 남은 불일치가 결정자·영향·다음 행동과 함께 명시적으로 기록되어야 한다.

## TBD

- 파생 문서 impact matrix와 required reading set
- baseline의 정확한 범위와 evidence schema
- GitHub Issue field/body/dependency 동기화 규칙
- 문서 변경 commit 경계와 reviewer/CI 요구
