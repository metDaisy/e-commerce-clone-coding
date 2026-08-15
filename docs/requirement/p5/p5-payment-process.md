# P5 Payment Process (결제 과정)

최종 결제, 결제 상태 전이, Payment Simulator Webhook, 결제 이벤트를 정의한다. 결제 수단의 등록·조회·삭제는 [P5 Payment Method](p5-payment-method.md)를 따른다.

주문 결제 승인은 `POST /api/v1/orders/{orderId}/pay`에서 시작한다. 주문서 생성과 최종 결제를 분리한다.

현재 개발 환경은 Payment Simulator의 Webhook 지연 시간을 3초로 설정한다. 이 값은 Payment Simulator의 고정 기본값이 아니라 현재 프로젝트의 인프라 설정이다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/orders/{orderId}/pay` | 로그인 | 주문 최종 결제 요청 |
| GET | `/api/v1/payments/{paymentId}` | 로그인 | 결제 상태 조회 |
| POST | `/internal/payment-webhooks/payment-simulator` | Payment Simulator | 결제 최종 결과 Webhook |

`POST /api/v1/orders/{orderId}/pay`가 `PROCESSING` Payment를 생성하고 `202 Accepted`를 반환하면, 클라이언트는 결제 버튼을 비활성화하고 결제 상태 조회 API를 호출한다.

별도의 결제 페이지는 만들지 않는다. 주문 화면 안에서 `PROCESSING` 상태를 표시하며, 결제 결과에 따라 같은 주문 화면의 상태를 갱신한다.

## 2. 데이터 모델

`Payment`는 하나의 결제 시도를 나타낸다. 같은 주문의 재결제는 기존 Payment를 수정하지 않고 새로운 Payment를 생성한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | Payment 식별자. API에서는 `paymentId`로 노출한다. |
| `orderId` | UUID | 결제 대상 Order 식별자 |
| `paymentMethodId` | UUID | 사용한 PaymentMethod 식별자 |
| `transactionId` | String nullable | Payment Simulator가 발급한 거래 식별자. 최종 결과 전에는 null일 수 있다. |
| `status` | Enum | `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED` |
| `amount` | Integer | 결제 요청 금액(KRW). Payment 생성 후 변경하지 않는다. |
| `failureCode` | String nullable | 실패 시 결제 제공자가 전달한 실패 코드 |
| `webhookEventId` | String nullable | 최종 결과 Webhook의 `eventId`. Webhook 중복 처리 방지에 사용한다. |
| `completedAt` | Instant nullable | `SUCCESS`, `FAILED`, `REFUNDED`로 확정된 시각 |
| `createdAt` | Instant | Payment 생성 시각 |
| `updatedAt` | Instant | Payment 최종 수정 시각 |

현재 프로젝트의 금액 단위는 KRW로 고정하므로 `currency` 필드는 사용하지 않는다. 카드번호·인증번호·Webhook 인증 원문은 Payment에 저장하지 않는다.

관계와 제약조건:

- `Order : Payment`는 `1 : N`이다. 실패한 결제와 재결제 이력을 모두 보존한다.
- `PaymentMethod : Payment`는 `1 : N`이다. 결제수단 삭제 후에도 과거 Payment의 참조와 표시 정보는 유지한다.
- 같은 `Order`에는 `PROCESSING` Payment를 하나만 허용한다.
- `webhookEventId`가 존재하면 결제 Webhook 이벤트 식별자로 유일해야 한다.
- 데이터베이스에는 다음 부분 유일 제약을 둔다.

```text
UNIQUE (orderId)
WHERE status = 'PROCESSING'
```

Webhook 중복 방지를 위해 다음 유일 제약도 둔다.

```text
UNIQUE (webhookEventId)
WHERE webhookEventId IS NOT NULL
```

상태 전이는 다음과 같다.

```text
PROCESSING → SUCCESS → REFUNDED
PROCESSING → FAILED
```

`FAILED` Payment의 재결제는 새로운 Payment를 생성한다. `SUCCESS` 또는 `REFUNDED` Payment를 다시 `PROCESSING`으로 변경하지 않는다.

## 3. 최종 결제

주문 화면의 결제 버튼은 다음 요청을 호출한다.

`POST /api/v1/orders/{orderId}/pay`

```json
{
  "addressId": "uuid",
  "paymentMethodId": "uuid"
}
```

처리 규칙:

1. [P5 Order](p5-order.md)의 주문 소유자와 주문 상태가 `PENDING`인지 검증하고, [P5 Order Session](p5-order-session.md)의 유효한 Checkout Cookie인지 확인한다.
2. `addressId`가 로그인 사용자 소유인지 검증하고, Order가 배송지 전체를 스냅샷으로 저장한다.
3. Offer 판매 상태·상품 상태·최신 가격·재고를 다시 검증한다.
4. 쿠폰과 포인트의 현재 사용 가능 여부를 다시 검증한다.
5. 검증 결과가 주문 스냅샷과 달라지면 결제하지 않고 `PENDING`을 유지한다.
6. 결제수단을 검증하고 `PROCESSING` Payment를 생성한 뒤 `PaymentRequestedEvent`를 발행해 결제를 요청한다.
7. 같은 주문에 `PROCESSING` Payment가 있으면 새 결제를 시작하지 않고 `PAYMENT_IN_PROGRESS`를 반환한다.
8. API는 `202 Accepted`와 `paymentId`, `PROCESSING` 상태를 반환한다.
9. Payment Simulator 또는 PaymentGateway 인프라 어댑터는 설정된 지연 후 결제 최종 결과를 Webhook으로 전달한다.
10. 성공 Webhook을 수신하면 Payment를 `SUCCESS`로 변경하고 `PaymentCompletedEvent`를 발행한다. [P5 Order](p5-order.md)가 이 이벤트를 소비해 `PENDING → PAID`로 전환한다.
11. 결제 성공 후 재고·쿠폰·포인트·장바구니 처리를 수행한다.
12. [P5 Delivery](p5-delivery.md)가 `PaymentCompletedEvent`를 소비해 배송을 1개 생성하고 `PREPARING`으로 설정한다.

실패 Webhook을 수신하면 Payment를 `FAILED`로 기록하고 Order는 `PENDING`을 유지한다. 배송은 생성하지 않으며 사용자는 같은 주문에서 다른 결제 수단을 선택하거나 결제를 재시도할 수 있다. 재시도는 새로운 Payment를 생성한다.

화면 이동 규칙:

- `PROCESSING`: 현재 주문 화면에 결제 진행 중을 표시하고 결제 버튼을 비활성화한다.
- `SUCCESS`: `GET /api/v1/orders/{orderId}`로 주문 완료·배송 정보를 조회한 뒤 주문 내역 화면으로 이동한다.
- `FAILED`: 현재 주문 화면에 실패 메시지를 표시하고 결제수단 변경 또는 재결제를 제공한다.

최종 결제 요청은 주문 화면 조회와 동일한 Checkout Session을 요구한다. 다른 기기의 결제 요청은 `ORDER_CHECKOUT_IN_USE`로 거절하며 Payment를 생성하거나 결제를 시작하지 않는다.

가격·재고·쿠폰·포인트가 변경된 경우에는 결제하지 않고 `ORDER_REQUIRES_REVIEW`를 반환한다. 클라이언트는 주문 화면을 갱신해 사용자에게 변경 내용을 보여준다.

결제 요청 응답 `202`:

```json
{
  "orderId": "uuid",
  "paymentId": "uuid",
  "orderStatus": "PENDING",
  "paymentStatus": "PROCESSING"
}
```

## 4. 결제 상태 조회

`GET /api/v1/payments/{paymentId}`

결제 상태 조회도 본인 소유 Payment만 허용한다. 연결된 Order가 `PENDING`이면 주문 화면과 동일한 유효한 Checkout Cookie를 요구한다.

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "status": "SUCCESS",
  "transactionId": "simulator-transaction-id",
  "updatedAt": "2026-08-16T12:00:03Z"
}
```

