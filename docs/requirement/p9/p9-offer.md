# P9 Offer API

이 문서는 `Offer` 데이터 모델과 Offer를 조작·조회하는 API를 정의한다. 업무 정책은 [P9 Policy](p9-policy.md), Inventory API는 [P9 Inventory API](p9-inventory.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Offer` | Seller별 ProductVariant 판매 가격·상태·구매 제한 | 목록·상세·생성·상태 변경·가격 수정 |
| `OfferStatusHistory` | Offer 상태 변경 감사 이력 | 상태 변경 API에서 함께 생성 |
| `OfferActivationRequest` | 관리자 차단 Offer의 재활성화 요청·처리 결과 | 활성화 요청 생성 |
| `OfferMedia` | Seller Offer 소개 Media attachment | Media 등록·수정·보관 |
| `Inventory` | Offer별 구매 가능 수량 | [P9 Inventory API](p9-inventory.md) |

- P9가 소유하는 모델과 P2·P8·P10의 외부 모델을 구분한다.
- `sellerId`, `variantId`는 외부 도메인 식별자에 대한 논리 참조이며 외부 도메인의 필드를 복제하지 않는다.
- [P9 Policy](p9-policy.md)의 불변식은 생성·수정 API와 상태 전환에서 검증한다.

## 2. 데이터 모델

### 2-1. Offer

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `offerId` | UUID | 예 | Offer 식별자 |
| `sellerId` | UUID | 예 | Offer 소유 Seller의 논리 참조 |
| `variantId` | UUID | 예 | 판매 대상 ProductVariant의 논리 참조 |
| `basePrice` | Money | 예 | 할인 전 기본 가격. `amount`는 0 이상 |
| `discountPrice` | Money | 아니오 | 할인 기간에 적용할 가격 |
| `discountStartAt` | TIMESTAMP | 아니오 | 할인 시작 시각 |
| `discountEndAt` | TIMESTAMP | 아니오 | 할인 종료 시각 |
| `maxPurchaseQuantity` | INTEGER | 예 | 고객 한 명이 하나의 주문에서 구매할 수 있는 최대 수량 |
| `status` | ENUM | 예 | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `inactiveSource` | ENUM | 아니오 | `SELLER`, `ADMIN`, `SYSTEM`. `INACTIVE`일 때 사용 |
| `inactiveReasonCode` | VARCHAR(64) | 아니오 | 현재 비활성화 사유 코드 |
| `sellerMessage` | VARCHAR(500) | 아니오 | Seller에게 공개할 비활성화 사유·해결 안내 |
| `statusChangedAt` | TIMESTAMP | 예 | 현재 상태로 변경된 시각 |
| `createdAt` | TIMESTAMP | 예 | 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 최종 변경 시각 |

`Money`는 `amount`와 ISO 4217 `currency`의 조합이다. `appliedPrice`와 `discountRate`는 저장 필드가 아니라 현재 시각과 가격 기간에서 계산한다. `inventoryQuantity`와 `availabilityStatus`는 [P9 Inventory API](p9-inventory.md)의 값을 조합한 조회 필드다.

### 2-2. OfferStatusHistory

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `historyId` | UUID | 예 | 상태 변경 이력 식별자 |
| `offerId` | UUID | 예 | 대상 Offer |
| `fromStatus` | ENUM | 예 | 변경 전 상태 |
| `toStatus` | ENUM | 예 | 변경 후 상태 |
| `inactiveSource` | ENUM | 아니오 | `SELLER`, `ADMIN`, `SYSTEM`. 비활성화 전환이 아니면 null |
| `inactiveReasonCode` | VARCHAR(64) | 아니오 | 비활성화 사유. 활성화·보관 전환에서는 null 가능 |
| `sellerMessage` | VARCHAR(500) | 아니오 | 당시 Seller에게 공개한 안내 문구 |
| `changedByUserId` | UUID | 아니오 | 변경 User. 시스템 변경이면 null |
| `activationRequestId` | UUID | 아니오 | 활성화 요청 승인으로 변경된 경우의 요청 ID |
| `changedAt` | TIMESTAMP | 예 | 상태 변경 시각 |

모든 상태 변경마다 이력을 추가하며 기존 이력은 수정·삭제하지 않는다. `changedByUserId`는 Seller 응답에 포함하지 않는다.

### 2-3. OfferActivationRequest

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `requestId` | UUID | 예 | 활성화 요청 식별자 |
| `offerId` | UUID | 예 | 활성화 대상 Offer |
| `sellerId` | UUID | 예 | 요청 Seller |
| `requestedByUserId` | UUID | 예 | 실제 요청을 제출한 User |
| `resolutionMessage` | VARCHAR(1000) | 예 | Seller가 해결한 조치 설명 |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `processedByUserId` | UUID | 아니오 | 심사를 처리한 ADMIN |
| `processedAt` | TIMESTAMP | 아니오 | 심사 처리 시각 |
| `rejectionReasonCode` | VARCHAR(64) | 아니오 | 거절 사유 코드 |
| `rejectionMessage` | VARCHAR(500) | 아니오 | 거절 시 Seller에게 공개할 메시지 |
| `createdAt` | TIMESTAMP | 예 | 요청 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 요청 최종 변경 시각 |

### 2-4. OfferMedia

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `mediaId` | UUID | 예 | Media attachment 식별자 |
| `offerId` | UUID | 예 | 대상 Offer |
| `type` | ENUM | 예 | 현재 `IMAGE` |
| `url` | URI | 예 | CDN 또는 Media Storage 접근 URL |
| `sortOrder` | INTEGER | 예 | Offer 내 표시 순서 |
| `isPrimary` | BOOLEAN | 예 | 대표 이미지 여부 |
| `status` | ENUM | 예 | `ACTIVE`, `ARCHIVED` |
| `createdAt` | TIMESTAMP | 예 | 연결 시각 |
| `updatedAt` | TIMESTAMP | 예 | 최종 변경 시각 |
| `archivedAt` | TIMESTAMP | 아니오 | 보관 시각 |

### 2-5. 관계와 제약

- `(sellerId, variantId)`는 UNIQUE다. 하나의 Seller는 같은 ProductVariant에 Offer를 하나만 가진다.
- Offer 생성 시 Inventory를 `quantity = 0`으로 함께 생성한다. Inventory의 상세 모델과 조정 API는 [P9 Inventory API](p9-inventory.md)에 둔다.
- `maxPurchaseQuantity`는 1 이상의 정수다.
- 할인 가격이 있으면 기본 가격보다 작아야 하고, 할인 시작·종료 시각을 함께 가진다. `discountPrice = null`이면 할인 기간도 null이다.
- 하나의 Offer에는 Active 상태의 대표 Media를 최대 하나만 둘 수 있다.
- `ARCHIVED` Offer와 `ARCHIVED` OfferMedia는 다시 활성화하지 않는다.
- Offer가 `ACTIVE`·`INACTIVE`·`ARCHIVED`로 변경될 때 `OfferStatusHistory`를 추가한다.
- `ADMIN` 비활성화 Offer에는 `PENDING` 활성화 요청을 Offer당 하나만 허용한다.

## 3. API 정의

### 3-1. 내 Offer 목록 조회

`GET /api/v1/seller/offers`

권한: 인증된 Seller 소유자. Seller 상태가 `ACTIVE` 또는 `SUSPENDED`이면 조회할 수 있다.

기본 조회에는 본인 Seller의 `ACTIVE`, `INACTIVE`, `ARCHIVED` Offer를 모두 포함한다. `sellerId`는 요청으로 받지 않고 인증 주체에서 결정한다.

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "offerId": "uuid",
      "name": "무선 헤드폰",
      "variantDisplayName": "블랙",
      "status": "INACTIVE",
      "basePrice": { "amount": 49900.00, "currency": "KRW" },
      "appliedPrice": { "amount": 49900.00, "currency": "KRW" },
      "inventoryQuantity": 0,
      "availabilityStatus": "OUT_OF_STOCK",
      "inactiveSource": "ADMIN",
      "inactiveReasonCode": "POLICY_VIOLATION",
      "sellerMessage": "상품 설명을 수정해 주세요.",
      "statusChangedAt": "2026-08-16T12:00:00Z"
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
| 403 | [SELLER-001](../p8/p8-seller-profile.md) | — | — | — | — |

### 3-2. 내 Offer 상세 조회

`GET /api/v1/seller/offers/{offerId}`

권한: 인증된 Seller 소유자. `SUSPENDED` Seller도 본인 Offer를 조회할 수 있다.

#### 성공 응답: `200 OK`

목록 항목의 필드에 `media`, `maxPurchaseQuantity`, 활성화 요청 상태를 추가해 반환한다. 내부 `catalogProductId`, `variantId`, `changedByUserId`는 Seller 응답에 포함하지 않는다.

```json
{
  "offerId": "uuid",
  "status": "ACTIVE",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": null,
  "appliedPrice": { "amount": 49900.00, "currency": "KRW" },
  "maxPurchaseQuantity": 5,
  "inventoryQuantity": 20,
  "availabilityStatus": "IN_STOCK",
  "media": [],
  "activationRequestStatus": null
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |

### 3-3. Offer 생성

`POST /api/v1/seller/offers`

권한: `PRODUCT_MANAGER`이면서 `ACTIVE Seller`인 User.

요청:

```json
{
  "variantId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "maxPurchaseQuantity": 5
}
```

`sellerId`는 요청 본문으로 받지 않는다. P9는 P2의 공개 Catalog 계약으로 ACTIVE ProductVariant를 확인한다.

#### 성공 응답: `201 Created`

```json
{
  "offerId": "uuid",
  "sellerId": "uuid",
  "variantId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "maxPurchaseQuantity": 5,
  "inventoryQuantity": 0,
  "status": "ACTIVE",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `OFFER-002` | 필수 필드·수량 형식 오류 | 입력값을 확인해 주세요. | 실패 필드와 수정 방법 | Offer 생성 입력 검증 실패 |
| 400 | `OFFER-003` | 가격이 음수이거나 통화 형식이 잘못됨 | 가격을 확인해 주세요. | 가격 필드 | Offer 가격 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | [CATALOG-031](../p2/p2-product-variant.md) | — | — | — | — |
| 409 | [CATALOG-019](../p2/p2-catalog-product.md) | — | — | — | — |
| 409 | [CATALOG-033](../p2/p2-product-variant.md) | — | — | — | — |
| 409 | `OFFER-004` | 같은 Seller·ProductVariant Offer가 이미 있음 | 이미 등록한 판매 조건입니다. | 없음 | Offer 유일성 충돌 |

### 3-4. Offer 상태 수정

`PATCH /api/v1/seller/offers/{offerId}`

권한: Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`.

요청:

```json
{ "status": "INACTIVE" }
```

`ACTIVE` 전환은 Seller, CatalogProduct, ProductVariant가 모두 활성인 경우에만 허용한다. Seller가 직접 비활성화하면 `inactiveSource=SELLER`, `inactiveReasonCode=SELLER_REQUEST`를 기록한다. 관리자 차단 Offer는 Seller가 직접 활성화할 수 없다.

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "sellerId": "uuid",
  "status": "INACTIVE",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `OFFER-010` | 허용하지 않는 상태 값 | 상태값을 확인해 주세요. | `status` 필드 | Offer 상태 입력 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |
| 409 | `OFFER-005` | 보관된 Offer 변경 | 보관된 판매 조건은 변경할 수 없습니다. | 없음 | Offer terminal 상태 충돌 |
| 409 | `OFFER-006` | 관리자 차단 Offer를 직접 활성화 | 관리자 확인이 필요한 판매 조건입니다. | 없음 | 관리자 차단 우회 시도 |
| 409 | `OFFER-007` | Seller·CatalogProduct·ProductVariant 중 하나가 비활성 | 관련 상품 또는 판매자 상태를 확인해 주세요. | 없음 | Offer 활성화 의존 상태 실패 |

### 3-5. Offer 보관

`DELETE /api/v1/seller/offers/{offerId}`

권한: Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`. 물리 삭제하지 않고 `ARCHIVED`로 변경한다.

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "status": "ARCHIVED",
  "archivedAt": "2026-08-16T12:00:00Z",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |
| 409 | `OFFER-005` | 이미 보관된 Offer | 이미 보관된 판매 조건입니다. | 없음 | Offer terminal 상태 충돌 |

### 3-6. 관리자 차단 Offer 활성화 요청

`POST /api/v1/seller/offers/{offerId}/activation-requests`

권한: Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`.

요청:

```json
{
  "resolutionMessage": "상품 설명에서 문제 표현을 제거하고 관련 증빙을 보완했습니다."
}
```

#### 성공 응답: `201 Created`

```json
{
  "requestId": "uuid",
  "offerId": "uuid",
  "status": "PENDING",
  "createdAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `OFFER-011` | `resolutionMessage` 누락 또는 형식 오류 | 해결 조치 설명을 입력해 주세요. | 실패 필드 | 활성화 요청 입력 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |
| 409 | `OFFER-005` | 보관된 Offer | 보관된 판매 조건은 활성화를 요청할 수 없습니다. | 없음 | Offer terminal 상태 충돌 |
| 409 | `OFFER-008` | 동일 Offer에 PENDING 요청이 있음 | 이미 처리 중인 요청이 있습니다. | 없음 | 활성화 요청 유일성 충돌 |

관리자 승인·거절 API는 [P7 Offer API](../p7/p7-offer.md)에서 정의한다. 승인 시 요청은 `APPROVED`, Offer는 `ACTIVE`가 되고, 거절 시 요청은 `REJECTED`, Offer는 `INACTIVE`로 유지한다.

### 3-7. Offer 가격 수정

판매자:

`PATCH /api/v1/seller/offers/{offerId}/price`

관리자:

`PATCH /api/v1/admin/offers/{offerId}/price`

권한: 판매자 API는 Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`, 관리자 API는 `ADMIN`.

요청:

```json
{
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": { "amount": 44900.00, "currency": "KRW" },
  "discountStartAt": "2026-08-10T00:00:00Z",
  "discountEndAt": "2026-08-31T23:59:59Z"
}
```

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "basePrice": { "amount": 49900.00, "currency": "KRW" },
  "discountPrice": { "amount": 44900.00, "currency": "KRW" },
  "appliedPrice": { "amount": 44900.00, "currency": "KRW" },
  "discountRate": 10.02,
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `OFFER-003` | 음수·통화 변경·할인 가격이 기본 가격 이상 | 가격과 할인 기간을 확인해 주세요. | 실패 가격 필드 | Offer 가격 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |
| 409 | `OFFER-005` | 보관된 Offer 가격 수정 | 보관된 판매 조건은 변경할 수 없습니다. | 없음 | Offer terminal 상태 충돌 |

### 3-8. Offer Media 등록·수정·보관

등록:

`POST /api/v1/seller/offers/{offerId}/media`

수정:

`PATCH /api/v1/seller/offers/{offerId}/media/{mediaId}`

보관:

`DELETE /api/v1/seller/offers/{offerId}/media/{mediaId}`

권한: Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`.

요청:

```json
{
  "type": "IMAGE",
  "url": "https://cdn.example.com/seller-offer-main.jpg",
  "sortOrder": 1,
  "isPrimary": true
}
```

실제 파일 업로드·CDN 저장·물리 삭제는 공통 `MediaStoragePort`에 위임한다. `isPrimary = true`인 Active Media는 Offer당 최대 하나다.

#### 성공 응답: `201 Created` / `200 OK`

```json
{
  "mediaId": "uuid",
  "offerId": "uuid",
  "type": "IMAGE",
  "url": "https://cdn.example.com/seller-offer-main.jpg",
  "sortOrder": 1,
  "isPrimary": true,
  "status": "ACTIVE"
}
```

보관 성공 응답은 `200 OK`이며 `mediaId`, `offerId`, `status=ARCHIVED`, `archivedAt`을 반환한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `OFFER-012` | Media 필드 오류 또는 대표 Media 중복 | Media 정보를 확인해 주세요. | 실패 필드 | Offer Media 입력 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | `OFFER-001` | Offer가 존재하지 않음 | 판매 조건을 찾을 수 없습니다. | 없음 | Offer 식별자 조회 실패 |
| 404 | `OFFER-009` | Media가 존재하지 않음 | Media를 찾을 수 없습니다. | 없음 | Offer Media 식별자 조회 실패 |
| 409 | `OFFER-005` | 보관된 Offer의 Media 변경 | 보관된 판매 조건은 변경할 수 없습니다. | 없음 | Offer terminal 상태 충돌 |
