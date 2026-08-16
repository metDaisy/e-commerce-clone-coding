# P7 Offer API

이 문서는 관리자의 Offer 비활성화와 판매자의 재활성화 요청 심사 API를 정의한다. Offer·Inventory 정책과 원본 모델은 P9, 공통 관리자 권한은 [P7 Admin API](p7-admin.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Offer` | P9가 판매 상태·현재 비활성화 정보·가격을 소유한다. | 관리자 활성·비활성 |
| `OfferStatusHistory` | P9가 Offer 상태 변경과 관리자 사유를 소유한다. | 관리자 활성·비활성 |
| `OfferActivationRequest` | P9가 판매자의 재활성화 요청과 처리 결과를 소유한다. | 요청 목록·승인·거절 |
| `Inventory` | P9가 재고 수량과 가용 상태를 소유한다. | 관리자 재고 조정 진입점 |

- P7은 Offer·Inventory 원본을 복제하지 않는다.
- 가격 수정과 재고 조정의 상세 규칙은 P9 API를 따른다. P7은 관리자 진입점만 제공한다.
- 관리자 비활성화 사유와 판매자 메시지는 P9의 현재 상태와 불변 이력에 기록한다.

## 2. 데이터 모델

### 2-1. Offer 관리자 표현

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `offerId` | UUID | 예 | Offer 식별자 |
| `sellerId` | UUID | 예 | 판매자 식별자 |
| `variantId` | UUID | 예 | 연결된 ProductVariant 식별자 |
| `status` | ENUM | 예 | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `inactiveSource` | ENUM | 아니오 | `SELLER`, `ADMIN`, `SYSTEM` |
| `inactiveReasonCode` | VARCHAR(64) | 아니오 | 현재 비활성화 사유 |
| `sellerMessage` | VARCHAR(1000) | 아니오 | 판매자에게 공개할 안내 |
| `statusChangedAt` | TIMESTAMP | 예 | 현재 상태 변경 시각 |

### 2-2. OfferActivationRequest

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `requestId` | UUID | 예 | 활성화 요청 식별자 |
| `offerId` | UUID | 예 | 대상 Offer |
| `sellerId` | UUID | 예 | 요청 판매자 |
| `requestedByUserId` | UUID | 예 | 실제 제출 User |
| `resolutionMessage` | VARCHAR(1000) | 예 | 판매자가 해결한 조치 설명 |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `processedByUserId` | UUID | 아니오 | 처리한 ADMIN |
| `processedAt` | TIMESTAMP | 아니오 | 처리 시각 |
| `rejectionReasonCode` | VARCHAR(64) | 아니오 | 거절 사유 |
| `rejectionMessage` | VARCHAR(1000) | 아니오 | 판매자 공개 거절 메시지 |
| `createdAt`, `updatedAt` | TIMESTAMP | 예 | 생성·변경 시각 |

### 2-3. 관계와 제약

- `ARCHIVED` Offer는 다시 활성화할 수 없다.
- Seller·CatalogProduct·ProductVariant가 모두 활성인 경우에만 Offer를 활성화할 수 있다.
- 관리자에 의해 비활성화된 Offer는 판매자가 직접 활성화할 수 없고 활성화 요청 승인을 거친다.
- Offer 하나에는 처리 중인 `PENDING` 활성화 요청을 하나만 둘 수 있다.
- 승인·거절된 활성화 요청은 다시 처리하지 않는다.

## 3. API 정의

### 3-1. Offer 활성·비활성

`PATCH /api/v1/admin/offers/{offerId}/status`

권한: `ADMIN`

요청:

```json
{
  "status": "INACTIVE",
  "reasonCode": "POLICY_VIOLATION",
  "sellerMessage": "상품 설명에 허위 효능 표현이 포함되어 비활성화되었습니다."
}
```

`status`는 `ACTIVE` 또는 `INACTIVE`만 허용한다. ADMIN이 `INACTIVE`로 변경할 때 `reasonCode`와 `sellerMessage`는 필수다.

| `reasonCode` | 설명 |
|---|---|
| `POLICY_VIOLATION` | 플랫폼 또는 판매 정책 위반 |
| `PRODUCT_INFORMATION_ERROR` | 상품 정보가 실제 상품과 다르거나 불완전함 |
| `INTELLECTUAL_PROPERTY` | 지식재산권 침해 또는 권리자 이의 제기 |
| `SAFETY_OR_COMPLIANCE` | 법률·규제·안전·인증·진품성 요건 미충족 |

위 `reasonCode`는 Offer 운영 사유이며 API 예외 코드가 아니다.

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "sellerId": "uuid",
  "variantId": "uuid",
  "status": "INACTIVE",
  "inactiveSource": "ADMIN",
  "inactiveReasonCode": "POLICY_VIOLATION",
  "sellerMessage": "상품 설명에 허위 효능 표현이 포함되어 비활성화되었습니다.",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. Offer 원본 예외는 [P9 Exceptions](../p9/p9-exceptions.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | [OFFER-001](../p9/p9-offer.md) | — | — | — | — |
| 409 | [OFFER-005](../p9/p9-offer.md) | — | — | — | — |
| 400 | `ADMIN-021` | 비활성화 사유·메시지 누락 | 비활성화 사유를 입력해 주세요. | 실패 필드 | 입력값과 검증 원인 |
| 409 | [OFFER-007](../p9/p9-offer.md) | — | — | — | — |

### 3-2. Offer 활성화 요청 목록

`GET /api/v1/admin/offers/activation-requests`

권한: `ADMIN`

Query:

```text
page=0
size=20
status=PENDING|APPROVED|REJECTED
sellerId=uuid
offerId=uuid
```

기본 정렬은 `createdAt DESC, requestId DESC`다. 목록·상세에는 Offer·Seller 정보, 관리자 비활성화 사유, 판매자 해결 설명, 요청 시각을 표시한다.

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
      "offerId": "uuid",
      "sellerId": "uuid",
      "status": "PENDING",
      "inactiveReasonCode": "POLICY_VIOLATION",
      "resolutionMessage": "상품 설명에서 문제 문구를 제거했습니다.",
      "createdAt": "2026-08-16T12:00:00Z"
    }
  ]
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. Offer 원본 예외는 [P9 Exceptions](../p9/p9-exceptions.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `ADMIN-004` | query 값이 유효하지 않음 | 목록 조회 조건을 확인해 주세요. | 실패 query | 입력값과 검증 원인 |
| 404 | `ADMIN-013` | sellerId가 존재하지 않음 | 판매자를 찾을 수 없습니다. | sellerId | Seller 식별자 |

### 3-3. Offer 활성화 요청 승인

`POST /api/v1/admin/offers/activation-requests/{requestId}/approve`

권한: `ADMIN`

요청 본문은 없다. `PENDING` 요청이고 Seller·CatalogProduct·ProductVariant가 모두 활성일 때만 승인한다.

#### 성공 응답: `200 OK`

```json
{
  "requestId": "uuid",
  "offerId": "uuid",
  "status": "APPROVED",
  "processedByUserId": "uuid",
  "processedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. Offer 원본 예외는 [P9 Exceptions](../p9/p9-exceptions.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-022` | 요청이 존재하지 않음 | 활성화 요청을 찾을 수 없습니다. | 없음 | 요청 식별자 |
| 409 | `ADMIN-023` | 처리된 요청을 다시 승인 | 이미 처리된 활성화 요청입니다. | status | 요청 상태 |
| 409 | [OFFER-007](../p9/p9-offer.md) | — | — | — | — |

### 3-4. Offer 활성화 요청 거절

`POST /api/v1/admin/offers/activation-requests/{requestId}/reject`

권한: `ADMIN`

요청:

```json
{
  "reasonCode": "ISSUE_NOT_RESOLVED",
  "message": "상품 설명에서 문제 문구가 아직 제거되지 않았습니다."
}
```

거절 사유는 `ISSUE_NOT_RESOLVED`, `DEPENDENCY_NOT_ACTIVE`, `INSUFFICIENT_EVIDENCE`만 사용한다. 이 값은 심사 업무 사유이며 API 예외 코드가 아니다. `reasonCode`와 `message`는 필수다.

#### 성공 응답: `200 OK`

```json
{
  "requestId": "uuid",
  "offerId": "uuid",
  "status": "REJECTED",
  "rejectionReasonCode": "ISSUE_NOT_RESOLVED",
  "processedByUserId": "uuid",
  "processedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-022` | 요청이 존재하지 않음 | 활성화 요청을 찾을 수 없습니다. | 없음 | 요청 식별자 |
| 409 | `ADMIN-023` | 처리된 요청을 다시 거절 | 이미 처리된 활성화 요청입니다. | status | 요청 상태 |
| 400 | `ADMIN-024` | 사유 코드 또는 메시지 누락 | 거절 사유를 입력해 주세요. | 실패 필드 | 입력값과 검증 원인 |
