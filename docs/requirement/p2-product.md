# P2 Catalog & Inventory (상품·카탈로그·재고)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 모델

```text
CatalogProduct (상품군/전시 상품)
  └─ ProductVariant (실제 구매 단위: SKU)
       └─ Offer (판매 조건과 가격)
            └─ Inventory (구매 가능 수량)
```

- `CatalogProduct`는 고객에게 하나의 상품 상세 페이지로 보이는 상품군이다. 예를 들어 `무선 헤드폰`이라는 이름·설명·브랜드·카테고리를 가진다.
- `ProductVariant`는 고객이 실제로 선택하고 주문하는 개별 옵션 조합이다. 예를 들어 같은 `무선 헤드폰`의 `블랙/대형`과 `화이트/소형`은 서로 다른 Variant다.
- Variant는 반드시 하나의 `sku`를 가지며, SKU는 주문·장바구니·재고 차감에서 사용하는 실제 판매 단위 식별자다.
- 색상·사이즈처럼 선택 가능한 값이 없는 단일 상품도 Variant를 생략하지 않는다. 이 경우 `displayName`을 `기본 옵션`으로 정하고 Variant 1개를 생성한다.
- `Offer`는 Variant를 어떤 가격·판매 상태·상품 상태·판매자 조건으로 판매하는지를 나타낸다. 하나의 Variant에 여러 Offer를 둘 수 있는 구조지만, 요구사항에서는 기본 Offer 1개만 생성한다.
- `Inventory`는 Offer의 구매 가능 수량이다. 따라서 재고는 CatalogProduct 전체가 아니라 구매 가능한 판매 조건 단위로 차감한다.
- 현재 요구사항은 다중 옵션 선택 UI를 구현하지 않는다. 상품 등록 요청에는 `variant` 객체 하나만 받고, 심화사항에서 여러 Variant와 옵션 조합으로 확장한다.

예시:

| 계층 | 예시 | 의미 |
|---|---|---|
| CatalogProduct | 무선 헤드폰 | 상품 상세 페이지와 공통 상품 정보 |
| ProductVariant | 블랙 / 대형, SKU `HEADPHONE-BLK-L-001` | 실제 주문·배송되는 옵션 조합 |
| Offer | 49,900원, ACTIVE | 해당 Variant의 판매 가격과 판매 조건 |
| Inventory | 100개 | 해당 Offer의 구매 가능 수량 |

