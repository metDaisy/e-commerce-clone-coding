# P9 Offer & Marketplace (판매 조건·마켓플레이스)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 책임

P9는 Seller가 CatalogProduct의 ProductVariant를 판매하기 위한 `Offer`와 `Inventory`를 소유한다. CatalogProduct·ProductVariant의 메타데이터와 SKU는 [P2 Catalog](p2-catalog.md)가 소유하며, P9는 P2의 공개 Catalog interface로 Variant의 존재·보관 상태를 확인한다.

- `PRODUCT_MANAGER`는 `ACTIVE Seller`를 가진 User이며 자신의 Offer·Inventory만 관리한다.
- `ADMIN`은 모든 Offer·Inventory를 운영할 수 있고 Offer를 활성·비활성할 수 있다.
- 하나의 Seller는 같은 ProductVariant에 Offer를 하나만 등록한다.
- Offer 생성 시 Inventory를 함께 생성하고 초기 수량은 `0`이다.
- Offer 삭제는 물리 삭제가 아닌 `status = ARCHIVED`이며 다시 활성화할 수 없다.
- CatalogProduct 또는 ProductVariant가 보관되면 연결된 Offer는 `INACTIVE`가 되고 공개 검색·구매 대상에서 제외된다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/seller/catalog-products` | `PRODUCT_MANAGER` + `ACTIVE` | Offer 등록을 위한 CatalogProduct·Variant 페이지 조회 |
| GET | `/api/v1/product/search` | 공개 | 고객용 통합 상품 검색 |
| GET | `/api/v1/product/{catalogProductId}` | 공개 | 고객용 통합 상품 상세 |
| GET | `/api/v1/seller/offers` | `PRODUCT_MANAGER` + `ACTIVE` | 내 Offer 목록 |
| POST | `/api/v1/seller/offers` | `PRODUCT_MANAGER` + `ACTIVE` | Offer 등록 |
| PATCH | `/api/v1/seller/offers/{offerId}` | Offer 소유 `PRODUCT_MANAGER` | 내 Offer 활성·비활성 |
| DELETE | `/api/v1/seller/offers/{offerId}` | Offer 소유 `PRODUCT_MANAGER` | 내 Offer 보관 |
| PATCH | `/api/v1/seller/offers/{offerId}/price` | Offer 소유 `PRODUCT_MANAGER` | 내 Offer 가격 수정 |
| PATCH | `/api/v1/admin/offers/{offerId}/price` | `ADMIN` | Offer 가격 운영 수정 |
| POST | `/api/v1/seller/offers/{offerId}/inventory-adjustments` | Offer 소유 `PRODUCT_MANAGER` | 내 Offer 재고 조정 |
| POST | `/api/v1/admin/offers/{offerId}/inventory-adjustments` | `ADMIN` | Offer 재고 운영 조정 |

ADMIN의 Offer 활성·비활성 진입점은 [P7 관리자 요구사항](p7-admin.md)의 `/api/v1/admin/offers/{offerId}/status`를 사용한다.

## 3. 판매자 CatalogProduct 조회

`GET /api/v1/seller/catalog-products`

Offer 등록을 위한 페이지 기반 조회다. 보관되지 않은 CatalogProduct와 ProductVariant만 반환하고, 경쟁 판매자의 Offer·가격·재고는 반환하지 않는다. 이미 등록한 경우에도 인증된 Seller의 `myOffer`만 포함한다.

지원 Query:

```text
page=0
size=20
keyword=headphone (상품명·설명·브랜드·SKU·Variant 표시명)
categoryId=uuid
sort=LATEST|NAME_ASC|NAME_DESC
```

응답 `200`:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "data": [
    {
      "catalogProductId": "uuid",
      "name": "무선 헤드폰",
      "description": "카탈로그 상품 설명",
      "brand": "Example Brand",
      "category": { "categoryId": "uuid", "name": "헤드폰" },
      "variants": [
        {
          "variantId": "uuid",
          "sku": "HEADPHONE-BLK-001",
          "displayName": "블랙",
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

`myOffer`가 없으면 `null`이다. `inventoryQuantity`와 Offer 상태는 자신의 Offer에 대해서만 반환한다.

## 4. Offer 관리

### 4-1. Offer 등록

`POST /api/v1/seller/offers`

요청:

```json
{
  "variantId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" }
}
```

- 활성 Seller만 보관되지 않은 ProductVariant에 등록할 수 있다.
- `sellerId`는 요청 본문으로 받지 않고 인증된 Seller에서 결정한다.
- Offer 등록과 동시에 Inventory를 `quantity = 0`으로 생성한다.
- 같은 Seller가 같은 ProductVariant에 다시 등록하면 `409 SELLER_OFFER_ALREADY_EXISTS`다.

성공 응답 `201`:

```json
{
  "offerId": "uuid",
  "variantId": "uuid",
  "sellerId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "inventoryQuantity": 0,
  "status": "ACTIVE",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 4-2. Offer 상태·보관

`PATCH /api/v1/seller/offers/{offerId}` 요청:

```json
{ "status": "INACTIVE" }
```

`ACTIVE` 전환은 Seller, CatalogProduct, ProductVariant가 모두 활성인 경우에만 허용한다. 성공 응답은 `offerId`, `variantId`, `sellerId`, `status`, `updatedAt`을 반환한다.

`DELETE /api/v1/seller/offers/{offerId}`는 다음과 같이 보관 처리한다.

```json
{
  "offerId": "uuid",
  "status": "ARCHIVED",
  "archivedAt": "2026-08-10T12:05:00Z",
  "updatedAt": "2026-08-10T12:05:00Z"
}
```

### 4-3. Offer 가격

판매자 요청 URI는 `PATCH /api/v1/seller/offers/{offerId}/price`이고, 관리자 요청 URI는 `PATCH /api/v1/admin/offers/{offerId}/price`다. 두 URI는 같은 요청·응답 계약을 사용한다.

요청:

```json
{
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": { "amount": 44900.00, "currency": "KRW" },
  "discountStartAt": "2026-08-10T00:00:00Z",
  "discountEndAt": "2026-08-31T23:59:59Z"
}
```

- 전달된 가격 필드만 수정한다.
- 가격은 0 이상이어야 하며 할인 가격은 기본 가격보다 작아야 한다.
- 할인 가격이 `null`이면 할인 기간도 제거한다.
- 통화는 기존 Offer의 통화와 달라질 수 없다.

성공 응답 `200`:

```json
{
  "offerId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": { "amount": 44900.00, "currency": "KRW" },
  "appliedPrice": { "amount": 44900.00, "currency": "KRW" },
  "discountRate": 10.02,
  "updatedAt": "2026-08-10T12:00:00Z"
}
```

## 5. Inventory

Inventory는 Offer 생성 시 자동 생성하며 별도의 생성·삭제 API를 두지 않는다. Offer 소유 Seller와 ADMIN만 조정할 수 있고, 보관된 Offer의 Inventory는 조정할 수 없다.

판매자 요청 URI는 `POST /api/v1/seller/offers/{offerId}/inventory-adjustments`이고, 관리자 요청 URI는 `POST /api/v1/admin/offers/{offerId}/inventory-adjustments`다. 두 URI는 같은 동시성·수량 계약을 사용한다.

요청:

```json
{
  "quantityDelta": 20,
  "reason": "RESTOCK"
}
```

응답 `200`:

```json
{
  "offerId": "uuid",
  "previousQuantity": 0,
  "currentQuantity": 20,
  "availabilityStatus": "IN_STOCK",
  "reason": "RESTOCK",
  "adjustedBy": "uuid",
  "adjustedAt": "2026-08-10T12:10:00Z"
}
```

재고는 0 미만이 될 수 없으며 동시성 제어를 적용한다. 결제 완료 시 차감하고 주문 취소 또는 Saga 보상 시 복원한다. 고객 응답에는 정확한 수량 대신 `IN_STOCK` 또는 `OUT_OF_STOCK`만 반환한다.

## 6. 고객용 상품 검색·상세

### 6-1. 상품 검색

`GET /api/v1/product/search`

CatalogProduct·ProductVariant·Offer·Inventory·Review를 조합한 고객용 커서 기반 조회다. 결과는 CatalogProduct 단위이며 보관되지 않은 Variant와 공개 가능한 대표 Offer만 포함한다. 정확한 재고 수량, 비활성·보관 Offer, 관리자 감사 정보는 반환하지 않는다.

지원 Query:

```text
cursor=opaque-cursor (첫 조회 생략)
size=20
keyword=headphone
categoryId=uuid
minPrice=10000
maxPrice=100000
minRating=4
availability=IN_STOCK|OUT_OF_STOCK
tag=wireless
sort=RELEVANCE|LATEST|PRICE_ASC|PRICE_DESC|RATING_DESC
```

응답:

```json
{
  "data": [
    {
      "catalogProductId": "uuid",
      "name": "무선 헤드폰",
      "brand": "Example Brand",
      "category": { "categoryId": "uuid", "name": "헤드폰" },
      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
      "rating": { "average": 4.5, "count": 120 },
      "variants": [
        {
          "variantId": "uuid",
          "sku": "HEADPHONE-BLK-001",
          "displayName": "블랙",
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

`bestOffer`는 `ACTIVE` Offer 중 재고가 있는 Offer와 적용 가격이 가장 낮은 Offer를 우선한다. 가격이 같으면 `offerId` 오름차순을 사용한다. 커서는 CatalogProduct 단위이며 검색어·필터·정렬 조건과 함께 검증한다.

### 6-2. 고객용 상품 상세

`GET /api/v1/product/{catalogProductId}`

보관되지 않은 CatalogProduct와 ProductVariant, 활성 Seller의 공개 Offer, 재고 표시 상태, Review 요약을 조합해 반환한다. 상세 응답에는 정확한 Inventory 수량과 감사 이력을 포함하지 않는다.

응답 `200`:

```json
{
  "catalogProductId": "uuid",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "category": { "categoryId": "uuid", "name": "헤드폰" },
  "variants": [
    {
      "variantId": "uuid",
      "sku": "HEADPHONE-BLK-001",
      "displayName": "블랙",
      "offers": [
        {
          "offerId": "uuid",
          "sellerId": "uuid",
          "sellerName": "Example Store",
          "currentPrice": { "amount": 49900.00, "currency": "KRW" },
          "status": "ACTIVE",
          "availabilityStatus": "IN_STOCK"
        }
      ],
      "media": [
        { "type": "IMAGE", "url": "https://cdn.example.com/main.jpg", "isPrimary": true }
      ]
    }
  ],
  "reviewSummary": { "averageRating": 4.5, "reviewCount": 120 }
}
```

## 7. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Offer·Inventory 요청 필드 오류 |
| 400 | `INVALID_PRICE` | 가격이 음수이거나 할인 가격이 기본 가격 이상 |
| 400 | `INVALID_STOCK` | 재고가 음수 |
| 400 | `INVALID_CURSOR` | 커서 서명·필터·정렬 검증 실패 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 활성 Seller 없는 요청 |
| 403 | `SELLER_RESOURCE_ACCESS_DENIED` | 다른 Seller의 Offer·Inventory 접근 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant 없음 |
| 404 | `OFFER_NOT_FOUND` | Offer 없음 |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관된 CatalogProduct에 Offer 등록·활성화 |
| 409 | `PRODUCT_VARIANT_ARCHIVED` | 보관된 ProductVariant에 Offer 등록·활성화 |
| 409 | `OFFER_ARCHIVED` | 보관된 Offer 수정·활성화 |
| 409 | `SELLER_OFFER_ALREADY_EXISTS` | 같은 Seller의 ProductVariant Offer 중복 |
| 409 | `INSUFFICIENT_STOCK` | 주문 수량이 재고보다 많음 |
