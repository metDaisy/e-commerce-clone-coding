# P2 Catalog & Inventory (상품·카탈로그·재고)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 모델

```text
CatalogProduct (카탈로그 상품 메타데이터/전시 정보)
  └─ ProductVariant (실제 구매 단위: SKU)
       └─ Offer (판매 조건과 가격)
            └─ Inventory (구매 가능 수량)
```

현재 요구사항과 SQL은 다음 도메인 용어를 기준으로 한다. `CatalogProduct`는 공통 카탈로그
상품 메타데이터이고, `ProductVariant`는 판매자가 판매하는 실제 구매 단위이며, 판매자별 판매 조건은 `Offer`로 표현한다.
엔티티 클래스 이름은 이 문서와 SQL을 기준으로 이후 구현한다.

- `CatalogProduct`는 고객에게 하나의 CatalogProduct 상세 페이지로 보이는 상품 메타데이터 집합이다. 예를 들어 `무선 헤드폰`이라는 이름·설명·브랜드·카테고리를 가진다.
- `ProductVariant`는 고객이 실제로 선택하고 주문하는 개별 옵션 조합이다. 예를 들어 같은 `무선 헤드폰`의 `블랙/대형`과 `화이트/소형`은 서로 다른 ProductVariant다.
- ProductVariant는 반드시 하나의 `sku`를 가지며, SKU는 주문·장바구니·재고 차감에서 사용하는 실제 판매 단위 식별자다.
- 색상·사이즈처럼 선택 가능한 값이 없는 단일 상품도 ProductVariant를 생략하지 않는다. 이 경우 `displayName`을 `기본 옵션`으로 정하고 ProductVariant 1개를 생성한다.
- `Offer`는 ProductVariant를 어떤 가격·판매 상태·상품 상태·판매자 조건으로 판매하는지를 나타낸다. 하나의 ProductVariant에 여러 Offer를 둘 수 있으며, 각 Seller은 기본 요구사항에서 같은 ProductVariant에 Offer를 하나만 등록한다.
- `Inventory`는 Offer의 구매 가능 수량이다. 따라서 재고는 CatalogProduct 전체가 아니라 구매 가능한 판매 조건 단위로 차감한다.
- CatalogProduct 등록은 메타데이터만 생성한다. ProductVariant와 Media는 별도 API로 등록하고, Offer는 P8 판매자 API에서 등록한다.
- 현재 요구사항은 다중 옵션 선택 UI를 구현하지 않지만 ProductVariant를 별도 요청으로 하나씩 추가할 수 있다.

예시:

| 계층 | 예시 | 의미 |
|---|---|---|
| CatalogProduct | 무선 헤드폰 | CatalogProduct 상세 페이지와 공통 상품 메타데이터 |
| ProductVariant | 블랙 / 대형, SKU `HEADPHONE-BLK-L-001` | 실제 주문·배송되는 옵션 조합 |
| Offer | 49,900원, ACTIVE | 해당 ProductVariant의 판매 가격과 판매 조건 |
| Inventory | 100개 | 해당 Offer의 구매 가능 수량 |

예를 들어 `GIGABYTE AMD R9700`을 카탈로그에 등록하면 다음과 같이 구성한다.

```text
CatalogProduct: GIGABYTE AMD R9700
├─ ProductVariant: White / SKU-GIGA-R9700-W
│  ├─ Offer: 판매자 A / 500,000원 / Inventory 10개
│  └─ Offer: 판매자 B / 510,000원 / Inventory 4개
└─ ProductVariant: Black / SKU-GIGA-R9700-B
   └─ Offer: 판매자 A / 505,000원 / Inventory 7개
```

- `CatalogProduct`는 상품명·브랜드처럼 여러 Variant가 공유하는 카탈로그 메타데이터를 가진다.
- `ProductVariant`는 색상·사이즈와 SKU처럼 고객이 실제로 선택하고 주문하는 판매 단위다.
- `Offer`는 특정 ProductVariant를 판매하는 판매자별 가격·판매 상태·판매 조건이다.
- `Inventory`는 Offer별 재고이므로 같은 ProductVariant라도 판매자마다 수량이 다를 수 있다.

