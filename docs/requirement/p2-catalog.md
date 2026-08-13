# P2 Catalog (카탈로그)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 모델

```text
CatalogProduct (카탈로그 상품 메타데이터/전시 정보)
  └─ ProductVariant (실제 구매 단위: SKU)
```

현재 요구사항과 SQL은 다음 도메인 용어를 기준으로 한다. `CatalogProduct`는 공통 카탈로그
상품 메타데이터이고, `ProductVariant`는 CatalogProduct에 속한 공용 실제 구매 단위이며, 판매자별 판매 조건은 `Offer`로 표현한다.
엔티티 클래스 이름은 이 문서와 SQL을 기준으로 이후 구현한다.

- `CatalogProduct`는 고객 상품 페이지에 사용되는 공통 상품 메타데이터 집합이다. 판매자·관리자용 Catalog 조회와 고객용 조합 상세는 각각 P2·P9의 read model로 제공한다.
- `ProductVariant`는 고객이 실제로 선택하고 주문하는 개별 옵션 조합이다. 예를 들어 같은 `무선 헤드폰`의 `블랙/대형`과 `화이트/소형`은 서로 다른 ProductVariant다.
- ProductVariant는 반드시 하나의 `sku`를 가지며, SKU는 주문·장바구니·재고 차감에서 사용하는 실제 판매 단위 식별자다.
- 색상·사이즈처럼 선택 가능한 값이 없는 단일 상품도 ProductVariant를 생략하지 않는다. 이 경우 `displayName`을 `기본 옵션`으로 정하고 ProductVariant 1개를 생성한다.
- `Offer`는 ProductVariant를 어떤 가격·판매 상태·판매자 조건으로 판매하는지 나타내며, 등록·가격·재고 규칙은 [P9 Offer & Marketplace](p9-offer.md)가 소유한다.
- CatalogProduct 등록은 메타데이터만 생성한다. ProductVariant와 상품용 Media는 별도 API로 등록하고, Offer는 P9 판매자 API에서 등록한다.
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

- CatalogProduct·ProductVariant 등록자는 플랫폼 운영 역할인 `ADMIN`이다.
- CatalogProduct를 생성할 때 ProductVariant·Offer·Inventory를 함께 생성하지 않는다.
- CatalogProduct·ProductVariant의 생성·수정·보관과 상품용 Media 관리는 P2가 소유하며 `ADMIN`만 수행한다.
- `PRODUCT_MANAGER`는 활성 Seller를 가진 User이며, 기존 ProductVariant를 확인한 뒤 P9에서 자신의 Offer·Inventory만 관리한다.
- `catalogProductId`, `variantId`, `offerId`는 서로 다른 서버 생성 UUID다.
- 하나의 `ProductVariant`는 정확히 하나의 `CatalogProduct`에만 속한다. 따라서 `(catalogProductId, variantId)` 조합은 유일하며, `variantId` 자체가 전역 고유 식별자다.
- CatalogProduct에는 관리자 소유자나 `managerId`를 저장하지 않는다. 모든 `ADMIN`이 모든 CatalogProduct·ProductVariant를 관리할 수 있다.
- 관리자 변경 이력은 심화 과정에서 별도 감사 이력으로 기록하며, 대상 ID·`adminId`·변경 내용·변경 시각을 저장한다.
- `sku`는 구매 단위의 고유 식별자다.
- `asin`, `gtin`, `upc`, `ean`, `isbn`은 외부 식별자로 선택 저장하며 내부 PK로 사용하지 않는다.
- CatalogProduct 등록 요청에는 `sellerId`를 받지 않는다. Offer 소유자와 `offers.seller_id`는 P9 Offer 등록 시 인증된 `Seller`로 결정한다.

## 2. API 목록

