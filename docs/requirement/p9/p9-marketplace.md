# P9 Marketplace API

이 문서는 P2 Catalog의 상품 정보와 P9 Offer·Inventory, P10 Review 요약을 조합하는 고객용 검색·상품 상세 API를 정의한다. 업무 정책은 [P9 Policy](p9-policy.md), Offer·Inventory 모델은 [P9 Offer API](p9-offer.md)와 [P9 Inventory API](p9-inventory.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `MarketplaceProductView` | 고객에게 표시할 CatalogProduct 단위 검색·상세 결과 | 검색·상품 상세 |
| `MarketplaceVariantView` | Variant 표시 정보와 공개 Offer 목록 | 검색·상품 상세 |
| `MarketplaceOfferView` | 공개 가능한 Seller Offer와 가격·재고 상태 | 상품 검색·상세 |
| `ReviewSummary` | 상품 조회에 표시할 평점 요약 | P10 Review 공개 계약 |

- 조회 모델은 P2·P9·P10 원본을 조합한 결과이며 별도 원본 aggregate가 아니다.
- 고객 응답에는 내부 `catalogProductId`, `variantId`를 포함하지 않는다.
- 비활성·보관 Offer, 보관 CatalogProduct·ProductVariant, 관리자 감사 정보, 정확한 재고 수량은 고객 응답에서 제외한다.

## 2. 데이터 모델

### 2-1. MarketplaceProductView

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `name` | String | 예 | 상품명 |
| `description` | String | 상세만 | 상품 설명 |
| `brand` | String | 아니오 | 브랜드 |
| `categoryPath` | CategoryPath[] | 예 | P2 Category 경로 |
| `thumbnailUrl` | URI | 검색만 | 대표 Thumbnail |
| `rating` | RatingSummary | 검색만 | Review 요약 |
| `variants` | MarketplaceVariantView[] | 예 | 공개 가능한 Variant와 Offer |
| `reviewSummary` | RatingSummary | 상세만 | 상품 상세 Review 요약 |

### 2-2. MarketplaceVariantView와 MarketplaceOfferView

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `displayName` | String | 예 | Variant 표시명 |
| `attributes` | JSON Object | 예 | Variant 동적 속성 |
| `bestOffer` | MarketplaceOfferView | 검색만 | 공개 Offer 중 대표 Offer |
| `offers` | MarketplaceOfferView[] | 상세만 | 공개 가능한 Offer 목록 |

`MarketplaceOfferView`는 `offerId`, `sellerId`, `sellerName`, `currentPrice`, `status`, `availabilityStatus`, 대표 Media를 포함할 수 있다. `currentPrice`는 Offer의 현재 적용 가격이며 Inventory 수량 자체는 노출하지 않는다.

### 2-3. 관계와 제약

- 검색 결과는 ACTIVE CatalogProduct 단위다.
- ACTIVE ProductVariant와 공개 가능한 ACTIVE Offer만 포함한다.
- `bestOffer`는 재고가 있는 Offer를 우선하고, 적용 가격이 낮은 순서, 가격이 같으면 `offerId` 오름차순으로 선택한다.
- `categoryId`는 P2의 공개 Category 조회 계약으로 자기 자신과 하위 Category를 해석한다.
- 검색 커서는 CatalogProduct 단위이며 검색어·필터·정렬 조건과 함께 검증한다.
- 고객에게는 `IN_STOCK` 또는 `OUT_OF_STOCK`만 반환한다.

## 3. API 정의

### 3-1. 상품 검색

`GET /api/v1/product/search`

권한: 공개.

Query:

```text
cursor=opaque-cursor (첫 조회 생략)
size=20
keyword=headphone
categoryId=uuid
minPrice=10000
maxPrice=100000
minRating=4
availability=IN_STOCK|OUT_OF_STOCK
sort=RELEVANCE|LATEST|PRICE_ASC|PRICE_DESC|RATING_DESC
```

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "name": "무선 헤드폰",
      "brand": "Example Brand",
      "categoryPath": [{ "categoryId": "uuid", "name": "전자기기" }],
      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
      "rating": { "average": 4.5, "count": 120 },
      "variants": [
        {
          "displayName": "블랙",
          "attributes": { "color": "Black" },
          "bestOffer": {
            "offerId": "uuid",
            "sellerId": "uuid",
            "sellerName": "Example Store",
            "currentPrice": { "amount": 49900.00, "currency": "KRW" },
            "availabilityStatus": "IN_STOCK"
          }
        }
      ]
    }
  ],
  "nextCursor": "opaque-cursor",
  "hasNext": false
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `MARKETPLACE-001` | 가격·평점·페이지 필터 형식 오류 | 검색 조건을 확인해 주세요. | 실패 query field | Marketplace search query 검증 실패 |
| 400 | `MARKETPLACE-002` | 커서 서명·필터·정렬 검증 실패 | 검색 페이지를 다시 요청해 주세요. | 없음 | Marketplace cursor 검증 실패 |
| 404 | [CATALOG-003](../p2/p2-category.md) | — | — | — | — |

유효한 검색 조건에 상품이 없는 경우 `200 OK`와 빈 `data`를 반환한다.

### 3-2. 고객용 상품 상세

`GET /api/v1/product/{catalogProductId}`

권한: 공개.

보관되지 않은 CatalogProduct와 ProductVariant, 활성 Seller의 공개 Offer, 재고 표시 상태, Review 요약을 조합한다. 보관된 ProductVariant는 상세 결과에서 제외한다.

#### 성공 응답: `200 OK`

```json
{
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "categoryPath": [{ "categoryId": "uuid", "name": "전자기기" }],
  "variants": [
    {
      "displayName": "블랙",
      "attributes": { "color": "Black" },
      "offers": [
        {
          "offerId": "uuid",
          "sellerId": "uuid",
          "sellerName": "Example Store",
          "currentPrice": { "amount": 49900.00, "currency": "KRW" },
          "status": "ACTIVE",
          "availabilityStatus": "IN_STOCK",
          "media": [
            { "type": "IMAGE", "url": "https://cdn.example.com/seller-offer-main.jpg", "isPrimary": true }
          ]
        }
      ]
    }
  ],
  "reviewSummary": { "averageRating": 4.5, "reviewCount": 120 }
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | [CATALOG-019](../p2/p2-catalog-product.md) | — | — | — | — |
| 404 | [CATALOG-031](../p2/p2-product-variant.md) | — | — | — | — |

보관 여부와 실제 미존재 여부를 구분하는 별도 고객용 오류 코드는 반환하지 않는다.
