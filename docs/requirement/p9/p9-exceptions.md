# P9 Exceptions (예외 코드 카탈로그)

P9 리소스 API의 예외 매트릭스는 각 API 문서에 정의한다. 이 문서는 P3·P7 등 외부 도메인이 P9 예외의 원본 코드와 의미를 참조하기 위한 카탈로그다. 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. P9 API 예외 코드

| exceptionCode | HTTP | 의미 | 원본 API |
|---|---:|---|---|
| `OFFER-001` | 404 | Offer가 없음 | [P9 Offer API](p9-offer.md) |
| `OFFER-002` | 400 | Offer 요청 필드 검증 실패 | [P9 Offer API](p9-offer.md) |
| `OFFER-003` | 400 | Offer 가격 검증 실패 | [P9 Offer API](p9-offer.md) |
| `OFFER-004` | 409 | 같은 Seller·ProductVariant Offer 중복 | [P9 Offer API](p9-offer.md) |
| `OFFER-005` | 409 | 보관된 Offer 변경 시도 | [P9 Offer API](p9-offer.md) |
| `OFFER-006` | 409 | 관리자 차단 Offer 직접 활성화 시도 | [P9 Offer API](p9-offer.md) |
| `OFFER-007` | 409 | Seller·CatalogProduct·ProductVariant 의존 상태 비활성 | [P9 Offer API](p9-offer.md) |
| `OFFER-008` | 409 | PENDING 활성화 요청 중복 | [P9 Offer API](p9-offer.md) |
| `OFFER-009` | 404 | Offer Media가 없음 | [P9 Offer API](p9-offer.md) |
| `OFFER-010` | 400 | Offer 상태값 검증 실패 | [P9 Offer API](p9-offer.md) |
| `OFFER-011` | 400 | Offer 활성화 요청 입력 검증 실패 | [P9 Offer API](p9-offer.md) |
| `OFFER-012` | 400 | Offer Media 입력 검증 실패 | [P9 Offer API](p9-offer.md) |
| `INVENTORY-001` | 400 | 재고 조정 요청 필드 검증 실패 | [P9 Inventory API](p9-inventory.md) |
| `INVENTORY-002` | 400 | 조정 후 재고가 음수 | [P9 Inventory API](p9-inventory.md) |
| [CATEGORY-003](../p2/p2-category.md) | 404 | P2 Category 원본 예외 참조 | [P2 Category](../p2/p2-category.md) |
| [CATALOG-019](../p2/p2-catalog-product.md) | 404 | P2 CatalogProduct 원본 예외 참조 | [P2 CatalogProduct](../p2/p2-catalog-product.md) |
| [CATALOG-031](../p2/p2-product-variant.md) | 404 | P2 ProductVariant 원본 예외 참조 | [P2 ProductVariant](../p2/p2-product-variant.md) |
| [CATALOG-033](../p2/p2-product-variant.md) | 409 | P2 ProductVariant 원본 예외 참조 | [P2 ProductVariant](../p2/p2-product-variant.md) |
| `CATALOG-040` | 400 | Seller Catalog API 자체 검증 | [P9 Seller Catalog API](p9-seller-catalog.md) |
| `MARKETPLACE-001` | 400 | Marketplace 검색 조건 검증 실패 | [P9 Marketplace API](p9-marketplace.md) |
| `MARKETPLACE-002` | 400 | Marketplace cursor 검증 실패 | [P9 Marketplace API](p9-marketplace.md) |

공통 인증·관리자 권한 코드는 다음 원본을 참조한다.

| exceptionCode | HTTP | 의미 |
|---|---:|---|
| [AUTH-001](../index.md#예외-응답) | 401 | [공통 API 계약](../index.md#예외-응답) |
| [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | 403 | [P7 Admin API](../p7/p7-admin.md#4-공통-예외) |
| [SELLER-001](../p8/p8-seller-profile.md) | 403 | [P8 Seller API](../p8/p8-seller-profile.md) |
| [SELLER-002](../p8/p8-seller-profile.md) | 403 | [P8 Seller API](../p8/p8-seller-profile.md) |
| [SELLER-003](../p8/p8-seller-profile.md) | 403 | [P8 Seller API](../p8/p8-seller-profile.md) |

각 API의 조건과 client/system message는 원본 API 문서를 따른다.

## 2. 외부 도메인 예외 참조

| exceptionCode | 의미 |
|---|---|
| [OFFER-001](p9-offer.md) | P9 Offer 미존재 원본 |
| [OFFER-005](p9-offer.md) | P9 Offer 보관 원본 |
| [OFFER-006](p9-offer.md) | P9 관리자 차단 원본 |
| [OFFER-008](p9-offer.md) | P9 활성화 요청 중복 원본 |
| [SELLER-001](../p8/p8-seller-profile.md) | P8 Seller 자격 원본 |
| [SELLER-002](../p8/p8-seller-profile.md) | P8 Seller 정지 원본 |
| [SELLER-003](../p8/p8-seller-profile.md) | P8 Seller 소유권 원본 |

## 3. 예외 소유권 원칙

- P9 원본 리소스의 상태·가격·재고 예외는 P9가 정의한다.
- P2 Catalog 예외는 [P2 Catalog](../p2/p2-catalog.md)가 정의하고, P9는 P2 공개 계약의 결과를 P9 API에 등록한다.
- P7 관리자 API의 `ADMIN-xxx`와 심사 처리 예외는 [P7 Offer API](../p7/p7-offer.md)가 정의한다.
- P5 주문·결제 중 재고 부족과 주문 재검토 예외는 P5가 정의한다. P9는 재고 상태와 조정 계약만 제공한다.
- client message에는 내부 상태·ID·SQL·stack trace를 포함하지 않는다. system message에는 requestId와 내부 원인을 기록할 수 있다.
