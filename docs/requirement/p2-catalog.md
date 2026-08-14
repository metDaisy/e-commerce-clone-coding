# P2 Catalog (카탈로그)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 소유권

P2는 상품의 공통 메타데이터와 전시 정보를 소유한다.

- Category: 상품 분류 메타데이터와 계층
- CatalogProduct: 상품군의 공통 메타데이터
- ProductVariant: 고객이 선택하고 주문하는 실제 구매 단위
- 상품용 Media attachment: CatalogProduct 대표 이미지와 정렬·대표 이미지·보관 규칙

정식 CatalogProduct·ProductVariant·CatalogProduct Media의 생성·수정·보관은 `ADMIN`만 수행한다. Seller는 P8을 통해 등록을 요청하고, P7이 관리자 진입점과 승인 처리를 제공한다. 판매자 소개 이미지는 P9 Offer Media로 관리한다.

Offer와 Inventory는 P9가 소유하며, Review Media와 Review는 P10이 소유한다. 실제 파일 저장·CDN·스토리지 삭제는 공통 Media 인프라가 담당한다.

## 2. 도메인 관계

```text
Category 1 : N CatalogProduct
CatalogProduct 1 : N ProductVariant
ProductVariant 1 : N Offer       (P9)
Offer          1 : 1 Inventory    (P9)
Offer          1 : N Review       (P10, 구매 경험 리뷰)
```

- Category 연결은 `catalog_products.category_id` 단일 FK로 저장한다. 하나의 CatalogProduct에는 대표 Category 하나만 연결한다.
- Category의 상위 경로는 `parentId`를 따라 P2가 계산한다. Category 검색은 선택한 Category와 모든 하위 Category에 연결된 CatalogProduct를 포함한다.
- 현재 V1 스키마의 `tags`·`catalog_product_tags`는 현재 목표 범위에서 제외한 이전 Tag 모델이다. 구현 정리 시 별도 마이그레이션 또는 제거가 필요하다.
- 하나의 CatalogProduct는 하나의 대표 Category를 가진다.
- 하나의 ProductVariant는 하나의 CatalogProduct에만 속한다.
- `catalogProductId`와 `variantId`는 서버가 생성하는 내부 식별자다. 고객·Seller 상품 응답에는 반환하지 않고 관리자·내부 Modulith 호출에서만 사용한다.
- CatalogProduct·ProductVariant가 보관되면 P9가 연결된 Offer를 구매 대상에서 제외한다.

상세 규칙은 다음 문서로 분리한다.

- [P2 Category](p2-category.md)
- [P2 CatalogProduct](p2-catalog-product.md)
- [P2 ProductVariant](p2-product-variant.md)

## ProductVariant 식별자 정책

ProductVariant는 서버가 생성한 `variantId`를 유일한 식별자로 사용한다. `variantId`는 CatalogProduct에 속한 실제 판매 단위를 P2 내부와 Modulith 연계에서 구분하기 위한 값이며, 요청으로 직접 지정하지 않는다.

이번 범위에서는 SKU를 사용하지 않는다. SKU는 판매자나 물류 운영에서 사용하는 별도 업무 코드가 될 수 있지만, 이 프로젝트에서는 CatalogProduct와 ProductVariant를 플랫폼이 관리하고 판매자별 판매 정보는 P9 Offer가 관리한다. 따라서 플랫폼이 UUID 기반 SKU를 추가로 생성하면 `variantId`와 같은 대상을 가리키는 식별자가 중복되고, 별도의 업무 의미나 소비자 기능이 생기지 않는다.

이에 따라 다음을 명시적으로 제외한다.

- SKU 입력·자동 생성·형식 검증·중복 검사를 제공하지 않는다.
- SKU 컬럼, SKU 전용 유일성 제약, SKU 기반 조회 API를 만들지 않는다.
- 구매자·판매자 응답과 주문 스냅샷에 SKU를 포함하지 않는다. 화면에는 상품명, Variant 표시명, attributes 등 표시용 메타데이터를 사용한다.
- SKU 도입은 심화사항으로 남기지 않으며, 향후 확장 대상으로도 관리하지 않는다.

다른 모듈이 활성 Variant를 확인해야 할 때는 P2의 `CatalogVariantQueryApi.findActiveByVariantId(variantId)`를 사용한다. 구매자·판매자용 상품 조회 응답에서는 내부 ID를 노출하지 않고 상품 표시 정보만 반환하며, 관리·모듈 연계에 필요한 경우에만 내부 식별자를 사용한다.

## 3. API 소유권 요약

| 기능 | HTTP 진입점 소유자 | 도메인 규칙 소유자 |
|---|---|---|
| Category 생성·수정 | P7 `ADMIN` API | P2 |
| CatalogProduct·ProductVariant 생성·수정·보관 | P2 `ADMIN` API | P2 |
| Seller Catalog 등록 요청 | P8 | P2 검증 + P7 승인 |
| Seller Catalog 조회 | P9/P2 공개 Catalog 조회 | P2 |
| 고객 상품 검색·상세 | P9 | P9 조합 + P2 Catalog interface |

