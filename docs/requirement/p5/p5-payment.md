# P5 Payment API

Payment의 데이터 모델과 결제 상태 조회 API를 정의한다. 결제 수단은 [Payment Method API](p5-payment-method.md), 결제 요청과 Webhook은 [Payment Process](p5-payment-process.md), 정책은 [P5 Policy](p5-policy.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | API |
|---|---|---|
| `Payment` | 주문에 대한 결제 시도와 최종 상태 | 상태 조회 |
| `Payment`와 `Order` | Order 1건에 대한 결제 상태 연결 | 결제 요청은 Payment Process에서 정의 |

## 2. 데이터 모델

### 2-1. `Payment`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `paymentId` | UUID | O | 결제 식별자 |
| `orderId` | UUID | O | 대상 주문 |
| `paymentMethodId` | UUID | O | 사용한 결제 수단 |
| `transactionId` | String | - | Simulator 거래 식별자 |
| `status` | Enum | O | `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED` |
| `amount` | Integer | O | 결제 금액(KRW) |
| `failureCode` | String | - | 실패 사유 코드 |
| `webhookEventId` | String | - | Webhook 중복 처리를 위한 이벤트 ID |
| `completedAt` | Instant | - | 성공 또는 실패 확정 시각 |
| `createdAt` | Instant | O | 생성 시각 |
| `updatedAt` | Instant | O | 변경 시각 |

`currency` 필드는 사용하지 않는다. 프로젝트의 금액 단위가 KRW로 고정되어 있기 때문이다. 카드번호 원문과 CVC는 Payment에 저장하지 않는다.

### 2-2. 관계와 제약

- Payment는 하나의 Order와 하나의 PaymentMethod에 연결된다.
- 결제 요청 직전 계산한 Order 금액과 Payment `amount`는 같아야 한다.
- `PROCESSING` Payment는 중복 결제 요청으로 여러 건 생성하지 않는다.
- `webhookEventId`는 unique 제약으로 중복 Webhook을 멱등 처리한다.

## 3. API 정의

### 3-1. 결제 상태 조회

`GET /api/v1/payments/{paymentId}`

권한: 주문 소유자 본인과 유효한 OrderSession.

#### 성공 응답: `200 OK`

```json
{
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "status": "PROCESSING",
  "amount": 1200000,
  "failureCode": null,
  "completedAt": null
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 403 | `PAYMENT-001` | 다른 사용자의 결제 | 결제 정보를 조회할 수 없습니다. |
| 404 | `PAYMENT-002` | 결제가 없음 | 결제 정보를 찾을 수 없습니다. |
| 423 | `ORDER-003` | 주문 화면 세션 없음 | 주문 화면 세션이 필요합니다. |