`ADMIN` 권한이 필요한 CatalogProduct·ProductVariant·Media 변경 API는 모두 `/api/v1/admin` prefix를 사용한다. CatalogProduct 조회는 판매자의 Offer 등록을 위한 카탈로그 조회이며, 고객용 조합 상품 조회는 P9에서 제공한다.

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/categories` | 공개 | 카테고리 트리 조회 |
| GET | `/api/v1/catalog-products/{catalogProductId}` | `PRODUCT_MANAGER` + `ACTIVE` | Offer 등록을 위한 CatalogProduct·Variant 상세 |
| GET | `/api/v1/admin/catalog-products/{catalogProductId}` | ADMIN | CatalogProduct·Variant 관리자 상세 |
| POST | `/api/v1/admin/catalog-products` | ADMIN | CatalogProduct 등록 |
| POST | `/api/v1/admin/catalog-products/{catalogProductId}/variants` | ADMIN | ProductVariant 등록 |
| PATCH | `/api/v1/admin/product-variants/{variantId}` | ADMIN | ProductVariant 수정 |
| POST | `/api/v1/admin/catalog-products/{catalogProductId}/media` | ADMIN | CatalogProduct Media 등록 |
| PATCH | `/api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}` | ADMIN | CatalogProduct Media 수정 |
| DELETE | `/api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}` | ADMIN | CatalogProduct Media 보관 |
| POST | `/api/v1/admin/product-variants/{variantId}/media` | ADMIN | ProductVariant Media 등록 |
| PATCH | `/api/v1/admin/product-variants/{variantId}/media/{mediaId}` | ADMIN | ProductVariant Media 수정 |
| DELETE | `/api/v1/admin/product-variants/{variantId}/media/{mediaId}` | ADMIN | ProductVariant Media 보관 |
| PATCH | `/api/v1/admin/catalog-products/{catalogProductId}` | ADMIN | CatalogProduct 수정 |
| PATCH | `/api/v1/admin/catalog-products/{catalogProductId}/identifiers` | ADMIN | CatalogProduct 식별자 수정 |
| POST | `/api/v1/admin/catalog-products/{catalogProductId}/archive` | ADMIN | CatalogProduct 보관 |
| POST | `/api/v1/admin/product-variants/{variantId}/archive` | ADMIN | ProductVariant 보관 |

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
- 카테고리 생성·수정은 구매자·판매자 API가 아닌 P7의 `ADMIN` 전용 API로 수행한다.
- 기본 요구사항에서는 카테고리 삭제 API를 제공하지 않는다. 카테고리 보관 정책은 심화 과정에서 별도로 결정한다.
- 기본 요구사항에는 `GET /api/v1/categories/{categoryId}`를 두지 않는다. 공개 트리 조회와 P9의 `GET /api/v1/product/search?categoryId=...`로 필요한 상품 조회를 제공한다.

### 3-2. CatalogProduct 등록

`POST /api/v1/admin/catalog-products`

권한:

- `ADMIN`만 호출할 수 있다.
- `USER`와 `PRODUCT_MANAGER`는 CatalogProduct를 생성할 수 없다.
- 요청 본문에는 `adminId`, `managerId`, `sellerId`를 받지 않으며 인증된 ADMIN을 변경 주체로 기록한다.
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

- `categoryId`는 기존 카테고리여야 한다. 카테고리 생성·수정은 P7의 `ADMIN` API에서 수행한다.
- `name`과 `description`은 공백만으로 구성할 수 없다.
- `tags`는 중복 없이 저장한다. `attributes`는 JSON object이며 서버가 허용하지 않은 구조는 거부한다.
- 등록 요청에 식별자 5개 중 하나라도 포함하면 `400 VALIDATION_ERROR`를 반환한다.

처리 규칙:

1. 인증 주체가 `ADMIN`인지 확인한다.
2. 카테고리 존재·활성 상태와 CatalogProduct 메타데이터를 검증한다.
3. 서버가 `catalogProductId`를 생성한다.
4. 인증된 ADMIN을 변경 주체로 확정한다.
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

CatalogProduct 자체는 상품 메타데이터 상태만 관리한다. 실제 구매 가능 여부는 P9 Marketplace가 ProductVariant, `ACTIVE` Offer, Inventory 수량을 조합해 판정한다. P2는 보관된 CatalogProduct·ProductVariant를 P9가 구매 대상에서 제외할 수 있도록 공개 Catalog 상태를 제공한다.

#### 3-2-1. ProductVariant 등록

`POST /api/v1/admin/catalog-products/{catalogProductId}/variants`

- `ADMIN`만 보관되지 않은 CatalogProduct에 ProductVariant를 등록할 수 있다.
- `PRODUCT_MANAGER`는 기존 ProductVariant를 조회하고 자신의 Offer를 등록할 수 있지만 ProductVariant 자체를 생성·수정·보관할 수 없다.
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

#### 3-2-1-1. ProductVariant 수정

`PATCH /api/v1/admin/product-variants/{variantId}`

- `ADMIN`만 호출할 수 있다.
- `sku`는 생성 후 변경할 수 없다.
- 수정 가능한 필드는 `displayName`, `weight`, `dimensions`다.
- `catalogProductId`, `sku`, `offer`, `inventory`, `media`는 요청으로 받지 않는다.
- 보관된 CatalogProduct에 속한 ProductVariant는 수정할 수 없다.
- 요청에 포함되지 않은 필드는 유지하며, 빈 객체나 허용되지 않은 필드만 전달하면 `400 VALIDATION_ERROR`를 반환한다.

요청:

```json
{
  "displayName": "블랙·대형",
  "weight": 0.3,
  "dimensions": { "width": 18, "height": 20, "depth": 8, "unit": "cm" }
}
```

성공 응답 `200`:

```json
{
  "variantId": "uuid",
  "catalogProductId": "uuid",
  "sku": "HEADPHONE-BLK-001",
  "displayName": "블랙·대형",
  "weight": 0.3,
  "dimensions": { "width": 18, "height": 20, "depth": 8, "unit": "cm" },
  "updatedAt": "2026-08-09T12:10:00Z"
}
```

#### 3-2-2. Media 등록

CatalogProduct 공통 이미지와 ProductVariant별 이미지는 대상별 API로 등록한다.

```http
POST /api/v1/admin/catalog-products/{catalogProductId}/media
POST /api/v1/admin/product-variants/{variantId}/media
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