- 상품 등록자는 판매자 역할인 `PRODUCT_MANAGER` 또는 플랫폼 운영 역할인 `ADMIN`이다.
- 요구사항에서는 단일 플랫폼 판매자, 기본 Variant 1개, 기본 Offer 1개만 생성한다.
- 판매자 신청·승인과 판매자 Offer 관리는 P8에서 정의한다. `PRODUCT_MANAGER`는 자신의 상품·Offer·재고를 관리하고, `ADMIN`은 플랫폼 운영 목적으로 동일 기능을 수행할 수 있다.
- `productId`, `variantId`, `offerId`는 서로 다른 서버 생성 UUID다.
- `products.manager_id`는 상품을 등록·관리한 `users.id`다. `PRODUCT_MANAGER`는 자신이 등록한 상품만 수정·보관할 수 있고, `ADMIN`은 모든 상품을 관리할 수 있다.
- `sku`는 구매 단위의 고유 식별자다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 외부 식별자로 선택 저장하며 내부 PK로 사용하지 않는다.
- 상품 등록 요청에는 `sellerId`를 받지 않는다. `PRODUCT_MANAGER`가 등록하면 인증된 `SellerProfile`을 Offer 소유자로 사용하고, `ADMIN`이 등록하면 플랫폼 기본 Offer로 생성한다.
- `offers.seller_id`는 Offer를 소유한 `seller_profiles.id`이며, `ADMIN`이 생성한 플랫폼 기본 Offer는 `NULL`이다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/categories` | 공개 | 카테고리 트리 조회 |
| GET | `/api/v1/products` | 공개 | 상품 목록·검색 |
| POST | `/api/v1/products` | PRODUCT_MANAGER, ADMIN | 상품 등록 |
| GET | `/api/v1/products/{productId}` | 공개 | 상품 상세 |
| PATCH | `/api/v1/products/{productId}` | PRODUCT_MANAGER, ADMIN | 상품 수정 |
| DELETE | `/api/v1/products/{productId}` | PRODUCT_MANAGER, ADMIN | 상품 보관 |
| POST | `/api/v1/offers/{offerId}/inventory-adjustments` | PRODUCT_MANAGER, ADMIN | 재고 조정 |
| GET | `/api/v1/products/{productId}/reviews` | 공개 | 리뷰 목록 |
| POST | `/api/v1/products/{productId}/reviews` | 로그인 | 리뷰 작성 |
| PATCH | `/api/v1/reviews/{reviewId}` | 작성자 | 리뷰 수정 |
| DELETE | `/api/v1/reviews/{reviewId}` | 작성자, ADMIN | 리뷰 삭제 |

## 3. 요구사항

### 3-1. 카테고리

- `parent_id` 자기참조 FK로 계층을 관리한다.
- `depth`는 루트를 1로 하는 계층 깊이이며, 자식은 부모 `depth + 1`로 저장한다.
- 현재 구현 범위는 최대 3단계이며 루트는 `parent_id = NULL`, `depth = 1`이다.
- `depth`는 `1~3`만 허용하고, 카테고리 생성·이동 시 부모와의 깊이 관계를 같은 트랜잭션에서 검증한다.
- `GET /api/v1/categories`는 전체 트리를 다음 형태로 반환한다.

```json
{
  "items": [
    {
      "categoryId": "uuid",
      "name": "전자기기",
      "parentId": null,
      "children": []
    }
  ]
}
```

- 카테고리별 상품 조회는 해당 카테고리와 모든 하위 카테고리를 포함한다.
- 하위 카테고리 또는 상품이 연결된 카테고리는 삭제할 수 없다.
- 삭제 충돌은 `CATEGORY_HAS_CHILDREN` (409)이다.

카테고리 생성 요청:

```json
{
  "name": "노트북",
  "parentId": "uuid"
}
```

성공 응답 `201`:

```json
{
  "categoryId": "uuid",
  "name": "노트북",
  "parentId": "uuid",
  "depth": 3,
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 상품 등록

`POST /api/v1/products`

요청:

```json
{
  "categoryId": "uuid",
  "name": "무선 헤드폰",
  "description": "상품 상세 설명",
  "brand": "Example Brand",
  "tags": ["무선", "블루투스"],
  "attributes": { "connectionType": "Bluetooth", "color": "Black" },
  "variant": {
    "sku": "HEADPHONE-BLK-001",
    "displayName": "블랙",
    "weight": 0.25,
    "dimensions": { "width": 18, "height": 20, "depth": 8, "unit": "cm" }
  },
  "offer": {
    "price": { "amount": 49900.00, "currency": "KRW" }
  },
  "inventory": { "initialQuantity": 100 },
  "media": [
    {
      "type": "IMAGE",
      "url": "https://cdn.example.com/headphone-main.jpg",
      "isPrimary": true,
      "sortOrder": 0
    }
  ]
}
```

필수 입력:

- `categoryId`
- `name`, `description`
- `variant.sku`, `variant.displayName`
- `offer.price.amount`, `offer.price.currency`
- `inventory.initialQuantity`
- `media`의 대표 이미지 1장

선택 입력:

- `brand`, `tags`, `attributes`, `weight`, `dimensions`

등록 요청의 `variant`는 상품의 선택 옵션이 아니라 실제 구매 단위를 표현한다. 그러므로 `variant.sku`와 `variant.displayName`은 필수이며, 옵션이 없는 상품도 다음처럼 입력한다.

```json
{
  "variant": {
    "sku": "MUG-BASIC-001",
    "displayName": "기본 옵션"
  }
}
```

처리 규칙:

1. 카테고리가 존재하고 삭제되지 않았는지 확인한다.
2. SKU가 중복되지 않았는지 확인한다.
3. 가격은 0 이상, 재고는 0 이상이어야 한다.
4. 상품·Variant·Offer·가격·재고를 하나의 트랜잭션으로 생성한다.
5. `PRODUCT_MANAGER`가 등록한 상품에는 해당 `SellerProfile`의 Offer를 생성하고, `ADMIN`이 등록한 상품에는 플랫폼 기본 Offer를 생성한다.
6. 재고가 0이면 구매 가능 상태를 `OUT_OF_STOCK`으로 계산한다.

성공 응답 `201`:

```json
{
  "productId": "uuid",
  "variantId": "uuid",
  "offerId": "uuid",
  "sku": "HEADPHONE-BLK-001",
  "name": "무선 헤드폰",
  "publicationStatus": "ACTIVE",
  "currentPrice": { "amount": 49900.00, "currency": "KRW" },
  "availabilityStatus": "IN_STOCK",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 심화 사항

- 다중 Variant와 variation theme을 지원한다.
- Variant별 이미지·가격·재고를 지원한다.
- 다중 판매자 Offer, 상품 상태, 출고자, 배송 조건, 반품 정책을 지원한다.
- 대표 Offer 선택과 Buy Box를 지원한다.

### 3-3. 상품 수정·보관

- `PATCH /api/v1/products/{productId}`는 전달된 필드만 수정한다.
- 상품명·설명·브랜드·태그·속성·기본 가격은 수정할 수 있다.
- SKU는 주문 이력이 있으면 변경할 수 없다.
- `DELETE`는 물리 삭제하지 않고 `ARCHIVED`로 변경한다.
- 보관 상품은 공개 목록에서 제외한다.
- 주문 상세에는 주문 당시 상품명·SKU·가격 스냅샷을 표시한다.

성공 응답:

```json
{
  "productId": "uuid",
  "publicationStatus": "ACTIVE",
  "updatedAt": "2026-08-09T12:05:00Z"
}
```

### 3-4. 상품 목록·검색

`GET /api/v1/products`

지원 Query:

```text
cursor=opaque-cursor (첫 조회 생략)
size=20
keyword=headphone
categoryId=uuid
minPrice=10000
maxPrice=100000
minRating=4
availability=IN_STOCK
tag=wireless
sort=RELEVANCE|LATEST|PRICE_ASC|PRICE_DESC|RATING_DESC
```

응답:

```json
{
  "items": [
    {
      "productId": "uuid",
      "variantId": "uuid",
      "offerId": "uuid",
      "name": "무선 헤드폰",
      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
      "currentPrice": { "amount": 49900.00, "currency": "KRW" },
      "originalPrice": { "amount": 59900.00, "currency": "KRW" },
      "discountRate": 16.7,
      "rating": { "average": 4.5, "count": 120 },
      "availabilityStatus": "IN_STOCK"
    }
  ],
  "nextCursor": "opaque-cursor",
  "hasNext": false
}
```

- 기본 정렬은 `RELEVANCE`이며, 같은 정렬값은 `productId`를 보조 키로 사용한다.
- `LATEST`는 `createdAt DESC, productId DESC`, 가격·평점 정렬도 동일한 보조 키를 사용한다.
- `cursor`에는 검색어·필터·정렬 조건이 포함되므로 조건을 변경한 요청에 기존 cursor를 재사용할 수 없다.
- 키워드는 상품명·설명·브랜드를 대상으로 검색한다.
- 카테고리 필터는 하위 카테고리를 포함한다.
- 품절 상품은 `availability=IN_STOCK`일 때 제외한다.
- 요구사항에서는 LIKE 검색을 허용하되, 검색 결과의 응답 구조는 전문 검색 엔진으로 교체해도 유지한다.

#### 심화 사항

- 카테고리별 동적 Facet, 자동완성, 오타 보정, 전문 검색을 지원한다.

### 3-5. 상품 상세

`GET /api/v1/products/{productId}`

응답:

```json
{
  "productId": "uuid",
  "name": "무선 헤드폰",
  "description": "상품 상세 설명",
  "brand": "Example Brand",
  "category": { "categoryId": "uuid", "name": "헤드폰" },
  "variants": [
    {
      "variantId": "uuid",
      "sku": "HEADPHONE-BLK-001",
      "displayName": "블랙",
      "selected": true,
      "offer": {
        "offerId": "uuid",
        "price": { "amount": 49900.00, "currency": "KRW" },
        "originalPrice": { "amount": 59900.00, "currency": "KRW" },
        "discountRate": 16.7
      },
      "availabilityStatus": "IN_STOCK",
      "media": [
        { "type": "IMAGE", "url": "https://cdn.example.com/main.jpg", "isPrimary": true }
      ]
    }
  ],
  "reviewSummary": { "averageRating": 4.5, "reviewCount": 120 }
}
```

### 3-6. 가격

- 가격은 Offer에 연결된 가격 정보가 소유한다.
- 기본 가격과 기간성 할인 0~1개를 지원한다.
- 할인 가격은 기본 가격보다 작아야 한다.
- 시작 시각은 종료 시각보다 이전이어야 한다.
- API는 기본 가격, 적용 가격, 할인율을 구분한다.
- 가격 변경 권한은 `PRODUCT_MANAGER`, `ADMIN`이다.

#### 심화 사항

- 쿠폰, 회원 가격, 수량 할인, 복수 프로모션과 가격 이력을 지원한다.

### 3-7. 재고

`POST /api/v1/offers/{offerId}/inventory-adjustments`

요청:

```json
{
  "quantityDelta": 20,
  "reason": "RESTOCK"
}
```

응답:

```json
{
  "offerId": "uuid",
  "previousQuantity": 0,
  "currentQuantity": 20,
  "availabilityStatus": "IN_STOCK",
  "reason": "RESTOCK"
}
```

- 결제 완료(`PAID`) 시 재고를 차감한다.
- 주문 취소 또는 Saga 보상 시 차감 수량을 복원한다.
- 동시성 제어로 수량이 음수가 되지 않도록 한다.
- 사용자 응답에는 정확한 수량 대신 `IN_STOCK`, `OUT_OF_STOCK`를 반환한다.
- 고객 상품 페이지의 실시간 WebSocket은 요구사항에 포함하지 않는다.

#### 심화 사항

- 재고 예약, 예약 만료, 안전재고, 다중 창고, 백오더를 지원한다.

### 3-8. 리뷰

`GET /api/v1/products/{productId}/reviews`

응답:

```json
{
  "summary": { "averageRating": 4.5, "reviewCount": 120 },
  "items": [
    {
      "reviewId": "uuid",
      "userName": "홍**",
      "rating": 5,
      "content": "좋은 상품입니다.",
      "images": [],
      "createdAt": "2026-08-09T12:00:00Z"
    }
  ],
  "nextCursor": "opaque-cursor",
  "hasNext": true
}
```

- 리뷰 목록은 `createdAt DESC, reviewId DESC` 순서의 커서 기반 조회다.
- `cursor`와 `page`를 함께 보내면 `PAGINATION_PARAMETER_CONFLICT`를 반환한다.

`POST /api/v1/products/{productId}/reviews` 요청:

```json
{
  "rating": 5,
  "content": "좋은 상품입니다.",
  "imageUrls": []
}
```

- 배송 완료(`DELIVERED`)된 구매자만 작성한다.
- 사용자당 하나의 CatalogProduct에 하나의 리뷰만 허용한다.
- 평점은 1~5 정수, 본문은 최대 2000자, 이미지는 최대 5장이다.

#### 심화 사항

- Variant별 리뷰, 인증 구매, 고객 이미지·동영상, 도움됨 투표와 리뷰 검수를 지원한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 검증 실패 |
| 400 | `INVALID_PRICE` | 가격이 음수이거나 할인 가격이 기본 가격 이상 |
| 400 | `INVALID_STOCK` | 재고가 음수 |
| 400 | `INVALID_DISCOUNT_PERIOD` | 할인 기간 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | 상품 관리 권한 부족 |
| 403 | `REVIEW_NOT_ELIGIBLE` | 구매·배송 완료 조건 미충족 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `PRODUCT_NOT_FOUND` | 상품 없음 |
| 404 | `VARIANT_NOT_FOUND` | Variant 없음 |
| 404 | `OFFER_NOT_FOUND` | Offer 없음 |
| 409 | `SKU_ALREADY_EXISTS` | SKU 중복 |
| 409 | `CATEGORY_HAS_CHILDREN` | 하위 카테고리 또는 상품 존재 |
| 409 | `INSUFFICIENT_STOCK` | 주문 수량이 재고보다 많음 |
| 409 | `REVIEW_ALREADY_EXISTS` | 이미 리뷰 작성 |
| 409 | `PRODUCT_ARCHIVED` | 보관 상품 변경 시도 |
