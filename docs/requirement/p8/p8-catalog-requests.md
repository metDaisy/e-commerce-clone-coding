# P8 CatalogRegistrationRequest API

이 문서는 판매자의 Category·CatalogProduct·ProductVariant 등록 요청과 `CatalogRegistrationRequest` 모델을 정의한다. 업무 정책은 [P8 Seller Policy](p8-policy.md), 정식 Catalog 데이터와 검증은 [P2 Catalog](../p2/p2-policy.md), 관리자 심사는 [P7 Catalog Registration Review](../p7/p7-catalog-requests.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `CatalogRegistrationRequest` | 판매자 제출 원본·심사 스냅샷·처리 상태 | 생성·목록·상세 조회 |
| `requestPayload` | 제출 당시 Catalog 입력값의 불변 JSON 스냅샷 | 각 등록 요청 생성 |
| `Category`, `CatalogProduct`, `ProductVariant` | 정식 Catalog 원본. P8은 소유하지 않음 | P2·P7 공개 계약 |

- P8은 승인 전 Catalog 원본을 생성하지 않는다.
- P2의 입력 검증과 정식 생성 규칙은 P8 문서에 복제하지 않는다.
- P7은 P8 요청을 승인·거절하고 승인 결과 ID를 기록한다.

## 2. 데이터 모델

<a id="catalogregistrationrequest"></a>
### 2-1. `CatalogRegistrationRequest`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `requestId` | UUID | 예 | 등록 요청 식별자 |
| `requestType` | ENUM | 예 | `CATEGORY`, `CATALOG_PRODUCT`, `PRODUCT_VARIANT` |
| `sellerId` | UUID | 예 | 인증된 Seller에서 설정한 판매 주체 |
| `requestedByUserId` | UUID | 예 | 실제 요청을 제출한 User |
| `targetCatalogProductId` | UUID | 조건부 | ProductVariant 요청의 대상 CatalogProduct |
| `requestPayload` | JSONB | 예 | 제출 당시 입력값의 불변 스냅샷 |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `processedByUserId` | UUID | 아니오 | 승인·거절을 처리한 ADMIN |
| `processedAt` | TIMESTAMP | 아니오 | 처리 시각 |
| `rejectionReasonCode` | VARCHAR(64) | 아니오 | 거절 사유 코드 |
| `rejectionMessage` | VARCHAR(500) | 아니오 | 판매자에게 공개할 거절 메시지 |
| `createdCategoryId` | UUID | 아니오 | 승인으로 생성된 Category |
| `createdCatalogProductId` | UUID | 아니오 | 승인으로 생성된 CatalogProduct |
| `createdVariantId` | UUID | 아니오 | 승인으로 생성된 ProductVariant |
| `createdAt` | TIMESTAMP | 예 | 요청 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 최종 변경 시각 |

### 2-2. 관계와 제약

- 요청 생성 시 `status=PENDING`이다.
- `requestPayload`는 심사 당시 입력값을 보존하며 처리 후 수정하지 않는다.
- Category 요청은 `requestPayload.name`, `parentId`를 저장한다.
- CatalogProduct 요청은 기존 `categoryId` 또는 단일 `categoryProposal`을 저장한다.
- ProductVariant 요청은 `targetCatalogProductId`를 사용하며 요청 본문에 Category를 포함하지 않는다.
- `sellerId`와 `requestedByUserId`는 인증 정보에서 설정하고 요청 본문으로 받지 않는다.
- 처리된 요청은 수정하지 않으며 재요청은 새 `CatalogRegistrationRequest`로 생성한다.
- 상태 전이와 생성 결과 ID 기록은 [P7 Catalog Registration Review](../p7/p7-catalog-requests.md)에 따른다.

## 3. API 정의

### 3-1. Category 등록 요청

`POST /api/v1/seller/category-registration-requests`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

요청:

```json
{
  "name": "스마트 조명",
  "parentId": "uuid"
}
```

Category 이름·부모·계층 검증은 [P2 Category](../p2/p2-category.md)의 공개 계약을 사용한다.

#### 성공 응답: `201 Created`

```json
{
  "requestId": "uuid",
  "requestType": "CATEGORY",
  "status": "PENDING",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `SELLER-005` | Category 입력 검증 실패 | 카테고리 정보를 확인해 주세요. | 실패 필드와 수정 방법 | P8 요청 검증 원인과 requestId |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 409 | `SELLER-009` | 동일 대상의 처리 중 요청 존재 | 처리 중인 등록 요청이 있습니다. | 대상 식별자는 노출하지 않음 | 충돌 요청과 대상 식별자 |

### 3-2. CatalogProduct 등록 요청

`POST /api/v1/seller/catalog-product-registration-requests`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

요청:

```json
{
  "name": "무선 헤드폰",
  "description": "노이즈 캔슬링 무선 헤드폰",
  "brand": "Example",
  "categoryId": "uuid",
  "attributes": {
    "connectionType": "BLUETOOTH",
    "noiseCancellation": true
  },
  "gtin": "8801234567890"
}
```

새 Category가 필요하면 `categoryProposal`을 하나만 포함할 수 있다. P2 CatalogProduct의 식별자·attributes 검증 계약을 따른다.

```json
{
  "name": "스마트 조명",
  "description": "앱으로 제어하는 조명",
  "brand": "Example",
  "gtin": "8801234567890",
  "categoryProposal": {
    "name": "스마트 조명",
    "parentId": "uuid"
  }
}
```

#### 성공 응답: `201 Created`

```json
{
  "requestId": "uuid",
  "requestType": "CATALOG_PRODUCT",
  "status": "PENDING",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `SELLER-005` | 요청 필드 검증 실패 | 상품 정보를 확인해 주세요. | 실패 필드와 수정 방법 | P8 요청 검증 원인과 requestId |
| 400 | [CATALOG-006](../p2/p2-catalog-product.md) | — | — | — | — |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 409 | `SELLER-009` | 동일 대상의 처리 중 요청 존재 | 처리 중인 등록 요청이 있습니다. | 대상 식별자는 노출하지 않음 | 충돌 요청과 대상 식별자 |

### 3-3. ProductVariant 등록 요청

`POST /api/v1/seller/catalog-products/{catalogProductId}/variant-registration-requests`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

요청:

```json
{
  "displayName": "블랙 / 대형",
  "attributes": {
    "color": "Black",
    "size": "L",
    "weight": { "value": 350, "unit": "g" },
    "packageSize": { "width": 200, "height": 180, "depth": 80, "unit": "mm" }
  }
}
```

`displayName`과 `attributes`의 상세 검증은 [P2 ProductVariant](../p2/p2-product-variant.md)를 사용한다. 검증 성공만으로 Catalog를 예약·생성하지 않는다.

#### 성공 응답: `201 Created`

```json
{
  "requestId": "uuid",
  "requestType": "PRODUCT_VARIANT",
  "targetCatalogProductId": "uuid",
  "status": "PENDING",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `SELLER-005` | Variant 입력 검증 실패 | Variant 정보를 확인해 주세요. | 실패 필드와 수정 방법 | P8 요청 검증 원인과 requestId |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 404 | [CATALOG-003](../p2/p2-catalog-product.md) | — | — | — | — |
| 409 | `SELLER-009` | 동일 대상의 처리 중 요청 존재 | 처리 중인 등록 요청이 있습니다. | 대상 식별자는 노출하지 않음 | 충돌 요청과 대상 식별자 |

### 3-4. 내 Catalog 등록 요청 목록

`GET /api/v1/seller/catalog-registration-requests?page=0&size=20`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

- 인증된 Seller의 요청만 반환한다.
- 기본 정렬은 `createdAt DESC, requestId DESC`이며 공통 페이지 형식을 사용한다.

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "requestId": "uuid",
      "requestType": "CATALOG_PRODUCT",
      "status": "PENDING",
      "createdAt": "2026-08-16T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |

### 3-5. Catalog 등록 요청 상세

`GET /api/v1/seller/catalog-registration-requests/{requestId}`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

#### 성공 응답: `200 OK`

```json
{
  "requestId": "uuid",
  "requestType": "CATALOG_PRODUCT",
  "seller": {
    "sellerId": "uuid",
    "displayName": "Example Store",
    "businessName": "Example Inc."
  },
  "requester": {
    "userId": "uuid",
    "name": "Daisy"
  },
  "requestPayload": {
    "name": "무선 헤드폰",
    "brand": "Example"
  },
  "status": "PENDING",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](p8-seller-profile.md) | — | — | — | — |
| 404 | `SELLER-008` | 요청이 존재하지 않음 | 등록 요청을 찾을 수 없습니다. | 없음 | requestId와 조회 원인 |