- CatalogProduct와 ProductVariant Media 등록은 `ADMIN`만 호출할 수 있다.
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

`PATCH` 요청은 `url`, `isPrimary`, `sortOrder` 중 전달된 필드만 수정한다. `DELETE`는 물리 삭제가 아닌 Media 보관 처리이며, 보관 후에는 공개 조회에 포함하지 않는다. 두 API 모두 `ADMIN`만 호출할 수 있다.

`PATCH /api/v1/admin/product-variants/{variantId}/media/{mediaId}` 성공 응답 예:

```json
{
  "mediaId": "uuid",
  "ownerId": "uuid",
  "ownerType": "PRODUCT_VARIANT",
  "type": "IMAGE",
  "url": "https://cdn.example.com/headphone-main-v2.jpg",
  "isPrimary": true,
  "sortOrder": 0,
  "updatedAt": "2026-08-09T12:07:00Z"
}
```

`DELETE` 성공 응답 `200`:

```json
{
  "mediaId": "uuid",
  "ownerId": "uuid",
  "status": "ARCHIVED",
  "archivedAt": "2026-08-09T12:08:00Z"
}
```

#### 심화 사항

- variation theme과 옵션 조합을 지원한다.
- 이미지 검수와 동영상·확대 이미지 등 Media 유형 확장을 지원한다.
- 출고자, 배송 조건, 반품 정책을 지원한다.
- 대표 Offer 선택을 바탕으로 한 Buy Box 정책을 지원한다.

### 3-3. CatalogProduct 수정·보관

#### 3-3-1. CatalogProduct 수정

