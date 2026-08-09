# P6 Infrastructure (Outbox & Saga)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. API 목록

P6의 Outbox와 Saga는 기본적으로 내부 인프라이며 일반 고객에게 공개하지 않는다. 운영·재처리 API는 [P7 관리자·운영 요구사항](p7-admin.md)에서 정의한다.

## 2. 요구사항

### 2-1. Outbox 데이터

| 필드 | 규칙 |
|---|---|
| `id` | UUID PK, 이벤트 멱등성 키 |
| `aggregateType` | `ORDER`, `PAYMENT`, `INVENTORY` 등 |
| `aggregateId` | 원본 애그리거트 ID |
| `eventType` | 공개 이벤트 타입 |
| `payload` | JSON, 비밀번호·토큰 제외 |
| `status` | `PENDING`, `PUBLISHED`, `FAILED` |
| `retryCount` | 재시도 횟수 |
| `createdAt` | 생성 시각 |
| `publishedAt` | 발행 시각 |
| `lastError` | 최근 오류 요약, 비밀값 제외 |

#### 심화 사항

- 이벤트 버전과 schema evolution을 지원한다.
- 이벤트 payload checksum, 보존 기간, 암호화, 압축을 지원한다.

### 2-2. 원자성

- 비즈니스 데이터 변경과 Outbox INSERT는 동일 DB 트랜잭션이다.
- 비즈니스 데이터가 저장됐지만 이벤트가 유실되는 상태를 허용하지 않는다.
- 다른 모듈의 내부 Bean을 직접 호출하지 않는다.
- 모듈 간 통신은 공개 이벤트 또는 허용된 작은 인터페이스만 사용한다.

#### 심화 사항

- 분산 추적용 correlation ID와 causation ID를 전파한다.

### 2-3. 발행 처리

- 5초 간격으로 `PENDING` 이벤트를 최대 100건 조회한다.
- 성공 시 `PUBLISHED`, `publishedAt`을 기록한다.
- 실패 시 `retryCount`를 1 증가하고 오류를 기록한다.
- 5회 실패 시 `FAILED`로 전환하고 관리자 알림 대상이 된다.
- 동시에 여러 Publisher가 실행되어도 같은 이벤트를 중복 선점하지 않는다.

#### 심화 사항

- 운영 대시보드, Dead Letter Queue, 수동 재처리와 알림 채널을 지원한다.

### 2-4. 멱등 소비

- 소비자는 `eventId`를 처리 키로 사용한다.
- 이미 처리한 이벤트는 비즈니스 작업을 반복하지 않는다.
- 소비 처리 기록은 성공·실패 여부와 시각을 남긴다.

### 2-5. Saga

정상 흐름:

```text
OrderCreatedEvent
 → PaymentCompletedEvent
 → StockDeductedEvent
 → 주문 확정
```

보상 흐름:

```text
재고 차감 실패
 → 결제 환불
 → 주문 취소
 → 쿠폰 복원
 → 포인트 복원
```

- 각 보상 단계는 독립 트랜잭션이다.
- 보상 실패 시 최대 3회 재시도한다.
- 3회 후에도 실패하면 `COMPENSATION_FAILED`로 기록하고 관리자 알림을 생성한다.

## 3. API 응답

Outbox 이벤트 상세 응답:

```json
{
  "eventId": "uuid",
  "aggregateType": "ORDER",
  "aggregateId": "uuid",
  "eventType": "OrderCreatedEvent",
  "status": "PUBLISHED",
  "retryCount": 0,
  "createdAt": "2026-08-09T12:00:00Z",
  "publishedAt": "2026-08-09T12:00:02Z",
  "lastError": null
}
```

Saga 상태 응답:

```json
{
  "sagaId": "uuid",
  "orderId": "uuid",
  "status": "COMPLETED",
  "steps": [
    { "name": "PAYMENT_REFUND", "status": "COMPLETED", "retryCount": 0 },
    { "name": "ORDER_CANCEL", "status": "COMPLETED", "retryCount": 0 }
  ],
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

## 4. 이벤트 목록

| 이벤트 | 발행 주체 | 소비 주체 | 설명 |
|---|---|---|---|
| `OrderCreatedEvent` | Order | Payment | 결제 요청 |
| `PaymentCompletedEvent` | Payment | Inventory | 재고 차감 요청 |
| `PaymentFailedEvent` | Payment | Order | 주문 취소 |
| `StockDeductedEvent` | Inventory | Order | 주문 확정 |
| `StockDeductionFailedEvent` | Inventory | Payment, Order | 환불·취소 보상 |
| `PaymentRefundedEvent` | Payment | Order | 환불 완료 |
| `OrderCanceledEvent` | Order | Coupon, Point | 쿠폰·포인트 복원 |
| `DeliveryCompletedEvent` | Delivery | Point, Review | 적립·리뷰 활성화 |

## 5. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | ADMIN 권한 부족 |
| 404 | `OUTBOX_EVENT_NOT_FOUND` | 이벤트 없음 |
| 404 | `SAGA_NOT_FOUND` | Saga 없음 |
| 409 | `EVENT_ALREADY_PUBLISHED` | 발행 완료 이벤트 재처리 |
| 409 | `SAGA_NOT_RETRYABLE` | 재시도할 수 없는 상태 |
| 500 | `EVENT_PROCESSING_FAILED` | 이벤트 처리 실패 |
| 503 | `EVENT_PUBLISHER_UNAVAILABLE` | Publisher 장애 |
