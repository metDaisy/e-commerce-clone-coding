# P7 CatalogRegistrationRequest API

이 문서는 P8 `CatalogRegistrationRequest`를 관리자 화면에서 조회·승인·거절하는 API를 정의한다. Catalog 정책은 [P7 Catalog Administration Policy](p7-catalog.md), 원본 모델은 [P8 Catalog 등록 요청](../p8/p8-catalog-requests.md)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `CatalogRegistrationRequest` | P8이 요청 payload·상태·처리 결과를 소유한다. | 관리자 목록·승인·거절 |
| `Category` | P2가 승인 시 생성 결과와 검증을 소유한다. | Category 요청 승인 |
| `CatalogProduct` | P2가 승인 시 생성 결과와 검증을 소유한다. | CatalogProduct 요청 승인 |
| `ProductVariant` | P2가 승인 시 생성 결과와 검증을 소유한다. | ProductVariant 요청 승인 |

P7은 요청을 별도 모델로 복제하지 않는다. 목록·상세·처리는 P8 원본 요청을 직접 조회한다.

## 2. 데이터 모델

### 2-1. CatalogRegistrationRequest 참조

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `requestId` | UUID | 예 | 등록 요청 식별자 |
| `requestType` | ENUM | 예 | `CATEGORY`, `CATALOG_PRODUCT`, `PRODUCT_VARIANT` |
| `sellerId` | UUID | 예 | 요청을 제출한 활성 Seller |
| `requestedByUserId` | UUID | 예 | 실제 제출 User |
| `targetCatalogProductId` | UUID | 조건부 | Variant 등록 대상 CatalogProduct |
| `requestPayload` | JSONB | 예 | 제출 당시의 회사·상품·Variant·Category 제안 snapshot |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `processedByUserId` | UUID | 아니오 | 처리한 ADMIN |
| `processedAt` | TIMESTAMP | 아니오 | 처리 시각 |
| `rejectionReasonCode` | VARCHAR(64) | 아니오 | 거절 사유 코드 |
| `rejectionMessage` | VARCHAR(1000) | 아니오 | 판매자 공개 거절 메시지 |
| `createdCategoryId` | UUID | 아니오 | Category 승인 결과 |
| `createdCatalogProductId` | UUID | 아니오 | CatalogProduct 승인 결과 |
| `createdVariantId` | UUID | 아니오 | ProductVariant 승인 결과 |
| `createdAt`, `updatedAt` | TIMESTAMP | 예 | 생성·최종 변경 시각 |

### 2-2. 관계와 제약

- `sellerId`와 `requestedByUserId`는 요청 본문이 아니라 인증 User와 연결된 활성 Seller에서 결정한다.
- `PRODUCT_VARIANT` 요청은 `targetCatalogProductId`가 필수다.
- 같은 대상의 `PENDING` 요청은 하나만 허용한다.
- 승인 시 생성 결과 ID를 기록하고, 승인된 요청은 수정·재처리하지 않는다.
- 거절된 요청은 이력으로 보존하고 새 요청으로 재신청한다.

## 3. API 정의

### 3-1. 등록 요청 목록

`GET /api/v1/admin/catalog-registration-requests`

권한: `ADMIN`

Query:

```text
page=0
size=20
status=PENDING|APPROVED|REJECTED
requestType=CATEGORY|CATALOG_PRODUCT|PRODUCT_VARIANT
sellerId=uuid
```

기본 정렬은 `createdAt DESC, requestId DESC`이다. 목록에는 요청 ID·유형·Seller·요청자·요청 요약·상태·제출 시각을 표시한다.

#### 성공 응답: `200 OK`

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "data": [
    {
      "requestId": "uuid",
      "requestType": "CATALOG_PRODUCT",
      "sellerId": "uuid",
      "requestedByUserId": "uuid",
      "summary": { "name": "무선 헤드폰", "brand": "Example Brand" },
      "status": "PENDING",
      "createdAt": "2026-08-16T12:00:00Z"
    }
  ]
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `ADMIN-004` | page·size·filter 값이 유효하지 않음 | 목록 조회 조건을 확인해 주세요. | 실패 query | 입력값과 검증 원인 |
| 404 | `ADMIN-013` | sellerId가 존재하지 않음 | 판매자를 찾을 수 없습니다. | sellerId | Seller 식별자 |

### 3-2. 등록 요청 승인

`POST /api/v1/admin/catalog-registration-requests/{requestId}/approve`

권한: `ADMIN`

요청 본문은 없다. `PENDING` 요청만 승인할 수 있으며, requestType에 따라 P2 공개 application interface를 호출한다.

#### 성공 응답: `200 OK`

```json
{
  "requestId": "uuid",
  "requestType": "CATALOG_PRODUCT",
  "status": "APPROVED",
  "createdCatalogProductId": "uuid",
  "processedByUserId": "uuid",
  "processedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. P2 생성·검증 예외는 [P2 Catalog](../p2/p2-catalog.md)의 원본 코드를 참조한다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-016` | 요청이 존재하지 않음 | 등록 요청을 찾을 수 없습니다. | 없음 | 요청 식별자 |
| 409 | `ADMIN-017` | 처리된 요청을 다시 승인 | 이미 처리된 등록 요청입니다. | status | 요청 상태와 식별자 |
| 409 | `ADMIN-018` | P2 생성 검증·중복 충돌 | 등록 요청을 승인할 수 없습니다. | 원본 오류 details | P2 원본 코드·요청 식별자 |

### 3-3. 등록 요청 거절

`POST /api/v1/admin/catalog-registration-requests/{requestId}/reject`

권한: `ADMIN`

요청:

```json
{
  "reasonCode": "DUPLICATE_CATALOG_ENTRY",
  "message": "동일한 CatalogProduct 또는 ProductVariant가 이미 존재합니다."
}
```

`PENDING` 요청만 거절할 수 있다. `reasonCode`와 판매자에게 공개할 `message`는 필수다. 아래 `reasonCode`는 심사 업무 사유이며 API 예외 코드가 아니다.

| `reasonCode` | 설명 |
|---|---|
| `CATALOG_DATA_INVALID` | 상품·Variant 정보, 식별자 또는 속성 값이 유효하지 않음 |
| `DUPLICATE_CATALOG_ENTRY` | 동일한 Catalog 리소스가 이미 존재함 |
| `CATEGORY_NOT_APPROVED` | Category가 존재하지 않거나 승인되지 않음 |

#### 성공 응답: `200 OK`

```json
{
  "requestId": "uuid",
  "status": "REJECTED",
  "rejectionReasonCode": "DUPLICATE_CATALOG_ENTRY",
  "processedByUserId": "uuid",
  "processedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-016` | 요청이 존재하지 않음 | 등록 요청을 찾을 수 없습니다. | 없음 | 요청 식별자 |
| 409 | `ADMIN-017` | 처리된 요청을 다시 거절 | 이미 처리된 등록 요청입니다. | status | 요청 상태와 식별자 |
| 400 | `ADMIN-019` | 사유 코드 또는 메시지 누락 | 거절 사유를 입력해 주세요. | 실패 필드 | 입력값과 검증 원인 |
