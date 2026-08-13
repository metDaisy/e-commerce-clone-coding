# P7 Admin & Operations (관리자·운영)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P7은 관리자 전용 진입점과 운영 기능을 정의한다. Catalog·Offer·Review·쿠폰·주문·이벤트 데이터의 소유권은 각각 P2~P6, P9, P10에 유지하며, P7은 공개된 application interface를 통해 해당 기능을 호출한다.

- 모든 P7 API는 `ADMIN`만 호출할 수 있다.
- `USER`는 구매자 기능을 사용하며 P7 API에 접근할 수 없다.
- `PRODUCT_MANAGER`는 P8 Seller API와 P9 Offer·Inventory API를 사용한다. P7 API에는 접근할 수 없다.
- 플랫폼 전체 운영 기능은 `ADMIN`만 수행한다. `PRODUCT_MANAGER`는 플랫폼 운영자가 아니라 입점 판매자다.
- 로그인하지 않았거나 권한이 없으면 각각 `401 AUTHENTICATION_REQUIRED`, `403 ACCESS_DENIED`를 반환한다.
- 관리자가 자기 자신의 `ADMIN` 권한을 해제하는 요청은 거부한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| PATCH | `/api/v1/admin/users/{userId}/role` | `ADMIN` | 사용자 권한 변경 |
| PATCH | `/api/v1/admin/seller-applications/{sellerId}/status` | `ADMIN` | 판매자 신청 승인·거절·정지 |
| POST | `/api/v1/admin/categories` | `ADMIN` | 카테고리 생성 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 카테고리 수정 |
| GET | `/api/v1/admin/catalog-products` | `ADMIN` | CatalogProduct·ProductVariant 관리 목록 |
| PATCH | `/api/v1/admin/offers/{offerId}/status` | `ADMIN` | Offer 활성·비활성 |
| PATCH | `/api/v1/admin/offers/{offerId}/price` | `ADMIN` | Offer 가격 운영 수정 |
| POST | `/api/v1/admin/offers/{offerId}/inventory-adjustments` | `ADMIN` | Offer 재고 운영 조정 |
| POST | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 생성 |
| PATCH | `/api/v1/admin/coupons/{couponId}` | `ADMIN` | 쿠폰 수정·비활성화 |
| GET | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 관리 목록 |
| PATCH | `/api/v1/admin/deliveries/{deliveryId}/status` | `ADMIN` | 배송 상태 변경 |
| GET | `/api/v1/admin/outbox/events` | `ADMIN` | 이벤트 처리 현황 |
| GET | `/api/v1/admin/outbox/events/{eventId}` | `ADMIN` | 이벤트 상세 |
| POST | `/api/v1/admin/outbox/events/{eventId}/retry` | `ADMIN` | 실패 이벤트 재처리 |
| GET | `/api/v1/admin/sagas/{sagaId}` | `ADMIN` | Saga 상태 조회 |
| POST | `/api/v1/admin/sagas/{sagaId}/retry` | `ADMIN` | Saga 보상 재시도 |

CatalogProduct·ProductVariant의 생성·수정·보관은 P2 Catalog 관리자 API에서 `ADMIN`만 수행한다. `PRODUCT_MANAGER`는 P9에서 자신의 Offer·Inventory만 관리하고, P7은 플랫폼 운영자인 `ADMIN`의 관리 진입점을 정의한다.

## 3. 요구사항

### 3-1. 사용자 권한 변경

`PATCH /api/v1/admin/users/{userId}/role`

요청:

```json
{
  "role": "PRODUCT_MANAGER"
}
```

- 허용 역할은 `USER`, `PRODUCT_MANAGER`, `ADMIN`이다.
- `PRODUCT_MANAGER`는 활성 `Seller`을 가진 판매자에게만 부여할 수 있다.
- 판매자 신청 승인 시 `Seller.status`를 `ACTIVE`로 변경하고 사용자의 역할을 `PRODUCT_MANAGER`로 변경한다.
- 판매자 정지 또는 승인 취소 시 판매자 API를 차단하고 역할을 `USER`로 되돌린다.
- 요청자는 자신의 `ADMIN` 권한을 해제할 수 없다.
- 대상 사용자가 없으면 `USER_NOT_FOUND`를 반환한다.
- 변경 결과는 다음과 같이 반환한다.

