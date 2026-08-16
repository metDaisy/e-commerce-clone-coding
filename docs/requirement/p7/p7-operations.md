# P7 Operations API

이 문서는 관리자용 Outbox·Saga 운영 조회와 제한된 재시도 API를 정의한다. Outbox·Saga 원본 상태와 전이 규칙은 [P6 Infrastructure](../p6/p6-infrastructure.md), 공통 관리자 권한은 [P7 Admin API](p7-admin.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `OutboxEvent` | P6가 이벤트 payload·발행 상태·재시도 정보를 소유한다. | 목록·상세·실패 재시도 |
| `SagaInstance` | P6가 Saga 전체 상태·현재 단계·오류를 소유한다. | 상세·보상 재시도 |
| `SagaStep` | P6가 단계별 상태·재시도 횟수·오류를 소유한다. | Saga 상세 |

- P7은 운영자가 원인을 확인하고 허용된 재시도만 요청하도록 한다.
- 관리자 화면의 기본 목록은 조치가 필요한 실패 상태를 우선한다.
- 업무 데이터 자체를 P7에서 직접 수정하지 않는다.

## 2. 데이터 모델

### 2-1. OutboxEvent

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `eventId` | UUID | 예 | 이벤트 멱등성 키 |
| `eventType` | VARCHAR(255) | 예 | 공개 이벤트 유형 |
| `aggregateType` | VARCHAR(64) | 예 | 원본 애그리거트 유형 |
| `aggregateId` | UUID | 예 | 원본 애그리거트 식별자 |
| `status` | ENUM | 예 | `PENDING`, `PUBLISHED`, `FAILED` |
| `retryCount` | INTEGER | 예 | 발행 재시도 횟수 |
| `lastError` | VARCHAR(2000) | 아니오 | 비밀값을 제외한 최근 오류 요약 |
| `correlationId` | UUID | 아니오 | 관련 요청·업무 추적 ID |
| `createdAt` | TIMESTAMP | 예 | 생성 시각 |
| `publishedAt` | TIMESTAMP | 아니오 | 발행 시각 |

### 2-2. SagaInstance·SagaStep

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `sagaId` | UUID | 예 | Saga 식별자 |
| `sagaType` | VARCHAR(255) | 예 | Saga 유형 |
| `aggregateType`, `aggregateId` | 문자열·UUID | 예 | 업무 대상 |
| `correlationId` | UUID | 아니오 | 관련 요청·이벤트 추적 ID |
| `status` | ENUM | 예 | `RUNNING`, `COMPLETED`, `COMPENSATING`, `COMPENSATION_FAILED` |
| `currentStep` | VARCHAR(255) | 아니오 | 실행 중이거나 실패한 단계 |
| `steps` | 배열 | 예 | 단계별 상태·재시도 횟수·오류 요약 |
| `retryCount` | INTEGER | 예 | 전체 또는 현재 보상 단계 재시도 횟수 |
| `lastError` | VARCHAR(2000) | 아니오 | 비밀값을 제외한 최근 오류 요약 |
| `createdAt`, `updatedAt` | TIMESTAMP | 예 | 생성·변경 시각 |

### 2-3. 관계와 제약

- Outbox `FAILED` 이벤트만 수동 재시도할 수 있다.
- `PUBLISHED` 이벤트는 임의 재발행하지 않는다.
- `COMPENSATION_FAILED` Saga만 보상 재시도할 수 있다.
- 같은 이벤트·Saga의 중복 재시도는 거부한다.
- payload는 상세 화면에서 마스킹하며 비밀번호·토큰·OAuth secret·세션 비밀값을 표시하지 않는다.

## 3. API 정의

### 3-1. Outbox 이벤트 목록

`GET /api/v1/admin/outbox/events`

권한: `ADMIN`

Query:

```text
page=0
size=20
status=FAILED|PENDING|PUBLISHED
eventType=UserRolesChangedEvent
aggregateType=USER
aggregateId=uuid
```

기본 상태 필터는 `FAILED`이며, 기본 정렬은 `createdAt DESC, eventId DESC`다.

#### 성공 응답: `200 OK`

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "data": [
    {
      "eventId": "uuid",
      "eventType": "UserRolesChangedEvent",
      "aggregateType": "USER",
      "aggregateId": "uuid",
      "status": "FAILED",
      "retryCount": 5,
      "lastError": "consumer timeout",
      "correlationId": "uuid",
      "createdAt": "2026-08-16T12:00:00Z",
      "publishedAt": null
    }
  ]
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `ADMIN-004` | page·size·status·aggregate filter가 유효하지 않음 | 목록 조회 조건을 확인해 주세요. | 실패 query | 입력값과 검증 원인 |

### 3-2. Outbox 이벤트 상세

`GET /api/v1/admin/outbox/events/{eventId}`

권한: `ADMIN`

#### 성공 응답: `200 OK`

상세 응답은 오류 요약·발행 시도 이력·마스킹된 payload를 포함한다. 민감한 secret은 제외한다.

```json
{
  "eventId": "uuid",
  "eventType": "UserRolesChangedEvent",
  "status": "FAILED",
  "retryCount": 5,
  "lastError": "consumer timeout",
  "attempts": [
    { "attempt": 5, "status": "FAILED", "error": "consumer timeout", "attemptedAt": "2026-08-16T12:05:00Z" }
  ],
  "payload": { "userId": "uuid", "roles": "USER" }
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-027` | 이벤트가 존재하지 않음 | 이벤트를 찾을 수 없습니다. | 없음 | eventId |

### 3-3. 실패 Outbox 이벤트 재시도

`POST /api/v1/admin/outbox/events/{eventId}/retry`

권한: `ADMIN`

요청 본문은 없다. 재시도 요청 시각·처리 관리자·결과를 기록한다.

#### 성공 응답: `200 OK`

```json
{
  "eventId": "uuid",
  "status": "PENDING",
  "requestedByUserId": "uuid",
  "requestedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-027` | 이벤트가 존재하지 않음 | 이벤트를 찾을 수 없습니다. | 없음 | eventId |
| 409 | `ADMIN-025` | `FAILED`가 아닌 이벤트 | 현재 상태에서는 이벤트를 재시도할 수 없습니다. | status | 이벤트 상태와 eventId |

### 3-4. Saga 상세

`GET /api/v1/admin/sagas/{sagaId}`

권한: `ADMIN`

#### 성공 응답: `200 OK`

```json
{
  "sagaId": "uuid",
  "sagaType": "ORDER_PAYMENT",
  "aggregateType": "ORDER",
  "aggregateId": "uuid",
  "status": "COMPENSATION_FAILED",
  "currentStep": "PAYMENT_REFUND",
  "retryCount": 3,
  "lastError": "refund timeout",
  "steps": [
    { "name": "PAYMENT_REFUND", "status": "FAILED", "retryCount": 3 }
  ],
  "createdAt": "2026-08-16T12:00:00Z",
  "updatedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-028` | Saga가 존재하지 않음 | Saga를 찾을 수 없습니다. | 없음 | sagaId |

### 3-5. Saga 보상 재시도

`POST /api/v1/admin/sagas/{sagaId}/retry`

권한: `ADMIN`

`COMPENSATION_FAILED` 상태에서만 허용한다. 처리 전에 현재 상태를 다시 확인하고 중복 재시도를 거부한다.

#### 성공 응답: `200 OK`

```json
{
  "sagaId": "uuid",
  "status": "COMPENSATING",
  "requestedByUserId": "uuid",
  "requestedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-028` | Saga가 존재하지 않음 | Saga를 찾을 수 없습니다. | 없음 | sagaId |
| 409 | `ADMIN-026` | `COMPENSATION_FAILED`가 아님 | 현재 상태에서는 Saga를 재시도할 수 없습니다. | status | Saga 상태와 sagaId |
