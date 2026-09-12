# build-task-graph 구현 계획

## 목적

사용자 승인 requirement, GitHub Issue, freshness가 확인된 current-state를 Coder가 requirement 문서를 다시 기획하지 않고 구현할 수 있는 self-contained Kanban task graph로 바꾼다. 기존 `write-task`의 코드 전수 조사 중심 절차를 문서 우선 절차로 대체한다.

## 사용할 때

- open leaf Issue의 최초 구현 graph를 만들 때
- 사용자가 requirement를 먼저 수정한 뒤 Coder rework를 만들 때
- Reviewer finding을 구체적인 Coder rework contract로 바꿀 때
- Kanban graph가 유실된 뒤 `controll-task-graph`가 정상 graph 재생성을 허용할 때

## mode

- `new-delivery`: 새 leaf Issue의 최초 child/root graph 작성
- `requirement-rework`: requirement 변경을 기존 Issue 또는 새 delivery scope의 rework graph로 변환
- `review-rework`: structured Reviewer finding을 rework graph로 변환

## 입력

- GitHub root-to-leaf Issue lineage와 해당 Issue 본문
- 사용자 승인 requirement locator
- freshness가 확인된 current-state의 관련 section
- 기존 Kanban card, dependency, review finding이 있으면 그 read-back
- clean repository와 선택된 planning/delivery branch 정보

## 절차 초안

1. Issue가 delivery tracking인지 확인하고 requirement를 기능 목표의 기준으로 읽는다.
2. current-state에서 관련 모듈·API·DB·테스트 상태를 파악한다. 문서가 충분하면 source 전수 조사를 하지 않는다.
3. requirement, current-state, Issue가 충돌하거나 부족하면 `needs-input` 또는 제한적 조사로 routing한다.
4. 구현 범위를 독립 검증 가능한 child로 나누고 dependency로 한 시점에 하나의 Coder child만 eligible하게 만든다.
5. 각 Coder card에 Goal, scope, out-of-scope, actor, observable behavior, error/state semantics, acceptance, verification, current-state/requirement locator를 넣는다.
6. 모든 child와 Reviewer-assigned `root-review-{n}` aggregate contract를 초안으로 만든다.
7. board contract/validator로 draft와 생성 후 graph를 검증하고 native read-back한다.

## 출력

- PM-authored Coder child cards
- immutable root-review contract와 PM-authored review questions
- dependency graph 및 첫 ready child
- source 문서 locator, unknown, blocked/needs-input 기록

## 경계

- requirement의 policy 의미를 새로 결정하지 않는다.
- Coder에게 requirement 문서 재해석을 지시하지 않는다.
- Reviewer가 수행할 독립 review를 대신하지 않는다.
- source 조사 필요성을 문서 불충분·충돌·불일치 보고의 예외로 제한한다.

## 완료 기준

모든 실행 card가 한 명의 owner, 명시적 scope/out-of-scope, acceptance, verification, 의존성을 가지며, validator와 native read-back이 이를 증명해야 한다.

## TBD

- 새 board-contract v4의 task body schema
- requirement-rework의 lineage 및 prior task 참조 형식
- root-review finding schema와 rework draft generator
- 문서 우선 planning에서 제한적 source investigation을 허용하는 정확한 조건