```json
{
  "userId": "uuid",
  "role": "PRODUCT_MANAGER",
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 카테고리 관리

카테고리는 관리자가 관리하는 메타데이터다. 공개 사용자는 `GET /api/v1/categories`로 조회만 할 수 있으며, 생성·수정은 다음 `ADMIN` 전용 API로 수행한다. 기본 요구사항에서는 카테고리 삭제 API를 제공하지 않는다.

#### 카테고리 생성

`POST /api/v1/admin/categories`

요청:

```json
{
  "name": "노트북",
  "parentId": "uuid"
}
```

- `name`은 공백만으로 구성할 수 없다.
- `parentId`가 없으면 루트 카테고리로 생성하고 `depth = 1`로 저장한다.
- `parentId`가 있으면 부모 카테고리가 존재해야 하며 `depth = parent.depth + 1`로 저장한다.
- 부모 카테고리는 하나만 지정할 수 있으며, 서버는 부모 연결을 검증해 순환 참조가 생기지 않도록 한다.
- `depth`는 `1~3`만 허용한다.

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

#### 카테고리 수정

`PATCH /api/v1/admin/categories/{categoryId}`

요청:

```json
{
  "name": "노트북·태블릿",
  "parentId": "uuid"
}
```

- 전달된 필드만 수정하고 전달되지 않은 필드는 유지한다.
- `parentId: null`은 루트 카테고리로 이동한다. `parentId`를 생략하면 기존 부모를 유지한다.
- `parentId` 변경 시 자기 자신이나 자신의 하위 카테고리를 부모로 지정할 수 없다.
- 서버는 변경 후 부모를 따라가며 순환 참조가 발생하지 않는지 검증한다.
- 수정 후 하위 카테고리 전체의 `depth`가 `1~3`을 벗어나면 요청을 거부한다.
- 카테고리와 하위 카테고리의 `parentId`, `depth` 변경은 하나의 트랜잭션으로 처리한다.

성공 응답 `200`:

```json
{
  "categoryId": "uuid",
  "name": "노트북·태블릿",
  "parentId": "uuid",
  "depth": 3,
  "updatedAt": "2026-08-09T12:05:00Z"
}
```

### 3-3. CatalogProduct 관리 목록

`GET /api/v1/admin/catalog-products`

P2가 소유한 CatalogProduct를 관리자 화면에서 조회하기 위한 P7 관리자 전용 진입점이다. 일반 사용자의 상품 탐색에는 사용하지 않으며, 상품 검색은 P9의 `GET /api/v1/product/search`에서 제공한다.

지원 Query:

```text
page=0
size=20
keyword=headphone (상품명·설명·브랜드·식별자·SKU·Variant 표시명)
categoryId=uuid
publicationStatus=ACTIVE|ARCHIVED
sort=LATEST|NAME_ASC|NAME_DESC
```

- `page`는 0부터 시작하고 기본 `size`는 20, 최대 `size`는 100이다.
- `ADMIN`은 모든 CatalogProduct와 ProductVariant를 조회할 수 있으며 보관된 항목도 조회한다.
- 기본 정렬은 `LATEST`이며 `createdAt DESC, catalogProductId DESC`를 사용한다.
- `categoryId`는 하위 카테고리를 포함한다.
- 이 목록은 판매자용 목록과 달리 보관 상태·관리 메타데이터를 포함한다. `managerId`는 저장하거나 반환하지 않는다.

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
      "publicationStatus": "ACTIVE",
      "archivedAt": null,
      "createdAt": "2026-08-09T12:00:00Z",
      "updatedAt": "2026-08-09T12:05:00Z",
      "variants": [
        {
          "variantId": "uuid",
          "sku": "HEADPHONE-BLK-001",
          "displayName": "블랙",
          "publicationStatus": "ACTIVE",
          "archivedAt": null,
          "createdAt": "2026-08-09T12:01:00Z",
          "updatedAt": "2026-08-09T12:01:00Z"
        }
      ]
    }
  ]
}
```

관리자 목록은 CatalogProduct·ProductVariant의 관리 상태를 확인하는 조회이며 Offer 가격·재고의 상세는 P9의 Offer·Inventory 관리 API에서 조회한다.

### 3-4. Offer 활성·비활성

