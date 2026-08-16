# P9 Offer & Marketplace Policy

이 문서는 P9의 범위·책임과 API에 독립적인 업무 정책을 정의한다. 데이터 모델과 API 계약은 [P9 Offer API](p9-offer.md), [P9 Inventory API](p9-inventory.md), [P9 Seller Catalog API](p9-seller-catalog.md), [P9 Marketplace API](p9-marketplace.md)를 따른다.

## 1. 범위와 책임

### 범위

- Seller별 ProductVariant 판매 조건인 Offer의 생성·조회·수정·활성·비활성·보관
- Offer별 가격·기간성 할인·구매 수량 제한
- Offer에 연결된 Inventory와 구매 가능 상태
- Seller가 등록 대상을 찾기 위한 Catalog 조회
- 고객용 상품 검색·상세 조합 조회
- Offer 상태 이력·관리자 차단 재활성화 요청·Offer Media

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Offer·Inventory·OfferMedia 원본·상태 | P9 | [P9 Offer API](p9-offer.md), [P9 Inventory API](p9-inventory.md) |
| Seller 소유권과 Seller 상태 | P8 | [P8 Seller Policy](../p8/p8-policy.md), [P8 Seller Profile](../p8/p8-seller-profile.md) |
| CatalogProduct·ProductVariant·Category 원본 | P2 | [P2 Catalog](../p2/p2-catalog.md) |
| 관리자 Offer 운영·재활성화 심사 진입점 | P7 | [P7 Offer](../p7/p7-offer.md) |
| 결제 후 재고 차감·주문 취소 보상 | P5·P6 | [P5 Payment Process](../p5/p5-payment-process.md), [P6 Infrastructure](../p6/p6-infrastructure.md) |
| Review 원본·Review 요약 | P10 | [P10 Review](../p10/p10-review.md) |

