# P2 Catalog API

이 문서는 P2가 소유하는 Category·CatalogProduct·ProductVariant의 관계와 다른 도메인이 사용할 공개 application interface를 정의한다. 업무 정책은 [P2 Policy](p2-policy.md), 각 리소스의 필드와 HTTP 계약은 [Category API](p2-category.md), [CatalogProduct API](p2-catalog-product.md), [ProductVariant API](p2-product-variant.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Category` | 상품 분류 계층과 검색 범위 | 조회·생성·수정 |
| `CatalogProduct` | 여러 Variant가 공유하는 상품 공통 정보 | 조회·생성·수정·보관 |
| `ProductVariant` | 고객이 선택하고 주문하는 실제 구매 단위 | 생성·수정·보관·조회 |
| `CatalogProductMedia` | CatalogProduct의 이미지 대상·순서·대표·보관 상태 | 연결·수정·보관 |

```text
Category 1 : N CatalogProduct
Category 1 : N Category (parent → child)
CatalogProduct 1 : N ProductVariant
ProductVariant 1 : N Offer       (P9)
Offer          1 : 1 Inventory   (P9)
Offer          1 : N Review      (P10)
```

- 상품 연결은 `CatalogProduct.categoryId` 단일 FK다. 별도의 다대다 Category 연결 테이블을 사용하지 않는다.
- Category 검색 범위는 선택한 Category와 모든 하위 Category다.
- `CatalogProduct`와 `ProductVariant`의 내부 ID는 서버가 생성한다. 고객용 Product 응답에는 반환하지 않는다. 관리자·Product Manager용 Catalog 조회 응답에는 등록 대상 선택을 위해 반환한다.
- ProductType·ItemType·SearchKeyword는 현재 리소스 모델에 포함하지 않는다. 상세 내용은 [심화 문서](p2-product-type.md), [SearchKeyword](p2-search-keyword.md)를 따른다.

## 2. 관계와 제약

- 하나의 CatalogProduct는 대표 Category 하나를 가진다.
- 하나의 ProductVariant는 CatalogProduct 하나에만 속한다.
- CatalogProduct를 보관하면 하위 Variant는 공개 조회와 Offer 등록 대상에서 제외한다.
- ProductVariant를 보관하면 연결된 Offer를 P9 규칙에 따라 비활성화하고 공개 조회와 Offer 등록 대상에서 제외한다.
- Offer·Inventory·Review는 P2가 생성하거나 상태를 소유하지 않는다.
- P2에는 SKU를 도입하지 않는다. 입력·자동 생성·중복 검증·SKU 조회·응답 및 주문 스냅샷 포함을 모두 제공하지 않는다.

## 3. 공개 application interface

다음 interface는 Spring Modulith의 공개 계약으로 제공한다. HTTP 검증 API를 별도로 만들지 않으며, 반환 객체는 HTTP 응답으로 노출하지 않는다.

```text
CatalogCategoryValidationApi.validate(CategoryProposal)
  → CategoryValidationResult(valid, depth, errors)

CatalogCategoryQueryApi.findSelfAndDescendantIds(categoryId)
  → Set<CategoryId>

CatalogVariantQueryApi.findActiveByVariantId(variantId)
  → CatalogVariantReference
```

### Catalog 관리자·Product Manager 조회 API

`GET /api/v1/catalog-products`

권한: `ADMIN` 또는 `PRODUCT_MANAGER` 권한과 `ACTIVE Seller` 상태를 가진 사용자.

관리자는 운영 목적으로, Product Manager는 Offer 등록 대상 CatalogProduct와 ProductVariant를 찾는 목적으로 사용한다. 응답은 CatalogProduct와 연결된 ProductVariant를 함께 반환하며 `catalogProductId`와 `variantId`를 포함한다.

Query는 `page`, `size`, `keyword`, `categoryId`, `tag`, `catalogPublicationStatus`, `variantPublicationStatus`, `sort`를 지원한다. `categoryId`는 자기 자신과 모든 하위 Category를 검색한다. 일반 사용자는 이 API를 사용할 수 없으며 고객용 검색은 [P9 Marketplace](../p9/p9-marketplace.md)의 Product API가 담당한다.

상세 조회는 `GET /api/v1/catalog-products/{catalogProductId}`, Variant 단건 조회는 `GET /api/v1/product-variants/{variantId}`를 사용하며 같은 권한 정책을 따른다.

- 기본적으로 CatalogProduct와 ProductVariant 모두 `ACTIVE`만 조회한다.
- 관리자는 상태 Query를 지정하여 `ARCHIVED`도 조회할 수 있다.
- Product Manager는 항상 `ACTIVE` 데이터만 조회한다.

| 호출자 | 사용 목적 | P2가 보장하는 결과 |
|---|---|---|
| P7 | Category 생성·수정 전 검증 | 부모 존재, 순환, 중복, 최대 깊이 검증 |
| P8 | Seller Category·Catalog 등록 요청 검증 | P2의 동일한 Category·상품 규칙 재사용 |
| P9 | Category 조건 검색 | 자기 자신과 모든 하위 Category ID 반환 |
| P9 | Offer 등록 전 Variant 확인 | 존재하고 `ACTIVE`인 Variant만 반환 |

P8의 제안 검증과 P7의 승인 시점에는 같은 검증을 다시 수행한다. 검증 interface는 Category를 생성하거나 이름을 예약하지 않는다.

## 4. 역할별 조회와 내부 ID

| 리소스 상태 | ADMIN | 구매자·Seller |
|---|---|---|
| 존재하는 `ACTIVE` | `200` | `200` |
| 존재하는 `ARCHIVED` | `200` | 존재하지 않는 리소스와 같은 `404` |
| 실제 미존재 | `404` | `404` |

- Catalog 관리 조회의 목록·조건 검색에는 기본적으로 보관된 상품과 Variant를 포함하지 않는다.
- 고객용 Product 응답에는 `catalogProductId`, `variantId`를 반환하지 않는다. 관리자·Product Manager용 Catalog 조회 응답에는 등록 대상 선택을 위해 반환한다.
- 보관 데이터와 미존재 데이터의 클라이언트 응답은 동일한 일반 메시지를 사용한다.
- 서버 로그에는 `lookupResult = ARCHIVED` 또는 `NOT_FOUND`, `requestId`, 역할, 리소스 종류, 내부 ID, 요청 경로, 실패 원인을 기록한다. 이 내부 판단은 클라이언트에 반환하지 않는다.

예를 들어 보관된 CatalogProduct를 구매자가 조회하면 `404`와 `상품을 찾을 수 없습니다.`를 반환하고, 서버에는 `result=ARCHIVED`를 기록한다.

## 5. API 문서와의 관계

- HTTP API의 상세 요청·성공 응답·예외 매트릭스는 각 리소스 문서에서 정의한다.
- CatalogProduct Media의 파일 업로드·검증·저장소 계약은 [P12 Media](../p12/p12-media.md)를 따르고, P2는 대상 연결·정렬·대표·보관 규칙만 소유한다.
- P2와 P9가 함께 만드는 고객 상품 검색·상세 응답은 [P9 Marketplace](../p9/p9-marketplace.md)가 조합한다.