- `PATCH /api/v1/admin/catalog-products/{catalogProductId}`는 전달된 필드만 수정한다.
- 요청 본문에서 허용하는 CatalogProduct 메타데이터 필드는 `name`, `description`, `brand`, `tags`, `attributes`다.
- `categoryId`, `variant`, `offer`, `inventory`, `media`, `publicationStatus`, `managerId`, `sellerId`는 CatalogProduct 수정 요청으로 받지 않는다.
- `categoryId` 변경은 카테고리 메타데이터와 CatalogProduct 검색에 영향을 주므로 기본 요구사항에서는 지원하지 않는다.
- CatalogProduct는 가격을 소유하지 않으며 가격은 Offer가 소유한다. Offer 가격 API와 재고 API는 P9에서 정의한다.
- `ProductVariant`의 `sku`는 구매·장바구니·재고·주문을 식별하는 값이므로 주문 이력 여부와 관계없이 변경할 수 없다.
- `ADMIN`은 ProductVariant의 `displayName`, `weight`, `dimensions`를 수정할 수 있으며, ProductVariant Media의 등록·수정·삭제도 수행한다.
- `ADMIN`만 CatalogProduct를 수정할 수 있다.
- 수정 주체인 `adminId`와 변경 필드는 심화 과정의 관리자 변경 이력에 기록한다.
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

`asin`, `gtin`, `upc`, `ean`, `isbn`은 CatalogProduct 등록 후 일반 수정 API로 변경할 수 없다. 이 값들을 `PATCH /api/v1/admin/catalog-products/{catalogProductId}` 요청으로 전달하면 `400 VALIDATION_ERROR`를 반환한다.

#### 3-3-2. CatalogProduct 식별자 수정

`PATCH /api/v1/admin/catalog-products/{catalogProductId}/identifiers`

- `ADMIN`만 호출할 수 있다.
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

`POST /api/v1/admin/catalog-products/{catalogProductId}/archive`

- 요청 본문은 없다.
- `ADMIN`만 호출할 수 있다.
- 보관은 물리 삭제가 아니라 `publicationStatus = ARCHIVED`, `archivedAt = 현재 시각`으로 변경하는 상태 전이이다.
- 보관된 CatalogProduct는 공개 목록·검색에서 제외한다.
- CatalogProduct의 ProductVariant·Offer·Inventory·Media를 물리 삭제하지 않는다.
- 하위 ProductVariant는 계속 보존하지만 공개 조회·판매자 조회·Offer 등록 대상에서 제외한다.
- 하위 Offer는 모두 `INACTIVE`로 전환한다. 이미 `INACTIVE` 또는 `ARCHIVED`인 Offer는 그대로 유지한다.
- 보관된 CatalogProduct를 다시 보관하거나 일반 수정·Variant 등록의 대상으로 사용하면 `409 CATALOG_PRODUCT_ARCHIVED`를 반환한다.
- Offer 비활성화 처리는 P9의 멱등적인 보관 연계 처리로 수행하며, 연계 처리 전에도 공개 조회와 구매 검증은 상위 CatalogProduct의 `ARCHIVED` 상태를 확인한다.

성공 응답 `200` (공통 응답 envelope의 `data`):

```json
{
  "catalogProductId": "uuid",
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-09T12:10:00Z",
  "updatedAt": "2026-08-09T12:10:00Z"
}
```

#### 3-3-4. ProductVariant 보관

`POST /api/v1/admin/product-variants/{variantId}/archive`

- `ADMIN`만 호출할 수 있다.
- 물리 삭제하지 않고 `publicationStatus = ARCHIVED`, `archivedAt = 현재 시각`으로 변경한다.
- 보관된 ProductVariant는 고객 검색·상세, 판매자 CatalogProduct 조회, Offer 등록 대상에서 제외한다.
- 해당 ProductVariant의 Offer는 모두 `INACTIVE`로 전환한다.
- 이미 보관된 ProductVariant를 다시 보관하면 `409 PRODUCT_VARIANT_ARCHIVED`를 반환한다.

