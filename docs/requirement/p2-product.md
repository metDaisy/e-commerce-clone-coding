# P2 Catalog & Inventory (상품·카탈로그·재고)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 모델

```text
CatalogProduct (상품군/전시 상품)
  └─ ProductVariant (실제 구매 단위: SKU)
       └─ Offer (판매 조건과 가격)
            └─ Inventory (구매 가능 수량)
```

현재 요구사항과 SQL은 다음 도메인 용어를 기준으로 한다. `CatalogProduct`는 공통 카탈로그
상품이고, `ProductVariant`는 실제 구매 단위이며, 판매자별 판매 등록은 `Offer`로 표현한다.
엔티티 클래스 이름은 이 문서와 SQL을 기준으로 이후 구현한다.

- `CatalogProduct`는 고객에게 하나의 상품 상세 페이지로 보이는 상품군이다. 예를 들어 `무선 헤드폰`이라는 이름·설명·브랜드·카테고리를 가진다.
- `ProductVariant`는 고객이 실제로 선택하고 주문하는 개별 옵션 조합이다. 예를 들어 같은 `무선 헤드폰`의 `블랙/대형`과 `화이트/소형`은 서로 다른 ProductVariant다.
- ProductVariant는 반드시 하나의 `sku`를 가지며, SKU는 주문·장바구니·재고 차감에서 사용하는 실제 판매 단위 식별자다.
- 색상·사이즈처럼 선택 가능한 값이 없는 단일 상품도 ProductVariant를 생략하지 않는다. 이 경우 `displayName`을 `기본 옵션`으로 정하고 ProductVariant 1개를 생성한다.
- `Offer`는 ProductVariant를 어떤 가격·판매 상태·상품 상태·판매자 조건으로 판매하는지를 나타낸다. 하나의 ProductVariant에 여러 Offer를 둘 수 있는 구조지만, 요구사항에서는 기본 Offer 1개만 생성한다.
- `Inventory`는 Offer의 구매 가능 수량이다. 따라서 재고는 CatalogProduct 전체가 아니라 구매 가능한 판매 조건 단위로 차감한다.
- 현재 요구사항은 다중 옵션 선택 UI를 구현하지 않는다. 상품 등록 요청에는 `variant` 객체 하나만 받고, 심화사항에서 여러 ProductVariant와 옵션 조합으로 확장한다.

예시:

| 계층 | 예시 | 의미 |
|---|---|---|
| CatalogProduct | 무선 헤드폰 | 상품 상세 페이지와 공통 상품 정보 |
| ProductVariant | 블랙 / 대형, SKU `HEADPHONE-BLK-L-001` | 실제 주문·배송되는 옵션 조합 |
| Offer | 49,900원, ACTIVE | 해당 ProductVariant의 판매 가격과 판매 조건 |
| Inventory | 100개 | 해당 Offer의 구매 가능 수량 |

