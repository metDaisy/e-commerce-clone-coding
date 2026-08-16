# P5 Payment Process

주문 화면에서 결제를 요청하고 Payment Simulator의 Webhook으로 최종 결과를 받는 비동기 결제 흐름을 정의한다. Payment 모델은 [Payment API](p5-payment.md), 결제 수단은 [Payment Method API](p5-payment-method.md)를 따른다.

## 1. 결제 흐름

```text
주문 화면의 결제 버튼
  → POST /api/v1/orders/{orderId}/pay
  → 서버가 Order를 세 번째 검증
  → Payment PROCESSING 저장
  → Payment Simulator에 결제 요청
  → 202 Accepted 반환
  → 주문 화면에서 결제 처리 중 표시
  → Simulator가 Webhook으로 SUCCESS 또는 FAILED 전달
  → 서버가 Payment와 Order 상태 변경
```

별도의 결제 페이지는 만들지 않는다. 결제 요청 후에도 주문 화면에 머물며 `GET /api/v1/payments/{paymentId}`로 상태를 조회한다.

## 2. 결제 요청 API

`POST /api/v1/orders/{orderId}/pay`

권한: 주문 소유자 본인과 유효한 OrderSession.

### 요청

```json
{
  "addressId": "address-uuid",
  "paymentMethodId": "payment-method-uuid"
}
```

주소는 주문 생성 시 전달하지 않고, 결제 요청 시 선택한 `addressId`를 전달한다. 서버는 주소와 결제 수단의 소유자를 다시 확인한다.

### 처리 규칙

1. Order가 `PENDING`이고 `expiresAt` 이전인지 확인한다.
2. Cart Item·Offer·현재 가격·재고·수량·쿠폰·포인트를 세 번째 검증한다.
3. 선택한 주소와 결제 수단의 소유권·활성 상태를 확인한다.
4. 계산한 금액으로 Payment를 `PROCESSING` 상태로 저장한다.
5. 배송지 스냅샷을 Order에 저장하고 Payment Simulator에 요청한다.
6. 같은 주문에 이미 `PROCESSING` 결제가 있으면 새 결제를 만들지 않고 기존 Payment를 반환한다.

### 성공 응답: `202 Accepted`

```json
{
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "paymentStatus": "PROCESSING",
  "orderStatus": "PENDING"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 409 | `ORDER-019` | 최신 계산 결과가 주문 스냅샷과 다름 | 주문 정보가 변경되었습니다. |
| 409 | `PAYMENT-007` | 이미 성공한 결제가 있음 | 이미 결제가 완료되었습니다. |
| 409 | `PAYMENT-008` | 다른 결제가 처리 중 | 결제가 처리 중입니다. |
| 410 | `ORDER-007` | 주문 만료 | 주문이 만료되었습니다. |
| 423 | `ORDER-018` | 다른 디바이스가 점유 중 | 다른 기기에서 주문이 진행 중입니다. |
| 503 | `PAYMENT-009` | Simulator 요청 불가 | 결제 서비스를 사용할 수 없습니다. |

## 3. Payment Simulator 연동

개발 환경의 Payment Simulator는 요청을 즉시 확정하지 않고 처리 중 상태를 먼저 반환한 뒤, 설정된 지연 시간 후 Webhook을 호출한다. 지연 시간은 Simulator의 고정 기본값이 아니라 프로젝트 인프라 설정으로 관리한다.

서버가 Simulator에 전달하는 값은 Payment ID, Order ID, 결제 금액, 결제 수단의 provider token, 결과 Webhook URL이다. 카드번호·CVC 원문은 전달하거나 저장하지 않는다.

## 4. Webhook API

`POST /internal/payment-webhooks/payment-simulator`

인증: 공유 비밀 또는 서명 검증. 일반 사용자 API 인증을 사용하지 않는다.

요청 예시:

```json
{
  "eventId": "webhook-event-uuid",
  "paymentId": "payment-uuid",
  "transactionId": "sim-transaction-uuid",
  "result": "SUCCESS",
  "failureCode": null
}
```

### 성공 응답: `200 OK`

Webhook은 `eventId`를 기준으로 멱등 처리한다. 이미 처리한 이벤트면 상태를 다시 변경하지 않고 `200 OK`를 반환한다.

### 상태 변경

| Webhook 결과 | Payment | Order | 후속 처리 |
|---|---|---|---|
| `SUCCESS` | `SUCCESS` | `PAID` | Delivery `PREPARING` 생성, Cart Item 정리, 쿠폰·포인트 확정 |
| `FAILED` | `FAILED` | `PENDING` | 배송을 만들지 않고 주문 화면에서 재시도 허용 |

### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 401 | `PAYMENT-010` | 서명·공유 비밀 검증 실패 | Webhook 인증에 실패했습니다. |
| 400 | `PAYMENT-011` | 필수 필드·결과 오류 | 잘못된 결제 결과입니다. |
| 404 | `PAYMENT-002` | Payment가 없음 | 결제 정보를 찾을 수 없습니다. |

## 5. 결제 결과 화면

- `PROCESSING`: 주문 화면에서 결제 처리 중을 표시하고 Payment 상태를 재조회한다.
- `SUCCESS`: Order가 `PAID`가 되면 주문 내역 화면으로 이동하며 주문 상품·금액·배송지·배송 상태를 표시한다.
- `FAILED`: 주문 화면에 남아 실패 메시지와 재시도 가능한 결제 수단을 표시한다.

결제 성공 후 주문 내역으로 이동하더라도 `orderId` 소유권 검증은 계속 적용한다.