- CatalogProduct 등록자는 판매자 역할인 `PRODUCT_MANAGER` 또는 플랫폼 운영 역할인 `ADMIN`이다.
- CatalogProduct를 생성할 때 ProductVariant·Offer·Inventory를 함께 생성하지 않는다.
- 판매자 신청·승인과 판매자 Offer 등록·관리는 P8에서 정의한다. `PRODUCT_MANAGER`는 자신의 CatalogProduct·Offer·재고를 관리하고, `ADMIN`은 플랫폼 운영 목적으로 동일 기능을 수행할 수 있다.
- `catalogProductId`, `variantId`, `offerId`는 서로 다른 서버 생성 UUID다.
- 하나의 `ProductVariant`는 정확히 하나의 `CatalogProduct`에만 속한다. 따라서 `(catalogProductId, variantId)` 조합은 유일하며, `variantId` 자체가 전역 고유 식별자다.
- `catalog_products.manager_id`는 CatalogProduct를 등록·관리한 `users.id`다. `PRODUCT_MANAGER`는 자신이 등록한 CatalogProduct만 수정·보관할 수 있고, `ADMIN`은 모든 CatalogProduct를 관리할 수 있다.
- `sku`는 구매 단위의 고유 식별자다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 외부 식별자로 선택 저장하며 내부 PK로 사용하지 않는다.
- CatalogProduct 등록 요청에는 `sellerId`를 받지 않는다. Offer 소유자와 `offers.seller_id`는 P8 Offer 등록 시 인증된 `Seller`로 결정한다.
- `offers.seller_id`는 Offer를 소유한 `seller_profiles.id`이며, 플랫폼 기본 Offer를 생성하는 경우에만 `NULL`이다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/categories` | 공개 | 카테고리 트리 조회 |
| GET | `/api/v1/catalog-products` | 공개 | CatalogProduct 목록·검색 |
| POST | `/api/v1/catalog-products` | PRODUCT_MANAGER, ADMIN | CatalogProduct 등록 |
| POST | `/api/v1/catalog-products/{catalogProductId}/variants` | PRODUCT_MANAGER, ADMIN | ProductVariant 등록 |
| POST | `/api/v1/catalog-products/{catalogProductId}/media` | PRODUCT_MANAGER, ADMIN | CatalogProduct Media 등록 |
| POST | `/api/v1/product-variants/{variantId}/media` | PRODUCT_MANAGER, ADMIN | ProductVariant Media 등록 |
| GET | `/api/v1/catalog-products/{catalogProductId}` | 공개 | CatalogProduct 상세 |
| PATCH | `/api/v1/catalog-products/{catalogProductId}` | PRODUCT_MANAGER, ADMIN | CatalogProduct 수정 |
| PATCH | `/api/v1/catalog-products/{catalogProductId}/identifiers` | PRODUCT_MANAGER, ADMIN | CatalogProduct 식별자 수정 |
| POST | `/api/v1/catalog-products/{catalogProductId}/archive` | PRODUCT_MANAGER, ADMIN | CatalogProduct 보관 |
| PATCH | `/api/v1/offers/{offerId}/price` | Offer 소유 PRODUCT_MANAGER, ADMIN | Offer 가격 수정 |
| POST | `/api/v1/offers/{offerId}/inventory-adjustments` | PRODUCT_MANAGER, ADMIN | 재고 조정 |
| GET | `/api/v1/catalog-products/{catalogProductId}/reviews` | 공개 | 리뷰 목록 |
| POST | `/api/v1/product-variants/{variantId}/reviews` | 로그인 | ProductVariant 리뷰 작성 |
| PATCH | `/api/v1/reviews/{reviewId}` | 작성자 | 리뷰 수정 |
| DELETE | `/api/v1/reviews/{reviewId}` | 작성자, ADMIN | 리뷰 삭제 |

## 3. 요구사항

### 3-1. 카테고리

- 카테고리는 `ADMIN`이 관리하는 계층형 메타데이터이며, P2는 상품 분류와 공개 트리 조회만 담당한다.
- `parent_id` 자기참조 FK와 `depth`로 계층을 저장한다. 루트는 `parent_id = NULL`, `depth = 1`이고 최대 깊이는 3이다.
- 부모 존재 여부, 부모-자식 `depth` 관계, 자기 자신·하위 카테고리 지정, 순환 참조 검증은 별도 P2 API가 아니라 [P7 관리자 카테고리 생성·수정](p7-admin.md#3-2-카테고리-관리)의 처리 규칙이다.
- `GET /api/v1/categories`는 전체 트리를 다음 형태로 반환한다.

```json
{
  "data": [
    {
      "categoryId": "uuid",
      "name": "전자기기",
      "parentId": null,
      "children": []
    }
  ]
}
```

- 카테고리별 CatalogProduct 조회는 해당 카테고리와 모든 하위 카테고리를 포함한다.
- 카테고리 생성·수정·삭제는 구매자·판매자 API가 아닌 P7의 `ADMIN` 전용 API로 수행한다.
- 하위 카테고리 또는 CatalogProduct가 연결된 카테고리는 P7 관리자 API에서도 삭제할 수 없다.
- 기본 요구사항에는 `GET /api/v1/categories/{categoryId}`를 두지 않는다. 공개 트리 조회와 `GET /api/v1/catalog-products?categoryId=...`로 필요한 조회를 제공하고, CatalogProduct 상세는 자신의 category 정보를 반환한다.

### 3-2. CatalogProduct 등록

`POST /api/v1/catalog-products`

권한:

- `PRODUCT_MANAGER`는 `Seller.status = ACTIVE`일 때만 호출할 수 있다.
- `ADMIN`은 플랫폼 운영자로서 호출할 수 있다.
- `USER`, 승인되지 않은 `PRODUCT_MANAGER`, 비활성·정지된 판매자는 호출할 수 없다.
- `PRODUCT_MANAGER`와 `ADMIN` 모두 `managerId` 또는 `sellerId`를 요청 본문으로 전달하지 않는다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 등록 요청으로 받지 않는다. CatalogProduct 생성 후 식별자 전용 API에서 별도로 저장한다.
- 이 API는 `catalog_products`와 CatalogProduct의 태그 연결만 생성한다. ProductVariant·Offer·Inventory·Media는 생성하지 않는다.

요청:

```json
{
  "categoryId": "uuid",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "tags": ["무선", "블루투스"],
  "attributes": { "connectionType": "Bluetooth" }
}
```

필수 입력:

- `categoryId`
- `name`, `description`

선택 입력:

- `brand`, `tags`, `attributes`

입력 규칙:

- `categoryId`는 삭제되지 않은 기존 카테고리여야 한다. 카테고리 생성·수정·삭제는 P7의 `ADMIN` API에서 수행한다.
- `name`과 `description`은 공백만으로 구성할 수 없다.
- `tags`는 중복 없이 저장한다. `attributes`는 JSON object이며 서버가 허용하지 않은 구조는 거부한다.
- 등록 요청에 식별자 5개 중 하나라도 포함하면 `400 VALIDATION_ERROR`를 반환한다.

처리 규칙:

1. 인증 주체의 역할과 판매자 프로필 상태를 확인한다.
2. 카테고리 존재·활성 상태와 CatalogProduct 메타데이터를 검증한다.
3. 서버가 `catalogProductId`를 생성한다.
4. `catalog_products.manager_id`를 인증된 `users.id`로 저장한다.
5. CatalogProduct와 태그 연결을 하나의 트랜잭션으로 생성한다.

성공 응답 `201`:

```json
{
  "catalogProductId": "uuid",
  "categoryId": "uuid",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "publicationStatus": "ACTIVE",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

CatalogProduct는 ProductVariant가 등록되고, 하나 이상의 `ACTIVE` Offer와 수량이 1 이상인 Inventory가 존재한 후 실제 구매 가능한 상태가 된다. ProductVariant·Offer·Inventory가 없거나 구매 가능한 Offer가 없는 CatalogProduct는 메타데이터 조회는 가능하지만 구매 대상으로 노출하지 않는다.

#### 3-2-1. ProductVariant 등록

`POST /api/v1/catalog-products/{catalogProductId}/variants`

- `PRODUCT_MANAGER`는 자신이 관리하는 CatalogProduct에만 등록할 수 있고, `ADMIN`은 모든 CatalogProduct에 등록할 수 있다.
- 이 API는 ProductVariant만 생성하며 Offer·Inventory·Media는 생성하지 않는다.
- `catalogProductId`는 존재하고 보관되지 않은 CatalogProduct여야 한다.
- `sku`는 시스템 전체에서 유일하며 생성 후 변경하지 않는다.
- `displayName`은 실제 구매 단위를 설명하는 값이다.

요청:

```json
{
  "sku": "HEADPHONE-BLK-001",
  "displayName": "블랙",
  "weight": 0.25,
  "dimensions": { "width": 18, "height": 20, "depth": 8, "unit": "cm" }
}
```

성공 응답 `201`:

```json
{
  "variantId": "uuid",
  "catalogProductId": "uuid",
  "sku": "HEADPHONE-BLK-001",
  "displayName": "블랙",
  "createdAt": "2026-08-09T12:05:00Z"
}
```

#### 3-2-2. Media 등록

CatalogProduct 공통 이미지와 ProductVariant별 이미지는 대상별 API로 등록한다.

```http
POST /api/v1/catalog-products/{catalogProductId}/media
POST /api/v1/product-variants/{variantId}/media
```

요청:

```json
{
  "type": "IMAGE",
  "url": "https://cdn.example.com/headphone-main.jpg",
  "isPrimary": true,
  "sortOrder": 0
}
```

- 기본 요구사항에서는 `type = IMAGE`만 허용한다.
- 대상별 대표 이미지는 정확히 1개여야 한다.
- 대상별 `sortOrder`는 중복될 수 없고 이미지 URL 형식을 검증한다.
- Media 등록은 CatalogProduct·ProductVariant 생성과 별도 트랜잭션으로 처리한다.

성공 응답 `201`:

```json
{
  "mediaId": "uuid",
  "ownerId": "uuid",
  "ownerType": "CATALOG_PRODUCT",
  "type": "IMAGE",
  "url": "https://cdn.example.com/headphone-main.jpg",
  "isPrimary": true,
  "sortOrder": 0,
  "createdAt": "2026-08-09T12:06:00Z"
}
```

#### 심화 사항

- variation theme과 옵션 조합을 지원한다.
- Media 변경·삭제 API와 이미지 검수를 지원한다.
- 출고자, 배송 조건, 반품 정책을 지원한다.
- 대표 Offer 선택을 바탕으로 한 Buy Box 정책을 지원한다.

### 3-3. CatalogProduct 수정·보관

#### 3-3-1. CatalogProduct 수정

- `PATCH /api/v1/catalog-products/{catalogProductId}`는 전달된 필드만 수정한다.
- 요청 본문에서 허용하는 CatalogProduct 메타데이터 필드는 `name`, `description`, `brand`, `tags`, `attributes`다.
- `categoryId`, `variant`, `offer`, `inventory`, `media`, `publicationStatus`, `managerId`, `sellerId`는 CatalogProduct 수정 요청으로 받지 않는다.
- `categoryId` 변경은 카테고리 메타데이터와 CatalogProduct 검색에 영향을 주므로 기본 요구사항에서는 지원하지 않는다.
- CatalogProduct는 가격을 소유하지 않으며, 가격은 Offer가 소유하므로 `PATCH /api/v1/offers/{offerId}/price`에서 수정한다.
- `ProductVariant`의 `sku`는 구매·장바구니·재고·주문을 식별하는 값이므로 주문 이력 여부와 관계없이 변경할 수 없다.
- `ProductVariant`의 `displayName`, `weight`, `dimensions`와 Media 수정·삭제 API는 기본 요구사항에 포함하지 않는다. Media 등록 API는 별도로 제공한다.
- `PRODUCT_MANAGER`는 `catalog_products.manager_id`가 본인인 CatalogProduct만 수정할 수 있고, `ADMIN`은 모든 CatalogProduct를 수정할 수 있다.
- 요청에 포함되지 않은 필드는 기존 값을 유지한다. `null`은 nullable 필드에서만 허용하고, `name`·`description`의 `null`은 거부한다.
- 허용되지 않은 필드만 전달하거나 빈 객체를 전달하면 `400 VALIDATION_ERROR`를 반환한다.
- 수정은 하나의 트랜잭션으로 처리하며 보관된 CatalogProduct는 일반 CatalogProduct 수정 API로 수정할 수 없다.
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
  "categoryId": "uuid",
  "name": "수정된 상품명",
  "description": "수정된 상품 설명",
  "brand": "Example Brand",
  "asin": "B0EXAMPLE1",
  "gtin": "8801234567890",
  "upc": "012345678905",
  "ean": "8801234567890",
  "isbn": "9781234567897",
  "tags": ["무선", "블루투스"],
  "attributes": {
    "connectionType": "Bluetooth",
    "color": "Black"
  },
  "publicationStatus": "ACTIVE",
  "archivedAt": null,
  "createdAt": "2026-08-09T12:00:00Z",
  "updatedAt": "2026-08-09T12:05:00Z"
}
```

위 응답은 공통 응답 envelope의 `data` 내부에 포함한다. 수정된 CatalogProduct의 현재 상태를 반환하며, `asin`, `gtin`, `upc`, `ean`, `isbn`은 응답에는 포함하지만 수정 요청으로는 받지 않는다.

`asin`, `gtin`, `upc`, `ean`, `isbn`은 CatalogProduct 등록 후 일반 수정 API로 변경할 수 없다. 이 값들을 `PATCH /api/v1/catalog-products/{catalogProductId}` 요청으로 전달하면 `400 VALIDATION_ERROR`를 반환한다.

#### 3-3-2. CatalogProduct 식별자 수정

`PATCH /api/v1/catalog-products/{catalogProductId}/identifiers`

- `PRODUCT_MANAGER`는 자신이 관리하는 CatalogProduct만 수정할 수 있고, `ADMIN`은 모든 CatalogProduct를 수정할 수 있다.
- 요청 본문에는 `asin`, `gtin`, `upc`, `ean`, `isbn` 중 수정할 값을 포함한다.
- 요청에 포함되지 않았거나 `null`인 식별자는 기존 값을 유지한다. 식별자 삭제는 지원하지 않는다.
- 보관된 CatalogProduct에는 사용할 수 없다.
- 요청에 포함된 식별자는 식별자 타입별 검증기를 통해 확인한다.
- 하나라도 검증에 실패하면 전체 수정을 저장하지 않고 `400 PRODUCT_CODE_ERROR`를 반환한다.
- 확인과 수정은 하나의 트랜잭션에서 처리한다.

서버에 전달되는 수정 요청 JSON:

```json
{
  "asin": "B0EXAMPLE1",
  "gtin": "8801234567890"
}
```

요청에 포함된 식별자만 확인·수정하며, 나머지 식별자는 변경하지 않는다.

성공 응답 `200` (공통 응답 envelope의 `data`):

```json
{
  "id": "uuid",
  "asin": "B0EXAMPLE1",
  "gtin": "8801234567890",
  "upc": null,
  "ean": "8801234567890",
  "isbn": null,
  "updatedAt": "2026-08-09T12:08:00Z"
}
```

#### 3-3-3. CatalogProduct 보관

`POST /api/v1/catalog-products/{catalogProductId}/archive`

- 요청 본문은 없다.
- `PRODUCT_MANAGER`는 자신이 관리하는 CatalogProduct만 보관할 수 있고, `ADMIN`은 모든 CatalogProduct를 보관할 수 있다.
- 보관은 물리 삭제가 아니라 `publicationStatus = ARCHIVED`, `archivedAt = 현재 시각`으로 변경하는 상태 전이이다.
- 보관된 CatalogProduct는 공개 목록·검색에서 제외한다.
- CatalogProduct의 ProductVariant·Offer·Inventory·Media를 물리 삭제하지 않는다.
- 보관된 CatalogProduct를 다시 보관하거나 일반 수정·Variant 등록의 대상으로 사용하면 `409 CATALOG_PRODUCT_ARCHIVED`를 반환한다.

성공 응답 `200` (공통 응답 envelope의 `data`):

```json
{
  "catalogProductId": "uuid",
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-09T12:10:00Z",
  "updatedAt": "2026-08-09T12:10:00Z"
}
```

### 3-4. CatalogProduct 목록·검색

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
  "data": [
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
- 키워드는 CatalogProduct의 상품명·설명·브랜드 메타데이터를 대상으로 검색한다.
- 카테고리 필터는 하위 카테고리를 포함한다.
- 구매 가능한 ProductVariant가 없는 CatalogProduct는 `availability=IN_STOCK`일 때 제외한다.
- 요구사항에서는 LIKE 검색을 허용하되, 검색 결과의 응답 구조는 전문 검색 엔진으로 교체해도 유지한다.

#### 심화 사항

- 카테고리별 동적 Facet, 자동완성, 오타 보정, 전문 검색을 지원한다.

목록의 각 항목은 하나의 CatalogProduct를 나타낸다. `variantId`와 `offerId`는 목록 카드에 표시할 대표 판매 단위다.

- `ACTIVE` Offer 중 재고가 있는 Offer를 우선한다.
- 적용 가격이 가장 낮은 Offer를 선택하고, 가격이 같으면 `offerId` 오름차순을 사용한다.
- 구매 가능한 Offer가 없으면 `variantId`, `offerId`는 `null`이다.
- `thumbnailUrl`은 대표 Variant의 대표 이미지가 우선이고 없으면 CatalogProduct 대표 이미지로 대체한다. 두 이미지가 모두 없을 때만 `null`이다.
- 구매 가능한 Offer가 없으면 `currentPrice`, `originalPrice`, `discountRate`는 `null`이고 `availabilityStatus`는 `OUT_OF_STOCK`이다. `rating`은 구매 가능 여부와 무관하게 CatalogProduct 하위 Variant 리뷰를 통합해 반환한다.
- 전체 Variant와 Offer는 상세 API에서 반환한다.

### 3-5. CatalogProduct 상세

`GET /api/v1/catalog-products/{catalogProductId}`

플랫폼 기본 Offer는 판매자가 없으므로 상세 응답의 `sellerId`는 `null`일 수 있다.

응답:

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
      "selected": true,
      "offers": [
        {
          "offerId": "uuid",
          "sellerId": null,
          "price": { "amount": 49900.00, "currency": "KRW" },
          "originalPrice": { "amount": 59900.00, "currency": "KRW" },
          "discountRate": 16.7,
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

`reviewSummary`는 해당 CatalogProduct에 속한 모든 ProductVariant 리뷰를 통합해 계산한다.

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
- `PRODUCT_MANAGER`는 자신의 Offer만 조정할 수 있고, `ADMIN`은 모든 Offer를 조정할 수 있다.
- 공개 상품 조회 응답에는 정확한 수량 대신 `IN_STOCK`, `OUT_OF_STOCK`를 반환한다. Offer 소유자·`ADMIN`의 재고 조정 응답에는 감사와 후속 조정을 위해 정확한 전후 수량을 반환한다.
- 고객 CatalogProduct 상세 페이지의 실시간 WebSocket은 요구사항에 포함하지 않는다.

#### 심화 사항

- 재고 예약, 예약 만료, 안전재고, 다중 창고, 백오더를 지원한다.

### 3-8. 리뷰

`GET /api/v1/catalog-products/{catalogProductId}/reviews`

응답:

```json
{
  "summary": { "averageRating": 4.5, "reviewCount": 120 },
  "data": [
    {
      "reviewId": "uuid",
      "variantId": "uuid",
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
- 리뷰 목록은 해당 CatalogProduct에 속한 모든 ProductVariant의 리뷰를 통합해 반환한다.
- `cursor`와 `page`를 함께 보내면 `PAGINATION_PARAMETER_CONFLICT`를 반환한다.

`POST /api/v1/product-variants/{variantId}/reviews` 요청:

```json
{
  "rating": 5,
  "content": "좋은 상품입니다.",
  "imageUrls": []
}
```

- 해당 ProductVariant를 구매하고 배송 완료(`DELIVERED`)된 구매자만 작성한다.
- 사용자당 하나의 ProductVariant에 하나의 리뷰만 허용한다.
- 평점은 1~5 정수, 본문은 최대 2000자, 이미지는 최대 5장이다.

#### 심화 사항

- 인증 구매, 고객 이미지·동영상, 도움됨 투표와 리뷰 검수를 지원한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 검증 실패 |
| 400 | `INVALID_PRICE` | 가격이 음수이거나 할인 가격이 기본 가격 이상 |
| 400 | `INVALID_STOCK` | 재고가 음수 |
| 400 | `INVALID_MEDIA` | 이미지 형식·URL·정렬 순서가 잘못됨 |
| 400 | `DUPLICATE_PRIMARY_MEDIA` | 대표 이미지가 없거나 2개 이상임 |
| 400 | `INVALID_DISCOUNT_PERIOD` | 할인 기간 오류 |
| 400 | `PRODUCT_CODE_ERROR` | 상품 식별자(`asin`, `gtin`, `upc`, `ean`, `isbn`) 검증 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | CatalogProduct 또는 Offer 관리 권한 부족 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 활성 Seller 없는 PRODUCT_MANAGER의 CatalogProduct 등록 |
| 403 | `REVIEW_NOT_ELIGIBLE` | 구매·배송 완료 조건 미충족 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | CatalogProduct 없음 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant 없음 |
| 404 | `OFFER_NOT_FOUND` | Offer 없음 |
| 409 | `SKU_ALREADY_EXISTS` | SKU 중복 |
| 409 | `INSUFFICIENT_STOCK` | 주문 수량이 재고보다 많음 |
| 409 | `REVIEW_ALREADY_EXISTS` | 이미 리뷰 작성 |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관된 CatalogProduct 변경 시도 |
