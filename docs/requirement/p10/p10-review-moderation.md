# P10 Review Moderation API

이 문서는 판매자의 Review 숨김 요청과 관리자의 승인·거절을 위한 `ReviewModeration` 데이터 모델과 API를 정의한다. 숨김·심사 정책은 [P10 Policy](p10-policy.md), Review 원본과 고객용 API는 [P10 Review API](p10-review.md)를 따른다.

성공 응답은 각 API의 Response DTO를 직접 반환한다. 예외 응답은 [공통 API 계약](../index.md#공통-api-계약)의 `exceptionCode`, `message`, 선택적 `details` 형식을 사용한다. 요청 추적 ID는 `X-Request-Id` 응답 헤더로 제공한다.

## 1. 데이터 모델

### 1-1. ReviewModeration

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `moderationId` | UUID | 예 | 심사 요청 식별자 |
| `reviewId` | UUID | 예 | 대상 Review 식별자 |
| `sellerId` | UUID | 예 | 요청한 Offer의 판매자 식별자 |
| `requestedByUserId` | UUID | 예 | 실제 요청자 식별자 |
| `reason` | VARCHAR(2000) | 예 | 숨김 요청 사유 |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `reviewedByUserId` | UUID | 승인·거절 시 | 심사한 관리자 식별자 |
| `reviewedAt` | TIMESTAMP | 승인·거절 시 | 심사 시각 |
| `decisionComment` | VARCHAR(2000) | 아니오 | 관리자 심사 의견 |
| `createdAt` | TIMESTAMP | 예 | 요청 시각 |
| `updatedAt` | TIMESTAMP | 예 | 상태 변경 시각 |

## 2. 판매자 숨김 요청

### 요청

`POST /api/v1/seller/reviews/{reviewId}/moderation-requests`

```json
{
  "reason": "리뷰에 상품과 무관한 내용이 포함되어 있습니다."
}
```

### 성공 응답: `201 Created`

```json
{
  "moderationId": "uuid",
  "reviewId": "uuid",
  "status": "PENDING",
  "createdAt": "2026-08-10T12:00:00Z"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `REVIEW-016` | 숨김 요청 사유가 없거나 2,000자를 초과함 | 숨김 요청 사유를 입력해야 합니다. | `reason` 허용 길이 | Review 숨김 요청 사유 검증 실패: `reviewId={reviewId}, reasonLength={reasonLength}` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `REVIEW-008` | 요청자가 Offer 판매자가 아님 | 해당 Offer의 판매자만 숨김을 요청할 수 있습니다. | 없음 | Review 숨김 요청 권한 없음: `userId={userId}, reviewId={reviewId}, sellerId={sellerId}` |
| 404 | `REVIEW-006` | Review가 존재하지 않음 | 리뷰를 찾을 수 없습니다. | 없음 | Review 조회 실패: `reviewId={reviewId}` |
| 409 | `REVIEW-010` | 처리 대기 중인 숨김 요청이 이미 존재함 | 처리 대기 중인 숨김 요청이 이미 있습니다. | 없음 | Review 숨김 요청 중복: `reviewId={reviewId}, moderationId={moderationId}` |
| 409 | `REVIEW-011` | Review가 이미 `HIDDEN` 상태임 | 이미 숨김 처리된 리뷰입니다. | 없음 | 이미 숨김 처리된 Review: `reviewId={reviewId}` |

## 3. 관리자 승인

### 요청

`POST /api/v1/admin/review-moderations/{moderationId}/approve`

```json
{
  "decisionComment": "정책 위반 사유가 확인되어 숨김 처리합니다."
}
```

### 성공 응답: `200 OK`

```json
{
  "moderationId": "uuid",
  "reviewId": "uuid",
  "moderationStatus": "APPROVED",
  "reviewVisibilityStatus": "HIDDEN",
  "reviewedAt": "2026-08-10T12:05:00Z"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `REVIEW-017` | 심사 의견이 2,000자를 초과함 | 심사 의견이 올바르지 않습니다. | `decisionComment` 허용 길이 | Review 숨김 승인 의견 검증 실패: `moderationId={moderationId}, commentLength={commentLength}` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `REVIEW-009` | 요청자가 관리자가 아님 | 관리자만 리뷰 숨김 요청을 심사할 수 있습니다. | 없음 | Review 숨김 승인 권한 없음: `userId={userId}, moderationId={moderationId}` |
| 404 | `REVIEW-012` | ReviewModeration이 존재하지 않음 | 숨김 요청을 찾을 수 없습니다. | 없음 | ReviewModeration 조회 실패: `moderationId={moderationId}` |
| 409 | `REVIEW-013` | 이미 승인·거절된 요청임 | 이미 처리된 숨김 요청입니다. | 없음 | ReviewModeration 중복 처리: `moderationId={moderationId}, status={status}` |
| 409 | `REVIEW-011` | Review가 이미 `HIDDEN` 상태임 | 이미 숨김 처리된 리뷰입니다. | 없음 | 이미 숨김 처리된 Review: `reviewId={reviewId}` |

## 4. 관리자 거절

### 요청

`POST /api/v1/admin/review-moderations/{moderationId}/reject`

```json
{
  "decisionComment": "숨김 사유가 정책상 인정되지 않습니다."
}
```

### 성공 응답: `200 OK`

```json
{
  "moderationId": "uuid",
  "reviewId": "uuid",
  "moderationStatus": "REJECTED",
  "reviewVisibilityStatus": "VISIBLE",
  "reviewedAt": "2026-08-10T12:05:00Z"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `REVIEW-018` | 심사 의견이 2,000자를 초과함 | 심사 의견이 올바르지 않습니다. | `decisionComment` 허용 길이 | Review 숨김 거절 의견 검증 실패: `moderationId={moderationId}, commentLength={commentLength}` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `REVIEW-009` | 요청자가 관리자가 아님 | 관리자만 리뷰 숨김 요청을 심사할 수 있습니다. | 없음 | Review 숨김 거절 권한 없음: `userId={userId}, moderationId={moderationId}` |
| 404 | `REVIEW-012` | ReviewModeration이 존재하지 않음 | 숨김 요청을 찾을 수 없습니다. | 없음 | ReviewModeration 조회 실패: `moderationId={moderationId}` |
| 409 | `REVIEW-013` | 이미 승인·거절된 요청임 | 이미 처리된 숨김 요청입니다. | 없음 | ReviewModeration 중복 처리: `moderationId={moderationId}, status={status}` |
