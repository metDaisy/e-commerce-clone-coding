# P5 Order Checkout (주문서 생성·주문 화면)

장바구니에서 선택한 항목을 검증하고 `PENDING` 주문을 생성하거나 기존 주문을 갱신하는 흐름을 정의한다. Order 데이터 모델과 만료·취소는 [P5 Order Core](p5-order-core.md), 주문 화면의 단일 기기 제한은 [P5 Order Session](p5-order-session.md), 최종 결제는 [P5 Payment Process](p5-payment-process.md)를 따른다.

## 1. 주문서 생성

주문 흐름:

```text
장바구니 화면 검증
  → 주문서 생성 시 확정 검증·금액 계산
  → PENDING 주문 생성 또는 기존 PENDING 주문 갱신
  → 주문 화면 표시
  → 결제 직전 재검증·Payment PROCESSING
  → Payment Simulator Webhook
    성공: Payment SUCCESS → PAID → 재고 차감·쿠폰 사용·포인트 차감·배송 생성(PREPARING)
    실패: Payment FAILED → Order PENDING 유지·주문 화면에서 결제 재시도
```

`POST /api/v1/orders`

| 항목 | 내용 |
|---|---|
| 권한 | 로그인 사용자 |
| 목적 | 선택한 Cart Item 검증, 금액 계산, PENDING 주문 생성 또는 갱신 |
| 성공 응답 | 신규 `201 Created`, 기존 PENDING 갱신 `200 OK` |

요청 JSON:

```json
{
  "cartItemIds": ["uuid"],
  "couponApplications": [
    {
      "userCouponId": "uuid",
      "cartItemIds": ["uuid"]
    }
  ],
  "pointAmount": 5000
}
```

요청에는 `cartItemIds`, 쿠폰과 Cart Item의 매핑인 `couponApplications`, 사용할 `pointAmount`만 전달한다. 쿠폰을 사용하지 않으면 `couponApplications`를 빈 배열로 전달하고, 포인트를 사용하지 않으면 `pointAmount`를 `0`으로 전달한다. 주소는 주문 요청과 별도로 조회한다. 서버는 포인트 보유 잔액과 사용 한도를 검증한다.

