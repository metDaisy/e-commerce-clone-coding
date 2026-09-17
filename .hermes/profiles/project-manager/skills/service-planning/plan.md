# service-planning 구현 계획

## 목적

Project Manager가 새 기능, business policy, 사용자 흐름, UI 방향을 사용자와 논의하기 전에 사실과 선택지를 정리한다. Amazon 및 다른 e-commerce 서비스의 관찰은 판단 근거이며, PM이 product policy·API semantics·UI 요구사항을 독자적으로 확정하는 근거가 아니다.

## 사용할 때

- 사용자가 새 기능이나 서비스 개선을 논의할 때
- business policy를 변경할지 검토할 때
- Amazon 또는 다른 e-commerce 서비스와 기능·사용자 흐름을 비교할 때
- UI 목적, 화면 흐름, 우선순위, UX trade-off를 정할 때
- requirement 문서에 반영하기 전 사용자 결정이 필요할 때

## 입력

- 사용자 질문과 결정하려는 범위
- 관련 `docs/requirement` 문서
- freshness가 확인된 `docs/current-state.md`
- 필요할 때 공개 경쟁 서비스 관찰과 신뢰 가능한 외부 자료

## 절차 초안

1. 요구사항과 current-state에서 이미 확정된 사실·미결정 사항을 구분한다.
2. 비교 대상과 관찰 질문을 좁게 정하고, 기능·사용자 흐름·화면 목적·정책 선택지의 근거를 수집한다.
3. 선택지마다 사용자 가치, 구현·운영 영향, 기존 요구사항과의 차이를 정리한다.
4. PM이 결정할 수 없는 정책·권한·공개 계약·UI 요구사항은 사용자에게 명시적으로 질문한다.
5. 사용자 결정 뒤 requirement 문서에 먼저 반영할 변경을 제안한다.
6. 결정이 requirement에 반영되었음을 확인한 뒤 `build-task-graph`로 넘긴다.

## 출력

- 사실 / 제안 / 사용자 결정 필요 항목이 분리된 기획 보고
- 비교 근거와 선택지
- requirement 수정 제안 또는 사용자 결정 기록
- 후속 `build-task-graph` 또는 `sync-docs` 필요 여부

## 경계

- source·test·migration을 구현하지 않는다.
- 사용자 승인 전 policy를 requirement나 Issue의 확정 사실처럼 기록하지 않는다.
- 시각적 구현·frontend component 설계는 Coder의 작업 계약으로 넘긴다.

## 완료 기준

사용자 결정이 필요한 사항과 그 근거가 명확하고, 승인된 결정이 requirement 갱신 또는 명시적 보류 상태로 연결되어야 한다.

## TBD

- 경쟁 서비스 조사 시 인용·스크린샷·관찰 기록의 최소 형식
- UI 방향을 requirement에 기록하는 표준 schema
- 사용자가 복수 선택지를 유보할 때 Issue/task 생성 제한 규칙