`PATCH /api/v1/admin/offers/{offerId}/status`

요청:

```json
{
  "status": "INACTIVE"
}
```

- `ADMIN`만 호출할 수 있다.
- `status`는 `ACTIVE` 또는 `INACTIVE`만 허용한다. 보관은 삭제가 아니므로 `ARCHIVED` Offer를 다시 활성화할 수 없다.
- `ACTIVE` 전환은 Seller, CatalogProduct, ProductVariant가 모두 활성인 경우에만 허용한다.
- CatalogProduct 또는 ProductVariant가 보관되면 연결된 Offer는 모두 `INACTIVE`가 되며, 이 API로도 다시 활성화할 수 없다.

응답 `200`:

```json
{
  "offerId": "uuid",
  "variantId": "uuid",
  "sellerId": "uuid",
  "status": "INACTIVE",
  "updatedAt": "2026-08-10T12:00:00Z"
}
```

### 3-5. 판매자 신청 관리

`PATCH /api/v1/admin/seller-applications/{sellerId}/status`

요청:

```json
{
  "status": "ACTIVE",
  "reason": "사업자 정보 확인 완료"
}
```

- `PENDING → ACTIVE` 또는 `PENDING → REJECTED` 전환을 허용한다.
- `ACTIVE → SUSPENDED` 전환으로 판매자 Offer 등록·수정을 차단한다.
- 승인·정지 이력과 처리 관리자를 기록한다.

### 3-6. 쿠폰 관리

- 쿠폰 생성·수정·비활성화·관리 목록의 상세 규칙은 [P4 쿠폰 요구사항](p4-coupon.md)을 따른다.
- 쿠폰 발급·내 쿠폰 조회는 구매자 기능이므로 P4에 남긴다.

### 3-7. 배송 운영

`PATCH /api/v1/admin/deliveries/{deliveryId}/status`의 상태 전이와 응답은 [P5 배송 요구사항](p5-order-payment-delivery.md#2-6-배송)을 따른다.

- `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED` 순서만 허용한다.
- `SHIPPED` 전환 시 운송장 번호가 필요하다.
- 배송 상태 변경은 관리자만 수행한다.

### 3-8. 이벤트·Saga 운영

- Outbox 조회·실패 이벤트 재처리·Saga 조회·보상 재시도는 P6의 상태 전이와 멱등성 규칙을 따른다.
- 이미 성공 처리된 이벤트 또는 재시도할 수 없는 Saga는 다시 처리하지 않는다.
- 운영 API 응답에는 비밀번호·토큰·OAuth secret 등 민감 정보를 포함하지 않는다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 또는 역할 값 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | ADMIN 권한 부족 |
| 403 | `CANNOT_CHANGE_OWN_ADMIN_ROLE` | 자기 자신의 ADMIN 권한 변경 시도 |
| 400 | `INVALID_CATEGORY_PARENT` | 부모가 없거나 자기 자신·하위 카테고리를 부모로 지정함 |
| 400 | `CATEGORY_CYCLE_DETECTED` | 부모 연결에서 순환 참조가 발생함 |
| 400 | `CATEGORY_DEPTH_EXCEEDED` | 카테고리 깊이가 3단계를 초과함 |
| 400 | `CATEGORY_NAME_INVALID` | 카테고리 이름이 비어 있거나 공백뿐임 |
| 400 | `CATEGORY_UPDATE_EMPTY` | 수정할 카테고리 필드가 없음 |
| 404 | `USER_NOT_FOUND` | 사용자 없음 |
| 404 | `SELLER_NOT_FOUND` | 판매자 프로필 없음 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `COUPON_NOT_FOUND` | 쿠폰 없음 |
| 404 | `DELIVERY_NOT_FOUND` | 배송 없음 |
| 404 | `OUTBOX_EVENT_NOT_FOUND` | 이벤트 없음 |
| 404 | `SAGA_NOT_FOUND` | Saga 없음 |
| 409 | `CATEGORY_HAS_CHILDREN` | 하위 카테고리 또는 상품 존재 |
| 409 | `EVENT_ALREADY_PUBLISHED` | 발행 완료 이벤트 재처리 |
| 409 | `SAGA_NOT_RETRYABLE` | 재시도할 수 없는 Saga 상태 |