`cartItemIds`는 순서를 무시하고 비교한다. 동일 사용자의 동일한 `cartItemIds` 조합으로 유효한 `PENDING` 주문이 이미 있으면 새 Order를 생성하지 않고 기존 Order를 최신 요청 기준으로 갱신한다. `couponApplications`도 최신 요청으로 전체 교체한다. 자세한 키 계산은 [P5 Order Core의 checkoutKey](p5-order-core.md#2-checkoutkey)를 따른다.

쿠폰은 사용자가 `couponApplications`에 지정한 Cart Item에만 적용한다. 쿠폰의 `CouponTarget`에 포함되어 적용 가능한 Cart Item이라도 매핑에 포함하지 않으면 쿠폰 없이 주문한다. 예를 들어 장바구니에 A·B·C·D·E가 있고 쿠폰 a·b·c를 사용할 때 `A-a`, `B-b`, `C-c`만 전달하면 D·E는 쿠폰 없이 주문한다. 쿠폰 a가 A·D·E에 적용 가능하더라도 A에만 매핑했으므로 D·E에는 적용하지 않는다.

### 1-1. 주문서 생성 검증

1. 요청한 Cart Item이 로그인 사용자 소유인지, 존재하는지, 중복되지 않았는지 검증한다.
2. Offer의 판매 상태·상품 상태·현재 가격·구매 가능 재고·수량 제한을 검증한다.
3. `couponApplications`가 5개 이하이고, 각 `userCouponId`가 서로 다르며, 각 매핑에 하나 이상의 서로 다른 Cart Item이 포함되는지 검증한다.
4. 각 쿠폰의 Clip 상태·기간·대상 Offer를 검증한다. 하나의 Cart Item이 여러 쿠폰 매핑에 포함되면 거절한다.
5. 쿠폰 유형별 적용 한도와 할인액을 검증한다. 퍼센트 쿠폰은 쿠폰 하나당 최대 5개 대상 상품, 정액 쿠폰은 쿠폰 하나당 대상 상품 1개에 적용한다.
6. 적용 가격 × 수량의 합으로 상품 총액을 계산하고, OrderItem별 쿠폰 할인액과 주문 전체 할인액을 계산한다.
7. 포인트 사용액을 검증하고 최종 결제 금액을 계산한다.
8. 동일한 `checkoutKey`의 유효한 `PENDING` Order가 있으면 Order Item 수량, 상품·가격 스냅샷, 쿠폰 매핑, 쿠폰 할인액, 포인트 사용액, 금액 계산 결과를 최신 요청 기준으로 덮어쓴다. 없으면 `Order(PENDING)`과 Order Item 스냅샷을 새로 생성한다. 배송지 스냅샷은 아직 저장하지 않는다.
9. 갱신 또는 신규 생성 성공 응답으로 주문 화면에 표시할 검증 결과와 금액을 반환한다.

기존 주문을 갱신하는 경우 `orderId`와 `createdAt`은 유지하고 `updatedAt`, `expiresAt`, Order Item, 쿠폰 매핑, 사용 포인트, 금액 계산 결과만 최신 요청 기준으로 변경한다.

장바구니 화면에서 이미 검증한 항목도 주문서 생성 시 다시 검증한다. 주문서 생성 검증은 실패하면 Order를 생성하지 않는다.

검증 실패 시 API는 공통 오류 응답의 `code`, `message`, `details`를 반환한다. 클라이언트는 `message`를 사용자에게 표시한 뒤 장바구니 화면(`/cart`)으로 이동한다. 서버는 HTTP Redirect를 수행하지 않는다.

예시:

```json
{
  "code": "OUT_OF_STOCK",
  "message": "선택한 상품의 재고가 부족합니다.",
  "details": [
    { "cartItemId": "uuid", "reason": "outOfStock" }
  ]
}
```

성공 응답 예시:

```json
{
  "orderId": "uuid",
  "status": "PENDING",
  "items": [
    {
      "orderItemId": "uuid",
      "catalogProductName": "무선 헤드폰",
      "variantDisplayName": "블랙",
      "quantity": 2,
      "unitPrice": 49900.00,
      "subtotal": 99800.00,
      "coupon": {
        "userCouponId": "uuid",
        "discountAmount": 5000
      }
    }
  ],
  "amounts": {
    "subtotal": 99800.00,
    "discount": 5000.00,
    "pointUsed": 5000,
    "paidAmount": 89800.00
  },
  "shippingAddress": null,
  "delivery": null,
  "expiresAt": "2026-08-10T12:00:00Z",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

## 2. 주문 화면

주문 화면은 주문 생성 응답 또는 [주문 상세 API](p5-order-history.md#1-주문-상세)의 결과를 사용해 구성한다. `PENDING` 주문의 조회·갱신·결제에는 [P5 Order Session](p5-order-session.md)의 유효한 Checkout Cookie가 필요하다. 브라우저 전체 새로고침이 발생하면 주문 화면을 복구하지 않고 세션을 해제한 뒤 `/cart`로 이동한다.

- 주문 상품, 수량, 주문 당시 단가와 상품별 소계
- 상품 총액, 쿠폰 할인액, 포인트 사용액, 최종 결제 금액
- P1의 `GET /api/v1/me/addresses`로 별도 조회한 배송지 목록과 선택한 배송지
- 현재 주문 상태 `PENDING`
- 사용 가능한 결제 수단과 선택 상태

주문 화면은 다음 API의 결과를 조합한다.

| API | 용도 |
|---|---|
| `GET /api/v1/orders/{orderId}` | 주문 상품·금액·상태 조회 |
| `GET /api/v1/me/addresses` | 배송지 목록 조회 및 배송지 선택 |
| `GET /api/v1/payment-methods` | 결제수단 목록 조회 및 결제수단 선택 |
| `GET /api/v1/payments/{paymentId}` | 결제 상태 조회 |

주문 `orderId`는 주문 소유자 본인만 조회할 수 있다. 다른 사용자가 조회하면 `403 ORDER_ACCESS_DENIED`를 반환한다.

## 3. 최종 결제 연결

결제 버튼은 선택한 `addressId`, `paymentMethodId`와 함께 `POST /api/v1/orders/{orderId}/pay`를 호출한다. 별도의 결제 페이지로 이동하지 않고 주문 화면에서 결제 진행 상태를 표시한다. 최종 결제 API와 결제 승인·실패·재시도 규칙은 [P5 Payment Process의 최종 결제](p5-payment-process.md#3-최종-결제)를 따른다.

결제 직전 가격·재고·쿠폰·포인트 재검증, 배송지 스냅샷 저장, 결제 성공 후 Order 상태 변경은 P5 Payment와 P5 Delivery의 계약을 따른다.

## 4. 금액 계산

```text
상품 총액 = Σ(적용 가격 × 수량)
쿠폰 할인 = 정률 할인 또는 정액 할인
포인트 사용 = 보유 포인트 이내이며 상품 금액의 50% 이하
최종 결제 금액 = 상품 총액 - 쿠폰 할인 - 포인트 사용
```

배송비는 주문 금액에 포함하지 않는다.