P2는 Offer·Inventory·Review를 직접 생성하거나 고객용 통합 상품 화면을 조합하지 않는다.

## 4. 공개 application interface

P7·P8·P9는 P2의 domain·infra 내부 패키지를 직접 참조하지 않는다. HTTP 검증 API를 별도로 만들지 않고 Modulith 공개 interface를 사용한다.

```text
CatalogCategoryValidationApi.validate(CategoryProposal)
  → CategoryValidationResult(valid, depth, errors)

CatalogCategoryQueryApi.findSelfAndDescendantIds(categoryId)
  → Set<CategoryId>

CatalogVariantQueryApi.findActiveByVariantId(variantId)
  → CatalogVariantReference
```

내부 참조 객체는 HTTP 응답으로 노출하지 않는다. ID 기반 조회 결과는 다음과 같이 처리한다.

아래의 클라이언트 응답·서버 로그 분리 원칙은 P2만의 규칙이 아니라 모든 도메인에 적용되는 공통 예외 응답 정책이다.

- 요청한 ID가 실제로 존재하지 않으면 역할과 무관하게 해당 리소스의 `404`를 반환한다.
- `ADMIN`이 보관된 CatalogProduct·ProductVariant를 조회하면 존재하는 데이터이므로 `200`으로 반환한다.
- 구매자·Seller가 보관된 CatalogProduct·ProductVariant를 조회하면, 실제로 존재하지 않는 경우와 동일한 `404`를 반환한다.
- 구매자·Seller에 반환하는 응답 메시지는 보관 여부를 포함하지 않는 일반 메시지를 사용한다. 예를 들어 CatalogProduct는 `상품을 찾을 수 없습니다.`, ProductVariant는 `상품 옵션을 찾을 수 없습니다.`로 응답한다.
- 서버 로그에는 클라이언트 응답과 별도로 `lookupResult = ARCHIVED` 또는 `lookupResult = NOT_FOUND`를 기록한다. 로그에는 `requestId`, 역할, 리소스 종류, 내부 ID, 요청 경로, 실패 원인을 포함하되 클라이언트 응답에는 내부 판단 결과를 노출하지 않는다.

즉, 클라이언트는 보관 데이터와 미존재 데이터를 구분하지 못하고 동일한 404 안내를 받지만, 서버 운영자는 로그를 통해 보관된 데이터에 대한 접근인지 실제 미존재인지 구분할 수 있다.

예를 들어 구매자가 보관된 CatalogProduct를 요청한 경우 다음처럼 분리한다.

```text
Client response: 404 CATALOG_PRODUCT_NOT_FOUND / 상품을 찾을 수 없습니다.
Server log: [CatalogProductLookup] result=ARCHIVED, catalogProductId={internal-id}, role=BUYER, requestId={request-id}
```

실제로 ID가 존재하지 않는 경우에는 Client response 형식은 동일하게 유지하고, 서버 로그만 `result=NOT_FOUND`로 기록한다.

## 5. 역할별 보관 데이터 조회

- 구매자·Seller의 공개 목록·조건 검색 결과에는 보관된 CatalogProduct·ProductVariant를 포함하지 않는다.
- 구매자·Seller가 보관된 CatalogProduct를 ID로 조회하면 `404 CATALOG_PRODUCT_NOT_FOUND`다.
- 구매자·Seller가 보관된 ProductVariant를 ID로 조회하면 `404 VARIANT_NOT_FOUND`다.
- `ADMIN`은 존재하는 CatalogProduct·ProductVariant의 `ACTIVE`·`ARCHIVED` 상태를 모두 조회할 수 있다.
- `ADMIN`도 실제로 존재하지 않는 리소스를 조회하면 해당 `404`를 받는다.
- 구매자·Seller 응답에는 `catalogProductId`, `variantId`를 반환하지 않고 상품 표시 메타데이터만 반환한다. 관리자 응답에는 운영에 필요한 내부 ID와 상태를 포함할 수 있다.

## 6. 심화사항

현재 범위에는 검색용 내부 키워드와 상품 유형 체계를 포함하지 않는다. 관련 개념과 향후 방향은 다음 문서에서 정의한다.

- [SearchKeyword 심화사항](p2-search-keyword.md)
- [ProductType·ItemType 심화사항](p2-product-type.md)

## 7. 공통 예외

| HTTP | 코드 | 의미 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 공통 요청 검증 실패 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요함 |
| 403 | `ACCESS_DENIED` | Catalog 관리 권한 부족 |
| 404 | `CATEGORY_NOT_FOUND` | Category가 존재하지 않음 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | CatalogProduct가 존재하지 않거나 비관리자가 보관 상품을 조회함 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant가 존재하지 않거나 비관리자가 보관 Variant를 조회함 |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관된 CatalogProduct 변경 시도 |
| 409 | `PRODUCT_VARIANT_ARCHIVED` | 보관된 ProductVariant 변경 시도 |
