# P9 Offer & Marketplace 문서 안내

P9의 요구사항 문서 목록과 책임 경계를 안내한다. API와 데이터 모델은 리소스 문서에서, API에 독립적인 업무 규칙은 정책 문서에서 정의한다.

공통 URI, 성공 응답 원칙, 예외 응답 필드, 페이지네이션, 인증은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P9 Policy](p9-policy.md) | 정책 | 범위·책임, 행위자, 업무 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [P9 Offer API](p9-offer.md) | 데이터 모델·API | Offer, 상태 이력, 활성화 요청, Media 모델과 판매자 Offer API |
| [P9 Inventory API](p9-inventory.md) | 데이터 모델·API | Inventory 모델, 재고 조정 API, 구매 가능 상태와 연동 |
| [P9 Seller Catalog API](p9-seller-catalog.md) | 조회 API | 판매자 Offer 등록 대상 CatalogProduct·ProductVariant 조회 |
| [P9 Marketplace API](p9-marketplace.md) | 조회 API | 고객용 상품 검색·상세와 공개 Offer 조합 |
| [P9 Exceptions](p9-exceptions.md) | 예외 카탈로그 | P9 외부 도메인이 참조하는 원본 예외 코드 |

P9의 핵심 리소스는 Offer이며, Inventory·OfferMedia·OfferStatusHistory·OfferActivationRequest는 Offer와 연결된 하위 모델이다. 행위자·상태·조회 목적이 달라지는 Catalog와 Marketplace는 별도 API 문서로 분리한다.

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| Offer·Inventory·OfferMedia 원본·상태 | P9 Offer & Marketplace | [P9 Policy](p9-policy.md), [P9 Offer API](p9-offer.md), [P9 Inventory API](p9-inventory.md) |
| Seller 소유권·Seller 상태·판매자 자격 | P8 Seller | [P8 Seller Policy](../p8/p8-policy.md), [P8 Seller Profile](../p8/p8-seller-profile.md) |
| ProductVariant·CatalogProduct·Category 원본과 활성·보관 상태 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| 관리자 Offer 운영·활성화 심사 진입점 | P7 Admin | [P7 Offer](../p7/p7-offer.md) |
| 주문 시 가격·재고 검증과 결제 후 처리 | P5 Order·Payment | [P5 Order Checkout](../p5/p5-order-checkout.md), [P5 Payment Process](../p5/p5-payment-process.md) |
| 이벤트 발행·멱등성·재고 보상 흐름 | P6 Outbox & Saga | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| Review 원본과 Review 요약 | P10 Review | [P10 Review](../p10/p10-review.md) |

- P9는 Seller·Catalog·Review·Order의 내부 모델이나 Repository를 소유하지 않는다.
- 외부 도메인의 모델은 식별자와 공개 계약만 참조한다. 필드와 상태의 원본 정의는 소유 도메인 문서를 따른다.
- 다른 도메인의 예외는 P9 API 응답에 포함할 수 있지만, 예외 코드와 메시지의 원본 정의는 해당 도메인 문서를 참조한다.
- P9의 고객용 검색·상세는 P2 Catalog, P9 Offer·Inventory, P10 Review를 조합한 조회 모델이다. 어느 외부 원본의 소유권도 이전하지 않는다.

## 3. 문서 작성 순서

1. [P9 Policy](p9-policy.md)에서 범위·책임과 확정 업무 규칙을 정한다.
2. [P9 Offer API](p9-offer.md)와 [P9 Inventory API](p9-inventory.md)에서 정책을 만족하는 데이터 모델과 변경 API를 정의한다.
3. [P9 Seller Catalog API](p9-seller-catalog.md)와 [P9 Marketplace API](p9-marketplace.md)에서 외부 모델 조합 조회를 정의한다.
4. API별 성공·예외 응답과 외부 도메인 예외 참조를 완성한다.
5. 이 인덱스의 문서 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 이 문서는 안내와 책임 경계만 작성하고, 정책·필드·API 계약을 중복해서 작성하지 않는다.
- 정책 문서는 API나 ORM 구현보다 오래 유지되는 업무 규칙을 작성한다.
- 리소스 API 문서는 데이터 모델, 관계·제약, 요청·성공 응답·API별 예외 매트릭스를 작성한다.
- 구현 결과가 달라질 수 있는 선택 표현 대신 하나의 기본 동작을 확정한다.
- 문서 간 규칙이 충돌하면 P9 Policy를 기준으로 리소스 API 문서를 수정한다.
