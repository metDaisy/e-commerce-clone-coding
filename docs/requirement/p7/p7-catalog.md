# P7 Catalog Administration Policy

이 문서는 관리자가 Catalog를 운영하고 판매자의 Catalog 등록 요청을 심사하는 정책을 정의한다. 정식 Catalog 데이터와 검증은 P2, 판매자의 등록 요청 원본은 P8이 소유한다.

## 1. 범위와 책임

### 범위

- 관리자의 Category 생성·수정
- 관리자의 CatalogProduct·ProductVariant 관리 목록 조회
- 판매자의 Category·CatalogProduct·ProductVariant 등록 요청 심사
- 승인된 요청의 P2 Catalog 생성 application interface 호출

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Category·CatalogProduct·ProductVariant 원본·검증·보관 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| Catalog 등록 요청 원본·요청 상태 | P8 Seller | [P8 Catalog 등록 요청](../p8/p8-catalog-requests.md) |
| 관리자 HTTP 진입점·권한 | P7 Admin | [P7 Catalog Request API](p7-catalog-requests.md) |
| 판매자의 Offer·Inventory | P9 Offer | [P9 Offer](../p9/p9-offer.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Category` | CatalogProduct를 분류하는 계층형 기준 데이터 |
| `CatalogProduct` | 판매자 Offer와 분리된 플랫폼 공통 상품 정보 |
| `ProductVariant` | CatalogProduct의 선택 가능한 변형 |
| `CatalogRegistrationRequest` | 판매자가 Catalog 생성 또는 Variant 추가를 요청하는 P8 원본 |
| `ADMIN` | Category를 직접 관리하고 등록 요청을 수동 승인·거절한다. |
| `PRODUCT_MANAGER` | 활성 Seller를 가진 판매자. Catalog 등록 요청을 제출할 수 있다. |

## 3. 핵심 업무 규칙

- Category는 ADMIN이 직접 생성·수정한다. 판매자는 Category를 직접 생성하지 않고 등록 요청으로 제안한다.
- 판매자는 활성 Seller를 통해 Category·CatalogProduct·ProductVariant 등록 요청을 제출한다.
- 모든 등록 요청은 `PENDING`으로 저장하며 자동 승인하지 않는다.
- ADMIN은 요청 payload와 P2의 현재 검증 결과를 확인한 뒤 승인 또는 거절한다.
- Category 요청 승인은 P2 Category 생성 API를 호출하고 `createdCategoryId`를 기록한다.
- CatalogProduct 요청 승인은 P2 CatalogProduct 생성 API를 호출하고 `createdCatalogProductId`를 기록한다.
- ProductVariant 요청 승인은 기존 `targetCatalogProductId`에 Variant를 생성하고 `createdVariantId`를 기록한다.
- Catalog 등록 승인만으로 Seller Offer·Inventory를 생성하지 않는다.
- 거절된 요청은 수정하지 않고, 판매자가 새 요청을 제출한다.

## 4. 불변식과 상태 전이

### 불변식

- Category 깊이는 1~3이고 부모 연결은 순환할 수 없다.
- 동일한 대상에 처리 중인 Catalog 등록 요청은 하나만 허용한다.
- 승인·거절된 요청은 다시 처리할 수 없다.
- ProductVariant 승인 시 기존 CatalogProduct를 수정하거나 새 CatalogProduct를 생성하지 않는다.
- P7은 P2의 검증 규칙을 복제하지 않고 공개 validation interface를 사용한다.

### 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| 등록 요청 `PENDING` | P2 검증 성공 및 관리자 승인 | `APPROVED` | ADMIN |
| 등록 요청 `PENDING` | 정보 오류·중복·Category 문제 | `REJECTED` | ADMIN |

## 5. 도메인 간 규칙과 예외 소유권

- Category·CatalogProduct·ProductVariant 생성 결과와 검증 예외의 원본은 P2가 소유한다.
- Catalog 등록 요청 상태·거절 메시지·처리 이력의 원본은 P8이 소유한다.
- P7은 관리자 응답에 필요한 예외만 노출하고, P2·P8의 내부 구현과 저장 구조를 복제하지 않는다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P2 Catalog | Category·CatalogProduct·Variant 검증·생성 | [P2 Catalog](../p2/p2-catalog.md) |
| P8 Seller | Seller 자격·등록 요청 조회·상태 처리 | [P8 Catalog 등록 요청](../p8/p8-catalog-requests.md) |
| P9 Offer | Catalog 승인 이후 Offer 생성 여부 분리 | [P9 Offer](../p9/p9-offer.md) |

## 6. API 문서와의 관계

- Category API는 [P7 Category API](p7-category.md)를 따른다.
- CatalogProduct·Variant 목록 API는 [P7 CatalogProduct API](p7-catalog-products.md)를 따른다.
- 판매자 등록 요청 API는 [P7 Catalog Request API](p7-catalog-requests.md)를 따른다.
- 공통 권한과 오류 응답은 [P7 Admin API](p7-admin.md)와 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
