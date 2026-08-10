# P5 Order, Payment, Delivery (주문·결제·배송)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/orders/price-preview` | 로그인 | 주문 금액 계산 |
| POST | `/api/v1/orders` | 로그인 | 주문 생성·결제 시작 |
| GET | `/api/v1/orders` | 로그인 | 내 주문 목록 |
| GET | `/api/v1/orders/{orderId}` | 로그인 | 주문 상세 |
| POST | `/api/v1/orders/{orderId}/cancel` | 로그인 | 주문 취소 |
| POST | `/api/v1/payment-methods` | 로그인 | 결제 수단 등록 |
| GET | `/api/v1/payment-methods` | 로그인 | 결제 수단 조회 |
| DELETE | `/api/v1/payment-methods/{paymentMethodId}` | 로그인 | 결제 수단 삭제 |

## 2. 요구사항

### 2-1. 주문 생성

주문 흐름:

```text
장바구니 검증 → PENDING 주문 생성 → 금액 확정 → 결제 승인
  성공: PAID → 재고 차감·쿠폰 사용·포인트 차감·배송 생성
  실패: CANCELED → 사용 자원 복원
```

`POST /api/v1/orders/price-preview`

요청:

```json
{
  "cartItemIds": ["uuid"],
  "addressId": "uuid",
  "userCouponId": "uuid",
  "pointAmount": 5000
}
```

응답은 주문 생성과 동일한 금액 구조와 `issues` 배열을 반환한다. 가격·재고·쿠폰·포인트 검증에 실패하면 `valid`는 `false`다.

`POST /api/v1/orders` 요청:

```json
{
  "cartItemIds": ["uuid"],
  "addressId": "uuid",
  "userCouponId": "uuid",
  "pointAmount": 5000,
  "paymentMethodId": "uuid"
}
```

필수 입력은 `cartItemIds`, `addressId`, `paymentMethodId`다. 쿠폰과 포인트는 선택이다.

처리 규칙:

1. 장바구니 항목의 소유권·가격·상품 상태·재고를 검증한다.
2. 주문 생성 시 배송지 전체를 주소 스냅샷으로 복사한다.
3. 적용 가격 × 수량의 합으로 상품 금액을 계산한다.
4. 쿠폰 할인과 포인트 사용액을 검증한다.
5. 최종 금액을 저장하고 결제를 요청한다.
6. 결제 성공 시 `PENDING → PAID`로 변경한다.
7. 결제 성공 후 재고·쿠폰·포인트·장바구니·배송 처리를 실행한다.

#### 심화 사항

- 결제 승인 전 재고 예약과 예약 만료를 지원한다.

### 2-2. 주문 목록

`GET /api/v1/orders`

- Query는 `cursor`와 `size`를 사용한다. 첫 조회에서는 `cursor`를 생략한다.
- 기본 `size`는 20, 최대값은 100이다.
- 로그인한 사용자의 주문만 반환한다.
- 정렬은 `createdAt DESC, orderId DESC`로 고정한다.
- 응답은 `items`, `nextCursor`, `hasNext`를 포함하며 전체 주문 건수는 제공하지 않는다.
- 주문 상태·기간 필터를 추가할 경우 해당 조건은 cursor에 포함되어야 한다.

금액 계산:

```text
상품 총액 = Σ(적용 가격 × 수량)
쿠폰 할인 = 정률 할인 또는 정액 할인
포인트 사용 = 보유 포인트 이내이며 상품 금액의 50% 이하
최종 결제 금액 = 상품 총액 - 쿠폰 할인 - 포인트 사용
```

성공 응답 `201`:

```json
{
  "orderId": "uuid",
  "orderNumber": "ORD-20260809-000001",
  "status": "PAID",
  "items": [
    {
      "orderItemId": "uuid",
      "catalogProductId": "uuid",
      "sku": "HEADPHONE-BLK-001",
      "catalogProductName": "무선 헤드폰",
      "quantity": 2,
      "unitPrice": { "amount": 49900.00, "currency": "KRW" },
      "subtotal": { "amount": 99800.00, "currency": "KRW" }
    }
  ],
  "amounts": {
    "subtotal": { "amount": 99800.00, "currency": "KRW" },
    "discount": { "amount": 5000.00, "currency": "KRW" },
    "pointUsed": 5000,
    "shippingFee": { "amount": 0.00, "currency": "KRW" },
    "paidAmount": { "amount": 89800.00, "currency": "KRW" }
  },
  "delivery": {
    "deliveryId": "uuid",
    "status": "PREPARING"
  },
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 2-3. 주문 상태

| 상태 | 설명 | 허용 전이 |
|---|---|---|
| `PENDING` | 결제 대기 | `PAID`, `CANCELED` |
| `PAID` | 결제 완료 | `CANCELED` |
| `CANCELED` | 취소·결제 실패 | 없음 |

### 2-4. 주문 취소

`POST /api/v1/orders/{orderId}/cancel`

- `PENDING`은 즉시 취소한다.
- `PAID`는 결제 환불 후 쿠폰·포인트·재고를 복원한다.
- 배송이 `SHIPPED` 이상이면 취소할 수 없다.

응답:

```json
{
  "orderId": "uuid",
  "status": "CANCELED",
  "refundStatus": "COMPLETED",
  "restoredPointAmount": 5000,
  "restoredCoupon": true,
  "restoredInventory": true,
  "canceledAt": "2026-08-09T12:10:00Z"
}
```

### 2-5. 결제

- 결제 수단은 `CREDIT_CARD`, `KAKAO_PAY`, `BANK_TRANSFER`를 지원한다.
- 카드번호 등 민감 정보는 저장하지 않고 마스킹된 정보만 보관한다.
- Mock PG 승인 성공률은 80%, 실패율은 20%로 테스트할 수 있다.
- 결제 성공은 `PaymentCompletedEvent`, 실패는 `PaymentFailedEvent`로 전달한다.
- 모든 결제 결과는 `payments`에 거래 ID·금액·수단·상태·시각을 기록한다.

결제 상태는 `SUCCESS`, `FAILED`, `REFUNDED`다.

#### 심화 사항

- 결제 재시도, 부분 환불, 부분 취소, 결제 멱등성 키를 지원한다.

### 2-6. 배송

- 결제 완료마다 배송을 1개 생성하고 초기 상태는 `PREPARING`이다.
- 상태 전이는 `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED` 순서만 허용한다.
- `SHIPPED` 전환 시 운송장 번호가 필수다.
- 상태 변경 시 `deliveryStatusUpdatedAt`을 갱신한다.
- `DELIVERED` 도달 시 포인트 적립과 리뷰 작성 가능 이벤트를 발행한다.

#### 심화 사항

- 분할 배송, 다중 배송지, 판매자별 배송, 배송비 계산을 지원한다.
- 택배사 연동, 배송 추적, 반품·교환·환불 상태를 지원한다.

배송 상태 변경 응답:

```json
{
  "deliveryId": "uuid",
  "orderId": "uuid",
  "status": "SHIPPED",
  "trackingNumber": "TRACK-123456",
  "deliveryStatusUpdatedAt": "2026-08-09T12:00:00Z"
}
```

## 3. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 주문 요청 필드 오류 |
| 400 | `POINT_LIMIT_EXCEEDED` | 포인트 사용 한도 초과 |
| 400 | `INVALID_ORDER_STATUS_TRANSITION` | 주문 상태 전이 오류 |
| 400 | `INVALID_DELIVERY_STATUS_TRANSITION` | 배송 상태 전이 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ORDER_ACCESS_DENIED` | 타인 주문 접근 |
| 404 | `CART_ITEM_NOT_FOUND` | 주문 대상 장바구니 항목 없음 |
| 404 | `ORDER_NOT_FOUND` | 주문 없음 |
| 404 | `PAYMENT_METHOD_NOT_FOUND` | 결제 수단 없음 |
| 409 | `OUT_OF_STOCK` | 주문 생성 시 재고 부족 |
| 409 | `COUPON_NOT_AVAILABLE` | 쿠폰 사용 불가 |
| 409 | `INSUFFICIENT_POINT` | 포인트 잔액 부족 |
| 409 | `ORDER_CANNOT_BE_CANCELED` | 배송 시작 후 취소 |
| 402 | `PAYMENT_DECLINED` | 결제 승인 거절 |
| 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | 결제 서비스 장애 |
