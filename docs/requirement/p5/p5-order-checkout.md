# P5 Order Checkout

장바구니에서 주문 화면으로 이동하는 흐름과 주문 생성 API의 화면 책임을 정의한다. 주문 정책은 [P5 Policy](p5-policy.md), Order 모델과 API는 [Order API](p5-order.md)를 따른다.

## 1. 주문 화면 진입

```text
장바구니 화면 표시
  → 장바구니 1차 검증
  → 주문 버튼 클릭
  → POST /api/v1/orders
  → 주문 생성 또는 기존 PENDING 주문 갱신
  → 주문 화면 표시
```

주문 버튼을 누르면 현재 장바구니에서 선택한 `cartItemIds`만 주문으로 전환한다. 선택하지 않은 Cart Item은 주문에 포함하지 않는다.

최초 주문 생성 요청은 해당 주문의 OrderSession을 함께 획득한다. 이미 같은 `cartItemIds`의 `PENDING` 주문이 있으면 현재 디바이스의 세션을 확인한 뒤 갱신한다.

## 2. 장바구니 검증

장바구니 검증은 다음 세 시점에 수행한다.

| 시점 | 목적 | 실패 시 동작 |
|---|---|---|
| 장바구니 화면 표시 | 사용자에게 구매 가능 상태 표시 | 항목별 오류 표시, 주문 버튼 비활성화 |
| 주문 생성 | Order 생성 전 최종 금액 계산 | Order를 만들지 않고 `/cart` 이동 |
| 결제 요청 | 가격·재고·할인·주소의 최신 상태 재확인 | 결제를 요청하지 않고 주문 화면 유지 |

각 검증에는 Cart Item 소유권, Cart Item 존재, Offer 활성 상태, 현재 가격, 재고, 수량, 쿠폰 적용 가능 여부, 포인트 사용 가능 여부를 포함한다. 이전 검증 결과나 클라이언트가 보낸 금액을 재사용하지 않는다.

## 3. 주문 화면 구성

주문 화면은 다음 API 결과를 조합한다.

| 순서 | 화면 영역 | API |
|---:|---|---|
| 1 | 검증된 상품·현재 가격·재고·적용 쿠폰·포인트·주문 금액 | `GET /api/v1/orders/{orderId}` |
| 2 | 배송지 선택 | `GET /api/v1/me/addresses` |
| 3 | 결제 수단 선택 | `GET /api/v1/payment-methods` |
| 4 | 결제 상태 | `GET /api/v1/payments/{paymentId}` |

주문 화면의 초기 상태는 Order `PENDING`이다. 배송지는 주문 생성 요청에 포함하지 않으며, 사용자가 주소 API에서 선택한 뒤 결제 요청에 `addressId`를 전달한다.

## 4. 주문 생성 요청

```json
{
  "cartItemIds": ["cart-item-a", "cart-item-b"],
  "couponApplications": [
    { "cartItemId": "cart-item-a", "userCouponId": "user-coupon-a" },
    { "cartItemId": "cart-item-b", "userCouponId": "user-coupon-b" }
  ],
  "pointAmount": 3000
}
```

- 쿠폰은 Cart Item별로 하나씩 연결한다.
- 사용하지 않는 Cart Item에는 쿠폰을 적용하지 않는다.
- 한 주문에 적용할 수 있는 쿠폰은 최대 5개다.
- 배송지와 결제 수단은 이 요청에 넣지 않는다.

## 5. 주문 생성 실패 처리

주문 생성 중 오류가 발생하면 서버는 Order를 생성하지 않거나 기존 Order를 변경하지 않고 오류 응답을 반환한다.

```text
API 오류 응답
  → 클라이언트가 message 표시
  → /cart로 이동
  → 장바구니 재조회 및 최신 상태 표시
```

SPA 구조이므로 서버 HTTP Redirect는 사용하지 않는다. `message`는 사용자 안내용으로만 사용하고, 재시도 판단은 `exceptionCode`로 한다.

## 6. 주문 화면 이탈

- 정상적으로 장바구니 화면에 진입하면 장바구니 응답에서 `checkout_token` 삭제 쿠키를 내려 OrderSession 쿠키를 제거한다.
- 주문 화면에서 메인 등 다른 화면으로 이동하면 해당 화면의 API 응답에서도 OrderSession 쿠키를 삭제할 수 있다.
- 브라우저 새로고침은 SPA의 화면 진입 정책에 따라 장바구니로 이동시키며, 장바구니 응답에서 쿠키를 삭제한다.
- 브라우저 강제 종료·크래시·전원 차단으로 정리 요청을 받지 못하면 OrderSession은 `expiresAt`까지 유지된다.

세션의 단일 디바이스 제한과 쿠키 속성은 [Order Session](p5-order-session.md)을 따른다.
