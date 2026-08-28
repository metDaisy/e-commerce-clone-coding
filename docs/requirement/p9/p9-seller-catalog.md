# P9 Seller Catalog API

이 문서는 Seller가 Offer 등록 대상을 찾기 위한 CatalogProduct·ProductVariant 조회 API를 정의한다. 업무 정책은 [P9 Policy](p9-policy.md), Catalog 원본과 공개 조회 계약은 [P2 Catalog](../p2/p2-catalog.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `SellerCatalogView` | Offer 등록에 필요한 CatalogProduct·Variant 표시 정보와 본인 Offer 요약 | Catalog 페이지 조회 |
| `CatalogProduct`, `ProductVariant`, `Category` | 원본 상품·분류 정보 | P2 공개 Catalog 계약 |
| `myOffer` | 현재 Seller가 해당 Variant에 등록한 Offer 요약 | P9 Offer 조회·생성 계약 |

- `SellerCatalogView`는 P9 조회 모델이며 P2 Catalog 원본을 대체하지 않는다.
- 경쟁 Seller의 Offer·가격·재고는 포함하지 않는다.
- `myOffer`의 가격·상태·재고 필드는 본인 Offer에 대해서만 포함한다.

## 2. 데이터 모델

### 2-1. SellerCatalogView

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `name` | String | 예 | CatalogProduct 상품명 |
| `description` | String | 예 | CatalogProduct 설명 |
| `brand` | String | 아니오 | 브랜드 |
| `categoryPath` | CategoryPath[] | 예 | P2 Category 부모 경로 |
| `variants` | SellerCatalogVariant[] | 예 | ACTIVE ProductVariant 목록 |

### 2-2. SellerCatalogVariant와 myOffer

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `displayName` | String | 예 | Variant 표시명 |
| `attributes` | JSON Object | 예 | Variant 동적 속성 |
| `myOffer` | SellerOfferSummary 또는 null | 예 | 현재 Seller의 Offer가 없으면 null |

`myOffer`가 존재하면 `offerId`, `status`, `basePrice`, `appliedPrice`, `inventoryQuantity`, `availabilityStatus`를 포함한다. 공용 Catalog 관리 조회에서는 Offer 등록 대상 선택을 위해 `catalogProductId`, `variantId`를 포함한다.

### 2-3. 관계와 제약

- ACTIVE CatalogProduct와 ACTIVE ProductVariant만 반환한다.
- 보관된 CatalogProduct·ProductVariant는 결과에서 제외한다.
- `keyword`는 상품명·설명·브랜드·Variant 표시명을 대상으로 한다.
- `categoryId`가 있으면 P2의 공개 Category 조회 계약으로 자기 자신과 하위 Category를 해석한다.
- 페이지 기본값은 `page=0`, `size=20`이고 공통 계약의 최대 페이지 크기를 따른다.

## 3. API 정의

### 3-1. Seller CatalogProduct·Variant 조회

`GET /api/v1/catalog-products`

권한: `ADMIN` 또는 `PRODUCT_MANAGER` 권한과 `ACTIVE Seller` 상태를 가진 사용자. 별도의 `/seller` 경로는 사용하지 않는다.

Query:

```text
page=0
size=20
keyword=headphone
categoryId=uuid
tag=office
catalogPublicationStatus=ACTIVE|ARCHIVED
variantPublicationStatus=ACTIVE|ARCHIVED
sort=LATEST|NAME_ASC|NAME_DESC
```

#### 성공 응답: `200 OK`

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "data": [
    {
      "catalogProductId": "uuid-product",
      "name": "무선 헤드폰",
      "description": "카탈로그 상품 설명",
      "brand": "Example Brand",
      "categoryPath": [{ "categoryId": "uuid", "name": "전자기기" }],
      "variants": [
        {
          "variantId": "uuid-variant",
          "displayName": "블랙",
          "attributes": { "color": "Black" },
          "myOffer": {
            "offerId": "uuid",
            "status": "INACTIVE",
            "basePrice": { "amount": 49900.00, "currency": "KRW" },
            "appliedPrice": { "amount": 49900.00, "currency": "KRW" },
            "inventoryQuantity": 0,
            "availabilityStatus": "OUT_OF_STOCK"
          }
        }
      ]
    }
  ]
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-040` | 페이지·정렬·필터 형식 오류 | 검색 조건을 확인해 주세요. | 실패 query field | Seller Catalog query 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | [CATEGORY-003](../p2/p2-category.md) | — | — | — | — |

유효한 Category지만 상품이 없는 조건은 `200 OK`와 빈 `data`를 반환한다.