`FAILED`인 경우 `failureCode`를 추가로 반환한다. 결제가 `SUCCESS`가 되면 클라이언트는 `GET /api/v1/orders/{orderId}`로 주문 완료 및 배송 정보를 조회한다.

결제 성공 후 주문 상세 응답 `200`은 [P5 Order History](p5-order-history.md#1-주문-상세)의 주문 상세 계약을 따른다.

## 5. Payment Simulator Webhook

Payment Simulator는 다음 Webhook으로 최종 결과를 전달한다.

`POST /internal/payment-webhooks/payment-simulator`

```json
{
  "eventId": "uuid",
  "paymentId": "uuid",
  "transactionId": "simulator-transaction-id",
  "status": "SUCCESS",
  "failureCode": null,
  "occurredAt": "2026-08-16T12:00:03Z"
}
```

- 이 API는 브라우저와 일반 사용자가 호출하지 않는다.
- `X-Payment-Simulator-Signature` 등 Payment Simulator 전용 인증값을 검증하고, 원문 인증값은 로그에 기록하지 않는다.
- `eventId`가 이미 처리된 Webhook이면 상태 변경과 이벤트 발행 없이 성공으로 응답한다.
- `PROCESSING` Payment만 `SUCCESS` 또는 `FAILED`로 전환한다.
- `SUCCESS` 또는 `FAILED` Payment에 대한 중복·지연 Webhook은 기존 상태를 변경하지 않는다.

## 6. 이벤트와 책임

- `PaymentCompletedEvent`는 결제가 성공했다는 사실을 알린다. 재고 차감과 배송 생성은 해당 이벤트를 소비하는 모듈이 처리한다.
- `PaymentFailedEvent`는 결제 실패 사실을 알린다. 결제 실패만으로 Order를 취소하지 않으며, `PENDING` 주문의 결제 재시도를 허용한다.
- 결제 모듈은 이벤트를 통해 주문 상태를 직접 변경하지 않는다. 주문 상태 전이는 [P5 Order](p5-order.md)가 소유한다.
- 환불 완료는 `PaymentRefundedEvent`로 알리고, 주문 취소 완료 처리는 [P5 Order](p5-order.md)와 [P6 Outbox & Saga](../p6/p6-infrastructure.md)의 계약을 따른다.

## 7. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 결제 요청 필드 오류 |
| 400 | `PAYMENT_WEBHOOK_INVALID` | Payment Simulator Webhook 형식 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 401 | `ORDER_CHECKOUT_SESSION_REQUIRED` | PENDING 주문의 Checkout Cookie 없음 |
| 401 | `PAYMENT_WEBHOOK_UNAUTHORIZED` | Payment Simulator Webhook 인증 실패 |
| 403 | `ADDRESS_ACCESS_DENIED` | 타인의 배송지 접근 |
| 404 | `PAYMENT_METHOD_NOT_FOUND` | 결제 수단 없음 |
| 404 | `PAYMENT_NOT_FOUND` | 결제 상태 조회 대상 Payment 없음 |
| 404 | `ADDRESS_NOT_FOUND` | 선택한 배송지 없음 |
| 409 | `ORDER_REQUIRES_REVIEW` | 최종 결제 전 가격·재고·쿠폰·포인트 조건 변경 |
| 409 | `PAYMENT_IN_PROGRESS` | 같은 주문에 처리 중인 Payment가 있음 |
| 409 | `ORDER_CHECKOUT_IN_USE` | 다른 기기가 주문 Checkout Session을 점유 중 |
| 409 | `ORDER_CHECKOUT_SESSION_EXPIRED` | Checkout Session 만료 |
| 409 | `ORDER_EXPIRED` | 주문 생성 후 24시간이 지나 결제할 수 없음 |
| 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | 결제 서비스 장애 |