- 상품 등록자는 판매자 역할인 `PRODUCT_MANAGER` 또는 플랫폼 운영 역할인 `ADMIN`이다.
- 요구사항에서는 단일 플랫폼 판매자, 기본 ProductVariant 1개, 기본 Offer 1개만 생성한다.
- 판매자 신청·승인과 판매자 Offer 관리는 P8에서 정의한다. `PRODUCT_MANAGER`는 자신의 상품·Offer·재고를 관리하고, `ADMIN`은 플랫폼 운영 목적으로 동일 기능을 수행할 수 있다.
- `catalogProductId`, `variantId`, `offerId`는 서로 다른 서버 생성 UUID다.
- `catalog_products.manager_id`는 상품을 등록·관리한 `users.id`다. `PRODUCT_MANAGER`는 자신이 등록한 상품만 수정·보관할 수 있고, `ADMIN`은 모든 상품을 관리할 수 있다.
- `sku`는 구매 단위의 고유 식별자다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 외부 식별자로 선택 저장하며 내부 PK로 사용하지 않는다.
- 상품 등록 요청에는 `sellerId`를 받지 않는다. `PRODUCT_MANAGER`가 등록하면 인증된 `SellerProfile`을 Offer 소유자로 사용하고, `ADMIN`이 등록하면 플랫폼 기본 Offer로 생성한다.
- `offers.seller_id`는 Offer를 소유한 `seller_profiles.id`이며, `ADMIN`이 생성한 플랫폼 기본 Offer는 `NULL`이다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/categories` | 공개 | 카테고리 트리 조회 |
| GET | `/api/v1/catalog-products` | 공개 | 상품 목록·검색 |
| POST | `/api/v1/catalog-products` | PRODUCT_MANAGER, ADMIN | 상품 등록 |
| GET | `/api/v1/catalog-products/{catalogProductId}` | 공개 | 상품 상세 |
| PATCH | `/api/v1/catalog-products/{catalogProductId}` | PRODUCT_MANAGER, ADMIN | 상품 수정 |
| DELETE | `/api/v1/catalog-products/{catalogProductId}` | PRODUCT_MANAGER, ADMIN | 상품 보관 |
| PATCH | `/api/v1/offers/{offerId}/price` | Offer 소유 PRODUCT_MANAGER, ADMIN | Offer 가격 수정 |
| POST | `/api/v1/offers/{offerId}/inventory-adjustments` | PRODUCT_MANAGER, ADMIN | 재고 조정 |
| GET | `/api/v1/catalog-products/{catalogProductId}/reviews` | 공개 | 리뷰 목록 |
| POST | `/api/v1/catalog-products/{catalogProductId}/reviews` | 로그인 | 리뷰 작성 |
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

`POST /api/v1/catalog-products`

권한:

- `PRODUCT_MANAGER`는 `SellerProfile.status = ACTIVE`일 때만 호출할 수 있다.
- `ADMIN`은 플랫폼 운영자로서 호출할 수 있다.
- `USER`, 승인되지 않은 `PRODUCT_MANAGER`, 비활성·정지된 판매자는 호출할 수 없다.
- `PRODUCT_MANAGER`와 `ADMIN` 모두 `managerId` 또는 `sellerId`를 요청 본문으로 전달하지 않는다.

요청:

```json
{
  "categoryId": "uuid",
  "name": "무선 헤드폰",
  "description": "상품 상세 설명",
  "brand": "Example Brand",
  "asin": "B0EXAMPLE1",
  "gtin": "8801234567890",
  "upc": "012345678905",
  "ean": "8801234567890",
  "isbn": "9781234567897",
  "tags": ["무선", "블루투스"],
  "attributes": { "connectionType": "Bluetooth", "color": "Black" },
  "variant": {
    "sku": "HEADPHONE-BLK-001",
    "displayName": "블랙",
    "weight": 0.25,
    "dimensions": { "width": 18, "height": 20, "depth": 8, "unit": "cm" }
  },
  "offer": {
    "basePrice": { "amount": 49900.00, "currency": "KRW" }
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
- `offer.basePrice.amount`, `offer.basePrice.currency`
- `inventory.initialQuantity`
- `media`의 이미지 1장 이상과 대표 이미지 정확히 1장

선택 입력:

- `brand`, `asin`, `gtin`, `upc`, `ean`, `isbn`, `tags`, `attributes`, `variant.weight`, `variant.dimensions`

입력 규칙:

- `categoryId`는 삭제되지 않은 기존 카테고리여야 한다. 카테고리 생성·수정·삭제는 P7의 `ADMIN` API에서 수행한다.
- `name`과 `description`은 공백만으로 구성할 수 없다.
- `tags`는 중복 없이 저장한다. `attributes`는 JSON object이며 서버가 허용하지 않은 구조는 거부한다.
- `variant.sku`는 시스템 전체에서 유일해야 하며 생성 후 변경하지 않는다.
- `variant.displayName`은 실제 구매 단위를 설명하는 값이며 옵션 선택 목록 자체가 아니다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 선택 입력이며 각각 상품 간 중복될 수 없다. 내부 PK로 사용하지 않는다.
- `offer.basePrice.amount`와 `inventory.initialQuantity`는 0 이상이어야 한다.
- `offer.basePrice.currency`는 3자리 대문자 통화 코드다.
- 등록 시 할인 가격은 받지 않는다. 할인은 `PATCH /api/v1/offers/{offerId}/price`에서 설정한다.
- `media`는 기본 요구사항에서 `IMAGE`만 허용하고, 대표 이미지(`isPrimary = true`)는 정확히 1개여야 한다.
- `media.sortOrder`는 요청 내에서 중복될 수 없으며 이미지 URL 형식을 검증한다.

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

1. 인증 주체의 역할과 판매자 프로필 상태를 확인한다.
2. 카테고리 존재·활성 상태와 SKU 중복 여부를 확인한다.
3. 가격·재고·ProductVariant·Media 입력을 검증한다.
4. 서버가 `catalogProductId`, `variantId`, `offerId`를 생성한다.
5. `catalog_products.manager_id`를 인증된 `users.id`로 저장한다.
6. `PRODUCT_MANAGER`가 등록하면 인증된 `SellerProfile.id`를 `offers.seller_id`로 저장한다.
7. `ADMIN`이 등록하면 플랫폼 기본 Offer로 생성하고 `offers.seller_id`는 `NULL`로 저장한다.
8. CatalogProduct, ProductVariant, Offer, 가격, Inventory, Media를 하나의 트랜잭션으로 생성한다.
9. 재고가 0이면 `OUT_OF_STOCK`, 1 이상이면 `IN_STOCK`으로 계산한다.

성공 응답 `201`:

```json
{
  "catalogProductId": "uuid",
  "variantId": "uuid",
  "offerId": "uuid",
  "sku": "HEADPHONE-BLK-001",
  "name": "무선 헤드폰",
  "publicationStatus": "ACTIVE",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "appliedPrice": { "amount": 49900.00, "currency": "KRW" },
  "availabilityStatus": "IN_STOCK",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 심화 사항

- 다중 ProductVariant와 variation theme을 지원한다.
- ProductVariant별 이미지·가격·재고를 지원한다.
- 다중 판매자 Offer, 상품 상태, 출고자, 배송 조건, 반품 정책을 지원한다.
- 대표 Offer 선택과 Buy Box를 지원한다.

### 3-3. 상품 수정·보관

- `PATCH /api/v1/catalog-products/{catalogProductId}`는 전달된 필드만 수정한다.
- 요청 본문에서 허용하는 상품 필드는 `name`, `description`, `brand`, `tags`, `attributes`다.
- `categoryId`, `variant`, `offer`, `inventory`, `media`, `publicationStatus`, `managerId`, `sellerId`는 상품 수정 요청으로 받지 않는다.
- `categoryId` 변경은 카테고리 메타데이터와 상품 검색에 영향을 주므로 기본 요구사항에서는 지원하지 않는다.
- 상품 가격은 CatalogProduct가 아니라 Offer가 소유하므로 `PATCH /api/v1/offers/{offerId}/price`에서 수정한다.
- `ProductVariant`의 `sku`는 구매·장바구니·재고·주문을 식별하는 값이므로 주문 이력 여부와 관계없이 변경할 수 없다.
- `ProductVariant`의 `displayName`, `weight`, `dimensions`와 Media 변경 API는 기본 요구사항에 포함하지 않는다.
- `PRODUCT_MANAGER`는 `catalog_products.manager_id`가 본인인 상품만 수정할 수 있고, `ADMIN`은 모든 상품을 수정할 수 있다.
- 요청에 포함되지 않은 필드는 기존 값을 유지한다. `null`은 nullable 필드에서만 허용하고, `name`·`description`의 `null`은 거부한다.
- 허용되지 않은 필드만 전달하거나 빈 객체를 전달하면 `400 VALIDATION_ERROR`를 반환한다.
- 수정은 하나의 트랜잭션으로 처리하며 보관된 상품은 일반 상품 수정 API로 수정할 수 없다.
- `DELETE`는 물리 삭제하지 않고 `publicationStatus = ARCHIVED`와 `archivedAt = 현재 시각`으로 변경한다. 활성 상품은 `archivedAt = null`이어야 한다.
- 보관 상품은 공개 목록에서 제외한다.
- 주문 상세에는 주문 당시 상품명·SKU·가격 스냅샷을 표시한다.

수정 요청 예시:

```json
{
  "name": "수정된 상품명",
  "description": "수정된 상품 설명",
  "brand": "Example Brand",
  "tags": ["무선", "블루투스"],
  "attributes": {
    "connectionType": "Bluetooth",
    "color": "Black"
  }
}
```

성공 응답:

```json
{
  "catalogProductId": "uuid",
  "publicationStatus": "ACTIVE",
  "updatedAt": "2026-08-09T12:05:00Z"
}
```

### 3-4. 상품 목록·검색

`GET /api/v1/catalog-products`

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
      "catalogProductId": "uuid",
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

- 기본 정렬은 `RELEVANCE`이며, 같은 정렬값은 `catalogProductId`를 보조 키로 사용한다.
- `LATEST`는 `createdAt DESC, catalogProductId DESC`, 가격·평점 정렬도 동일한 보조 키를 사용한다.
- `cursor`에는 검색어·필터·정렬 조건이 포함되므로 조건을 변경한 요청에 기존 cursor를 재사용할 수 없다.
- 키워드는 상품명·설명·브랜드를 대상으로 검색한다.
- 카테고리 필터는 하위 카테고리를 포함한다.
- 품절 상품은 `availability=IN_STOCK`일 때 제외한다.
- 요구사항에서는 LIKE 검색을 허용하되, 검색 결과의 응답 구조는 전문 검색 엔진으로 교체해도 유지한다.

#### 심화 사항

- 카테고리별 동적 Facet, 자동완성, 오타 보정, 전문 검색을 지원한다.

### 3-5. 상품 상세

`GET /api/v1/catalog-products/{catalogProductId}`

응답:

```json
{
  "catalogProductId": "uuid",
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
- `PATCH /api/v1/offers/{offerId}/price`는 전달된 가격 필드만 수정한다.
- 가격 수정 요청은 `basePrice`, `discountPrice`, `discountStartAt`, `discountEndAt` 필드를 사용한다.
- `basePrice.amount`와 `discountPrice.amount`는 0 이상이어야 하며, 할인 가격은 기본 가격보다 작아야 한다.
- 할인 가격이 없으면 할인 시작·종료 시각도 전달할 수 없다.
- 할인 시작 시각은 종료 시각보다 이전이어야 한다.
- API는 기본 가격, 적용 가격, 할인율을 구분한다.
- 가격 변경 권한은 Offer 소유 `PRODUCT_MANAGER`와 `ADMIN`이다.
- 다른 판매자의 Offer 가격은 수정할 수 없다.

가격 수정 요청:

```json
{
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": { "amount": 44900.00, "currency": "KRW" },
  "discountStartAt": "2026-08-10T00:00:00Z",
  "discountEndAt": "2026-08-31T23:59:59Z"
}
```

- 요청에 포함되지 않은 가격 필드는 기존 값을 유지한다.
- `discountPrice`가 `null`이면 할인을 제거하고 할인 기간도 함께 제거한다.
- `currency`는 기존 Offer의 통화와 달라질 수 없다.
- 성공 응답은 `offerId`, 기본 가격, 할인 가격, 적용 가격, 할인율, `updatedAt`을 반환한다.

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

`GET /api/v1/catalog-products/{catalogProductId}/reviews`

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

`POST /api/v1/catalog-products/{catalogProductId}/reviews` 요청:

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

- ProductVariant별 리뷰, 인증 구매, 고객 이미지·동영상, 도움됨 투표와 리뷰 검수를 지원한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 검증 실패 |
| 400 | `INVALID_PRICE` | 가격이 음수이거나 할인 가격이 기본 가격 이상 |
| 400 | `INVALID_STOCK` | 재고가 음수 |
| 400 | `INVALID_MEDIA` | 이미지 형식·URL·정렬 순서가 잘못됨 |
| 400 | `DUPLICATE_PRIMARY_MEDIA` | 대표 이미지가 없거나 2개 이상임 |
| 400 | `INVALID_DISCOUNT_PERIOD` | 할인 기간 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | 상품 관리 권한 부족 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 활성 SellerProfile 없는 PRODUCT_MANAGER의 상품 등록 |
| 403 | `REVIEW_NOT_ELIGIBLE` | 구매·배송 완료 조건 미충족 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | 상품 없음 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant 없음 |
| 404 | `OFFER_NOT_FOUND` | Offer 없음 |
| 409 | `SKU_ALREADY_EXISTS` | SKU 중복 |
| 409 | `CATEGORY_HAS_CHILDREN` | 하위 카테고리 또는 상품 존재 |
| 409 | `INSUFFICIENT_STOCK` | 주문 수량이 재고보다 많음 |
| 409 | `REVIEW_ALREADY_EXISTS` | 이미 리뷰 작성 |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관 상품 변경 시도 |
