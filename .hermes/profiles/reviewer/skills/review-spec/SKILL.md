---
name: review-spec
description: "Review changed behavior against its frozen contract."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, requirements, acceptance]
    related_skills: [codebase-memory-mcp, semble-search]
---

# Spec Review

Frozen Review card의 behavior와 aggregate acceptance를 committed behavior와 대조한다. `/code-review`의
Spec 분리 원칙을 적용해 coding preference나 architecture 품질로 요구사항 판정을 대신하지 않는다.

## 입력과 범위

`run-review/references/axis-result-contract.md`의 공통 입력을 사용한다. Requirement 원문은 provenance와
모호성 확인에만 사용하고, PM이 materialize한 closed card를 임의로 확대·축소하지 않는다.

## Code discovery 선택

Behavior 구현 위치를 모르면
[`../run-review/references/code-discovery-guide.md`](../run-review/references/code-discovery-guide.md)의
**Semble branch**로 의도 기반 후보를 찾는다. Entry point나 symbol을 알지만 caller→effect 또는
producer→consumer 경로가 불명확하면 **Codebase Memory branch**를 사용한다. Exact endpoint, error text,
test ID 또는 configuration key는 local literal search를 사용한다. 발견한 behavior 경로는 source와 test에서
확정하며 같은 질문을 두 discovery tool에 반복하지 않는다.

## 절차

1. Behavior ID별로 success, rejection, boundary, state transition, authorization/ownership, API/error contract,
   required scenario와 explicit scope exclusion을 coverage map에 배치한다.
2. Entry point에서 caller, application/domain operation, persistence/external effect와 response/error mapping까지
   실행 경로를 추적한다. 설명이나 method 이름이 아니라 실제 branch와 side effect를 확인한다.
3. 각 contract item을 관련 test assertion과 연결한다. Happy path만 있고 rejection·boundary·transition이
   빠졌거나 assertion이 관찰 결과를 증명하지 않으면 coverage gap으로 기록한다. Test가 mock interaction,
   method call count 또는 implementation structure만 확인하고 observable behavior를 증명하지 않는지도 본다.
4. `missing`, `partial`, `incorrect`, `scope-creep`을 구분한다. Card가 정하지 않은 business policy,
   authorization, consistency 또는 error semantics가 필요하면 preferred behavior를 발명하지 않고
   `context-required` 또는 `decision-required` 후보로 돌린다.
5. Prior Spec finding은 원래 scenario가 current checkpoint에서 실제로 통과하고 contract가 유지될 때만
   resolved 후보로 만든다.

## 재현과 test evidence

- Finding마다 가장 작은 red-capable scenario를 찾는다. 현재 defect가 있으면 실패하고 해소되면 통과할
  수 있어야 하며, 단순히 application이 시작된다는 검사는 충분하지 않다.
- Error message와 stack trace를 끝까지 읽고 entry point에서 잘못된 값·상태가 생성된 지점까지 data flow를
  역추적한다. Symptom 위치만 보고 correction을 단정하지 않는다.
- Similar working path가 있으면 같은 input, configuration과 dependency 조건으로 비교한다. 원인을 확정할
  수 없으면 falsifiable question과 필요한 evidence를 남기고 추측을 finding으로 승격하지 않는다.
- Existing test가 있다면 assertion의 실제 관찰값, setup isolation, 실행 순서 독립성과 error/boundary input을
  확인한다. Test 이름, TDD 이력 또는 coverage percentage만으로 behavior 충족을 판정하지 않는다.

## Finding 기준

Finding에는 card의 exact basis, 실행 가능한 observed fact, source/test evidence와 사용자·데이터 영향이
필요하다. Test 이름만 존재하거나 코드 모양이 기대와 다르다는 사실만으로 defect를 만들지 않는다.

- `correction-required`: frozen contract와 현재 동작이 재현 가능하게 다르다.
- `context-required`: 승인된 contract가 서로 충돌하거나 필요한 의미가 누락됐다.
- `decision-required`: 해결하려면 새로운 제품 정책 또는 public behavior 결정이 필요하다.

## Not applicable과 검증

Aggregate Review에서는 effective behavior가 있으므로 일반적으로 `reviewed`다. 오직 card가 다른 축의
corrective evidence만 검토하도록 명시하고 Spec behavior가 전혀 변하지 않았음을 commit/call-path로
입증한 경우에만 `not-applicable`을 사용한다. 가장 가까운 behavior test를 root에 요청하고, 실행되지
않은 test는 evidence로 통과 처리하지 않는다.

## 완료 기준

모든 behavior와 aggregate acceptance가 source와 test에 매핑되고, 누락·오류·scope creep·모호성이 각각
판정되며 axis result에 evidence 또는 no-finding 근거가 있다. Discovery로 얻은 모든 strong claim은
checked-out source로 확인됐다.