성공 응답 `200`:

```json
{
  "variantId": "uuid",
  "catalogProductId": "uuid",
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-09T12:10:00Z",
  "updatedAt": "2026-08-09T12:10:00Z"
}
```

#### 심화: 관리자 변경 이력

CatalogProduct, ProductVariant, Media의 생성·수정·보관은 요청 JSON에 `adminId`를 받지 않는다. 인증된 ADMIN을 변경 주체로 삼아 다음 감사 레코드를 남긴다.

```json
{
  "auditId": "uuid",
  "targetType": "CATALOG_PRODUCT",
  "targetId": "uuid",
  "adminId": "uuid",
  "action": "UPDATE",
  "changedFields": ["name", "brand"],
  "before": { "name": "무선 헤드폰", "brand": "Old Brand" },
  "after": { "name": "무선 헤드폰 2", "brand": "Example Brand" },
  "reason": "상품명·브랜드 정정",
  "createdAt": "2026-08-09T12:10:00Z"
}
```

`before`와 `after`에는 변경된 필드만 기록하며, 비밀번호·토큰 등 인증 비밀값은 기록하지 않는다. 감사 이력은 일반 고객·판매자 조회 응답에 포함하지 않는다.

### 3-4. CatalogProduct 조회

`GET /api/v1/catalog-products/{catalogProductId}`

이 API는 Offer 등록을 준비하는 `PRODUCT_MANAGER`이면서 `Seller.status = ACTIVE`인 사용자만 호출할 수 있다. 고객용 가격·Offer·Inventory·Review 조합 상세는 P9의 `GET /api/v1/product/{catalogProductId}`를 사용한다.

- 보관되지 않은 CatalogProduct와 ProductVariant만 반환한다.
- 경쟁 Seller의 Offer·가격·재고와 Review 정보는 반환하지 않는다.
- 보관된 CatalogProduct는 `404 CATALOG_PRODUCT_NOT_FOUND`를 반환한다.

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
      "media": [
        { "type": "IMAGE", "url": "https://cdn.example.com/main.jpg", "isPrimary": true }
      ]
    }
  ],
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

### 3-5. 관리자 CatalogProduct 조회

`GET /api/v1/admin/catalog-products/{catalogProductId}`

`ADMIN`만 호출할 수 있으며 보관 상태와 관리자용 변경 시각을 포함한다. Offer·Inventory 상세는 P9 관리자 운영 API를 사용한다.

응답 `200`:

```json
{
  "catalogProductId": "uuid",
  "name": "무선 헤드폰",
  "publicationStatus": "ACTIVE",
  "archivedAt": null,
  "variants": [
    {
      "variantId": "uuid",
      "sku": "HEADPHONE-BLK-001",
      "publicationStatus": "ACTIVE",
      "archivedAt": null
    }
  ],
  "createdAt": "2026-08-09T12:00:00Z",
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

### 3-6. 연계 조회

Review와 고객용 조합 상품 조회는 각각 [P10 Review](p10-review.md)와 [P9 Offer & Marketplace](p9-offer.md)에서 정의한다. P2는 CatalogProduct·ProductVariant의 메타데이터와 상품용 Media만 제공한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 검증 실패 |
| 400 | `INVALID_MEDIA` | 이미지 형식·URL·정렬 순서가 잘못됨 |
| 400 | `DUPLICATE_PRIMARY_MEDIA` | 대표 이미지가 없거나 2개 이상임 |
| 400 | `PRODUCT_CODE_ERROR` | 상품 식별자(`asin`, `gtin`, `upc`, `ean`, `isbn`) 검증 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | CatalogProduct 또는 ProductVariant 관리 권한 부족 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | CatalogProduct 없음 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant 없음 |
| 409 | `SKU_ALREADY_EXISTS` | SKU 중복 |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관된 CatalogProduct 변경 시도 |
| 409 | `PRODUCT_VARIANT_ARCHIVED` | 보관된 ProductVariant 변경 시도 |