P9는 Seller·Catalog·Review·Order의 내부 모델·Repository·서비스 구현을 소유하거나 직접 참조하지 않는다.

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Offer` | Seller가 특정 ProductVariant를 판매하는 가격·상태·구매 제한의 집합 |
| `Inventory` | 특정 Offer의 구매 가능 수량. CatalogProduct 전체 재고가 아니다. |
| `OfferMedia` | Seller가 자신의 Offer를 소개하기 위해 연결한 Media attachment |
| `OfferStatusHistory` | Offer의 모든 상태 변경 당시 사유·메시지·처리자를 보존하는 불변 이력 |
| `OfferActivationRequest` | 관리자 차단 Offer의 문제 해결 내용을 제출하고 재활성화를 요청하는 기록 |
| `PRODUCT_MANAGER` | ACTIVE Seller를 가진 User. 본인 Offer를 등록·변경하고 재고를 조정한다. |
| `ADMIN` | 전체 Offer·Inventory를 운영하고 관리자 차단 Offer의 활성화 요청을 심사한다. |
| 고객 | 공개 가능한 ACTIVE Offer와 구매 가능 상태만 조회한다. |

기존 용어와 의미가 충돌하면 [domain-glossary.md](../../domain-glossary.md)와 [CONTEXT.md](../../../CONTEXT.md)를 함께 갱신한다.

## 3. 핵심 업무 규칙

- 하나의 Seller는 같은 ProductVariant에 Offer를 하나만 가진다.
- Offer 생성 시 Inventory를 함께 만들고 초기 수량은 `0`이다.
- Offer 등록은 ACTIVE Seller가 보관되지 않은 ACTIVE ProductVariant에 대해서만 할 수 있다.
- `sellerId`는 요청 본문으로 받지 않고 인증된 Seller에서 결정한다.
- Offer의 `maxPurchaseQuantity`는 고객 한 명이 하나의 주문에서 구매할 수 있는 최대 수량이며 1 이상의 정수다.
- 가격은 0 이상이고, 할인 가격은 기본 가격보다 작아야 한다. 기간이 끝나면 기본 가격을 적용한다.
- 판매자 자발적 비활성화는 `SELLER_REQUEST`로 기록하고 Seller가 조건을 충족하면 직접 활성화할 수 있다.
- 관리자 비활성화는 사유 코드와 Seller 안내 문구를 기록하며 Seller가 직접 우회 활성화할 수 없다.
- 관리자 차단 Offer는 Seller의 해결 설명이 포함된 활성화 요청을 ADMIN이 승인해야 `ACTIVE`가 된다.
- Seller 정지 또는 Catalog 보관에 따른 시스템 비활성화는 원인이 해결되어도 자동으로 활성화하지 않는다.
- Offer 보관은 물리 삭제가 아닌 `ARCHIVED`이며 다시 활성화하지 않는다.
- 보관된 Offer·OfferMedia와 비활성 Offer는 고객용 검색·상세에서 제외한다.
- Offer Media의 대표 이미지는 Offer당 Active 상태에서 최대 하나다.

## 4. 불변식과 상태 전이

### 불변식

- `(sellerId, variantId)`는 유일하다.
- Offer는 정확히 하나의 Inventory를 가지며 Inventory 수량은 0 이상이다.
- `ACTIVE` Offer가 공개되려면 Seller·CatalogProduct·ProductVariant가 모두 활성 상태여야 한다.
- `ARCHIVED`는 Offer와 OfferMedia의 terminal 상태다.
- 상태가 변경될 때마다 `OfferStatusHistory`를 추가하며 기존 이력은 수정·삭제하지 않는다.
- Offer당 `PENDING` 활성화 요청은 하나만 존재한다.
- `appliedPrice`, `discountRate`, `availabilityStatus`는 원본 저장값이 아니라 현재 시간·Inventory·공개 상태로 계산한다.

### 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| 없음 | ACTIVE Seller가 유효한 Variant에 등록 | `ACTIVE` | Seller API |
| `ACTIVE` | Seller가 판매 중단 | `INACTIVE` | Seller |
| `ACTIVE` | 관리자 정책 차단 | `INACTIVE` | ADMIN/P7 |
| `ACTIVE` | Seller 정지·Catalog 보관 | `INACTIVE` | SYSTEM |
| `INACTIVE` + `SELLER` | Seller가 의존 상태를 확인 | `ACTIVE` | Seller |
| `INACTIVE` + `ADMIN` | 활성화 요청 승인 | `ACTIVE` | ADMIN/P7 |
| `INACTIVE` + `SYSTEM` | 원인 해결 후 명시적 활성화 | `ACTIVE` | Seller 또는 ADMIN |
| `ACTIVE` 또는 `INACTIVE` | Seller가 보관 요청 | `ARCHIVED` | Seller |
| `ARCHIVED` | 모든 활성화·수정 요청 | 유지 | 없음, 요청 거절 |

`OfferActivationRequest`는 `PENDING → APPROVED` 또는 `PENDING → REJECTED`로 한 번만 처리한다. 승인 시 Offer를 `ACTIVE`로, 거절 시 Offer를 `INACTIVE`로 유지한다.

## 5. 도메인 간 규칙과 예외 소유권

- P9는 P2의 공개 Catalog 계약으로 ProductVariant 존재·활성·보관 상태와 Category 하위 ID를 확인한다. P2 내부 Repository를 직접 참조하지 않는다.
- P9는 P8의 Seller 소유권·상태·공개 Seller 정보 계약을 사용한다. Seller 신청·프로필·역할은 P8이 소유한다.
- P7은 관리자 Offer 상태 변경과 활성화 요청 심사의 HTTP 진입점을 제공한다. Offer 상태·요청 원본과 불변식은 P9가 소유한다.
- P5는 주문 생성·결제 직전에 Offer 가격·판매 상태·재고를 검증하고, P9는 현재 판매 조건과 재고 공개 계약을 제공한다.
- P5 결제 완료 후 재고 차감과 P6 Saga 보상에 필요한 이벤트·멱등성은 P5/P6 계약을 따른다.
- P10은 Review 원본과 요약을 소유하며 P9 Marketplace는 요약을 조합한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P2 Catalog | Variant·CatalogProduct·Category 활성·보관 확인 | [P2 Catalog](../p2/p2-catalog.md) |
| P8 Seller | Seller 소유권·상태 확인 | [P8 Seller Policy](../p8/p8-policy.md), [P8 Seller Profile](../p8/p8-seller-profile.md) |
| P7 Admin | 관리자 상태 변경·활성화 심사 | [P7 Offer](../p7/p7-offer.md) |
| P5 Order·Payment | 주문 검증·결제 완료 후 재고 처리 | [P5 Payment Process](../p5/p5-payment-process.md) |
| P6 Outbox & Saga | 이벤트 멱등성·실패 보상 | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| P10 Review | Review 원본·요약 | [P10 Review](../p10/p10-review.md) |

예외 코드는 각 API 문서에서 정의하며, 외부 도메인의 예외를 반환할 때는 [P9 Exceptions](p9-exceptions.md)와 해당 도메인의 원본 계약을 함께 참조한다.

## 6. API 문서와의 관계

- Offer의 필드·관계·판매자 변경 API는 [P9 Offer API](p9-offer.md)에서 정의한다.
- Inventory의 필드·재고 조정·차감 연동은 [P9 Inventory API](p9-inventory.md)에서 정의한다.
- Seller용 Catalog 조회와 고객용 Marketplace 조회는 각각 [P9 Seller Catalog API](p9-seller-catalog.md), [P9 Marketplace API](p9-marketplace.md)에서 정의한다.
- 정책과 API 문서의 요구사항이 충돌하면 이 문서의 정책을 기준으로 API 문서를 수정한다.
- 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따르고, API별 예외 코드는 각 API 문서에 등록한다.
