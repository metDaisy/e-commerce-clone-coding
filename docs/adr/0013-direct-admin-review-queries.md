# ADR-0013: 도메인별 관리자 심사 요청 직접 조회

- Status: Accepted
- Date: 2026-08-16
- Deciders: 사용자와 Codex
- Supersedes: ADR-0012
- Superseded by: 없음

## Context

판매자 신청, Catalog 등록 요청, Offer 활성화 요청은 모두 관리자의 승인·거절이 필요하다. 그러나 요청마다 payload와 상태 전이가 다르므로 이를 P7의 통합 저장 모델에 복제하면 원본 요청과의 동기화 비용과 도메인 결합이 증가한다.

## Decision

각 도메인이 관리자 심사 요청의 원본과 처리 이력을 소유한다.

- P8 `SellerApplication`과 `SellerApplicationReview`는 판매자 신청과 심사 이력을 소유한다.
- P8 `CatalogRegistrationRequest`는 Category·CatalogProduct·ProductVariant 등록 요청과 처리 결과를 소유한다.
- P9 `OfferActivationRequest`는 Offer 활성화 요청과 처리 결과를 소유한다.
- P7은 각 원본 모델을 직접 조회하는 관리자 목록·상세·승인·거절 API를 제공한다.
- 통합 관리자 화면이 필요하면 저장하지 않는 조회 DTO 또는 화면 조합으로 구현한다.
- 승인·거절 결과와 처리 관리자·처리 시각은 원본 요청과 도메인 이력에 기록한다.

P7에는 `AdminReviewItem`과 같은 통합 영속 모델을 두지 않는다.

## Consequences

### Positive

- 요청 상태의 단일 원본이 유지된다.
- 원본과 관리자 조회 모델의 동기화가 필요하지 않다.
- 요청 유형별 payload와 상태 전이를 각 도메인에서 명확하게 관리할 수 있다.
- 초기 구현에서 별도 projection 테이블과 이벤트 동기화를 만들지 않아도 된다.

### Negative

- 통합 관리자 목록이 필요하면 여러 원본 조회를 조합해야 한다.
- 요청 유형이 많아지면 관리자 화면의 조회 DTO 조합이 복잡해질 수 있다.
- 조회 성능 문제가 발생할 때만 별도 read model을 추가로 검토한다.

## Evidence

- [P7 관리자 API 목록](../requirement/p7/p7-admin.md#3-관리자-api-목록)
- [P8 Seller 신청 모델](../requirement/p8/p8-seller-application.md#sellerapplication)
- [P8 Catalog 등록 요청 모델](../requirement/p8/p8-catalog-requests.md#catalogregistrationrequest)
- [P9 Offer 모델](../requirement/p9/p9-offer.md)
