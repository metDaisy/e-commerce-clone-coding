# ADR-0012: 관리자 심사 통합 조회 모델과 도메인 요청 원본 분리

- Status: Superseded
- Date: 2026-08-16
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: ADR-0013

## Context

판매자 신청, CatalogProduct·ProductVariant 등록 요청, 관리자 비활성 Offer 활성화 요청은 모두 ADMIN의 심사가 필요하다. 이 요청들을 P7에 하나의 원본 테이블로 모으면 관리자 화면은 단순해지지만, Seller·Catalog·Offer의 상세 payload와 상태 소유권이 P7로 이동하고 도메인 간 결합이 커진다.

## Decision Drivers

- 각 도메인의 요청 payload와 상태 소유권 유지
- 관리자 통합 목록·필터·정렬 지원
- 승인·거절 처리 결과의 일관성
- 원본 요청과 관리자 조회 항목의 불일치·재처리 대응

## Considered Options

### Option A: 모든 요청을 P7 원본 테이블에 저장

P7이 요청 payload, 상태, 승인·거절 결과를 모두 소유한다.

### Option B: 도메인별 원본 요청과 P7 통합 조회 모델을 분리

P8과 P9가 요청 원본과 업무 상태를 소유하고, P7은 `AdminReviewItem`으로 관리자 목록용 요약과 원본 참조를 저장한다.

## Decision

Option B를 채택한다.

- P8 `SellerApplication`은 판매자 신청 원본을, P8 `SellerApplicationReview`는 신청 심사 이력을, P8 `CatalogRegistrationRequest`는 Catalog·Variant 등록 요청 원본을, P9 `OfferActivationRequest`는 Offer 활성화 요청 원본을 소유한다.
- P7 `AdminReviewItem`은 `requestType`, `sourceModule`, `sourceRequestId`, 판매자·요청자, 목록용 `summary`, 통합 상태와 처리 이력만 저장한다.
- P7의 승인·거절 API는 원본 모듈의 공개 application interface를 호출하고, 처리 결과를 `AdminReviewItem`에 반영한다.
- 원본 payload의 상세 조회는 `sourceModule`과 `sourceRequestId`를 통해 원본 모듈에서 수행한다.

## Consequences

### Positive

- P7 관리자 화면에서 서로 다른 심사 요청을 하나의 목록으로 조회할 수 있다.
- 각 도메인이 자신의 상태 전이와 요청 payload를 독립적으로 관리한다.
- P7이 Catalog·Offer·Seller 내부 모델을 직접 소유하지 않는다.

### Negative

- 원본 요청과 `AdminReviewItem` 사이의 동기화가 필요하다.
- 통합 목록의 요약 정보가 원본과 잠시 다를 수 있다.
- 관리자 상세 조회가 원본 모듈 호출로 이어질 수 있다.

### Follow-up

- P7 통합 심사 목록 API를 추가할 때 `AdminReviewItem`을 사용한다.
- 동기화 방식은 초기에는 원본 application interface 조회로 시작하고, 필요 시 Outbox 이벤트 기반 projection으로 전환한다.
- 원본 요청 처리와 통합 항목 갱신의 멱등성 규칙을 구현 단계에서 정의한다.

## Evidence

- [P7 Admin 관리자 심사 요청 조회](../requirement/p7/p7-admin.md#1-관리자-api-관계)
- [P8 Seller 신청·심사 모델](../requirement/p8/p8-seller-application.md#sellerapplication)
- [P8 CatalogRegistrationRequest 모델](../requirement/p8/p8-catalog-requests.md#catalogregistrationrequest)
- [P9 Offer 요청 모델](../requirement/p9/p9-offer.md#2-3-offeractivationrequest)
- [Project Context](../../CONTEXT.md)
