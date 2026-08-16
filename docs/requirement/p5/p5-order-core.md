# P5 Order Core Policy

Order의 생명주기, 변경 가능 범위, 주문 재진입 규칙을 정의한다. 데이터 필드와 HTTP API는 [Order API](p5-order.md), 전체 정책은 [P5 Policy](p5-policy.md)를 따른다.

## 1. 주문 생명주기

```text
Cart Item 선택
  → PENDING 주문 생성 또는 갱신
  → 결제 요청
  → Payment PROCESSING
  → Webhook SUCCESS
  → PAID
  → Delivery PREPARING
```

결제 실패는 `Payment FAILED`와 `Order PENDING`으로 종료한다. 사용자는 동일 주문 화면에서 결제 수단을 바꾸어 재시도할 수 있다.

## 2. 주문 상태

| 상태 | 의미 | 허용 동작 |
|---|---|---|
| `PENDING` | 결제 전 주문 | 조회, 최신 조건으로 갱신, 결제, 취소 |
| `PAID` | 결제 성공 주문 | 상세 조회, 배송 조회, 정책상 취소 |
| `CANCELED` | 전체 주문 취소 | 조회 |
| `EXPIRED` | 24시간 내 결제하지 않아 만료 | 조회 |

`PENDING`은 생성 후 24시간 동안만 유효하다. 만료 시 `expiresAt`을 기준으로 `EXPIRED`로 전환하며, `expiresAt`과 `expiredAt`을 중복 보관하지 않는다.

## 3. 동일 장바구니 재진입

`checkoutKey`는 다음 순수 함수로 정의한다.

```text
checkoutKey = SHA-256(join(sort(cartItemIds), ":"))
```

입력에는 Cart Item ID만 사용하며, ID를 오름차순 정렬한 뒤 구분자로 연결한다. 따라서 동일한 Cart Item 집합은 입력 순서가 달라도 같은 키를 만든다. 사용자별로 조회하므로 다른 사용자의 동일한 ID 집합과 충돌하지 않는다.

동일 사용자·동일 `checkoutKey`의 `PENDING` 주문이 있으면 새 주문을 만들지 않는다. 최신 요청의 수량, 쿠폰 매핑, 사용 포인트, 계산된 금액으로 Order와 OrderItem을 갱신한다.

## 4. 주문 금액

```text
상품 금액 = Σ(OrderItem.unitPrice × OrderItem.quantity)
주문 금액 = 상품 금액 - 쿠폰 할인 금액 - 사용 포인트
```

주문 생성과 결제 직전에 P9·P4·P1의 공개 API를 호출해 현재 값을 다시 확인한다. 클라이언트가 전달한 `totalAmount`는 받지 않는다.

## 5. 주문 식별자와 조회 권한

사용자에게 표시하는 주문번호를 별도로 만들지 않고 `orderId`를 표시한다. `orderId`만 알고 있어도 주문 소유자 `userId`가 일치하는 경우에만 상세 조회가 가능하다. 소유자가 아니면 주문 존재 여부를 추론할 수 없도록 접근 거부로 처리한다.

## 6. 불변 데이터

결제 성공 후에는 다음 값을 변경하지 않는다.

- OrderItem의 상품·Variant·Offer 식별자
- 상품명·Variant 표시명·단가·수량
- 쿠폰 할인·사용 포인트·최종 주문 금액
- 결제 시 저장한 배송지 스냅샷

현재 카탈로그나 Offer의 이름·가격이 바뀌어도 주문 내역은 주문 시점 스냅샷으로 표시한다.
