# P5 Index (주문·결제·배송)

P5는 장바구니에서 선택한 상품을 주문으로 만들고, 결제를 완료한 뒤 주문 내역과 배송 상태를 보여주는 흐름을 정의한다. `Order`, `Payment`, `Delivery`는 책임과 상태를 분리하되, 사용자에게는 하나의 구매 과정으로 제공한다.

세부 도메인 규칙은 [P5 Order](p5-order.md)와 [P5 Order 세부 문서](p5-order.md#2-세부-문서), [P5 Payment](p5-payment.md), [P5 Delivery](p5-delivery.md)를 따른다. 공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 범위

- 장바구니의 선택 상품 여러 개를 하나의 주문으로 생성한다.
- 주문 생성 시 가격·재고·쿠폰·포인트를 검증하고 금액을 계산한다. 배송지는 주문 화면에서 별도로 조회하고 최종 결제 시 확정한다.
- 주문 생성과 최종 결제는 별도 단계로 처리한다.
- 결제 성공 시 주문은 `PAID`가 되고 배송 1개를 `PREPARING` 상태로 생성한다.
- Fulfillment, Shipment, 창고·운송장·택배사 연동, 분할 배송은 지원하지 않는다.
- 주문 금액은 KRW로 고정하며 배송비와 Order의 `currency` 필드는 사용하지 않는다.

## 2. 사용자 화면 흐름

```text
장바구니 화면
  └─ 1차 검증 및 상품 선택
       │ 주문 버튼
       ▼
주문서 생성
  ├─ 검증·금액 계산 성공 → PENDING 주문 생성 또는 기존 PENDING 주문 갱신(24시간)
  └─ 검증 실패 → 오류 메시지 표시 → 장바구니 화면
       │
       ▼
주문 화면
  ├─ 검증된 상품·현재 가격·재고·쿠폰·포인트·주문 금액 표시
  ├─ 배송지 표시
  ├─ 결제 수단 표시 및 선택
  └─ 주문 상태: PENDING
       │ 결제 버튼
       ▼
최종 결제
  ├─ Payment PROCESSING: 주문 화면에서 결제 진행 중 표시
  ├─ 결제 실패: Payment FAILED, Order PENDING 유지 → 주문 화면에서 재결제
  ├─ 24시간 초과: Order EXPIRED → 새 주문 필요
  └─ 결제 성공: Order PAID → 장바구니·재고·쿠폰·포인트 처리 → Delivery 생성
       │
       ▼
결제 완료 및 주문 내역 화면
  ├─ 주문 상품·금액·배송지 표시
  ├─ 주문 상태 PAID 표시
  └─ 배송 정보와 배송 상태 PREPARING 표시
```

## 3. 단계별 처리

### 3-1. 장바구니 화면

장바구니를 조회할 때 다음 항목을 1차 검증한다.

- 장바구니 항목의 소유권과 존재 여부
- Offer와 상품의 판매 가능 상태
- 현재 가격
- 현재 재고와 주문 수량

검증에 실패한 항목은 장바구니 화면에 품절·판매 불가·가격 변경 등의 상태로 표시한다. 주문 버튼은 검증 가능한 선택 항목에 대해서만 사용할 수 있다. 이 검증은 화면 표시를 위한 것이며, 주문서 생성과 최종 결제에서 다시 수행한다.

### 3-2. 주문서 생성

장바구니 화면의 주문 버튼은 선택한 `cartItemIds`, 쿠폰과 Cart Item의 매핑인 `couponApplications`, 사용할 `pointAmount`를 `POST /api/v1/orders`에 전달한다. 주소는 주문 화면에서 P1의 주소 API로 별도 조회한다.

주문 모듈은 다음을 수행한다.

1. 선택한 Cart Item의 소유권·존재 여부·중복 여부를 검증한다.
2. Offer·상품 상태, 최신 가격, 재고, 수량을 다시 검증한다.
3. 상품 총액, 쿠폰 할인액, 포인트 사용액, 최종 결제 금액을 계산한다.
4. 가격·상품명·Variant 표시명을 주문 스냅샷으로 저장한다. 배송지 스냅샷은 최종 결제 시 저장한다.
5. `couponApplications`를 최대 5개까지 검증하고, 동일한 `cartItemId`에 쿠폰이 중복 적용되지 않도록 한다. 동일한 `cartItemIds` 조합의 유효한 `PENDING` Order가 있으면 수량·쿠폰 매핑·포인트·현재 가격·금액을 최신 요청 기준으로 갱신한다. 없으면 `PENDING` Order를 새로 생성한다.
6. 주문 화면에 필요한 상품·금액 정보를 반환한다. 신규 생성은 `201`, 기존 주문 갱신은 `200`이다.

주문서 생성 중 오류가 발생하면 Order를 생성하지 않는다. API는 `code`, `message`, `details`를 반환하고, 클라이언트는 `message`를 표시한 뒤 `/cart`로 이동한다. 장바구니 화면은 최신 가격과 재고를 다시 조회한다. 서버는 HTTP Redirect를 수행하지 않는다.

### 3-3. 주문 화면

주문 화면은 `GET /api/v1/orders/{orderId}`로 본인 주문을 조회한다. 다음 정보를 표시한다.

- 주문 상품, 수량, 주문 당시 단가와 상품별 소계
- 상품 총액, 쿠폰 할인액, 포인트 사용액, 최종 결제 금액
- P1의 `GET /api/v1/me/addresses`로 별도 조회한 배송지 목록과 선택한 배송지
- 현재 주문 상태 `PENDING`
- 사용 가능한 결제 수단과 선택 상태

주문 화면은 다음 API의 결과를 조합해 구성한다.

| API | 용도 |
|---|---|
| `GET /api/v1/orders/{orderId}` | 주문 상품·금액·상태 조회 |
| `GET /api/v1/me/addresses` | 배송지 목록 조회 및 배송지 선택 |
| `GET /api/v1/payment-methods` | 결제수단 목록 조회 및 결제수단 선택 |
| `GET /api/v1/payments/{paymentId}` | 결제 상태 조회 |

주문 `orderId`는 주문 소유자 본인만 조회할 수 있다. 다른 사용자가 조회하면 `403 ORDER_ACCESS_DENIED`를 반환한다.

### 3-4. 최종 결제

주문 화면은 주소 API와 결제수단 API를 별도로 호출해 배송지와 결제수단을 표시한다. 결제 버튼은 선택한 `addressId`, `paymentMethodId`와 함께 `POST /api/v1/orders/{orderId}/pay`를 호출한다.

결제 직전에 가격·재고·쿠폰·포인트를 3차 검증한다.

- 검증 결과가 주문 스냅샷과 다르면 결제하지 않고 `ORDER_REQUIRES_REVIEW`를 반환한다. 클라이언트는 주문 화면을 갱신해 변경 내용을 보여준다.
- 검증이 통과하면 `PROCESSING` Payment를 생성하고 `PaymentRequestedEvent`를 발행한다.
- API는 `202 Accepted`를 반환하며, 별도 결제 페이지로 이동하지 않고 주문 화면에서 결제 버튼을 비활성화하고 결제 상태를 조회한다.
- Payment Simulator가 지연 후 Webhook으로 결과를 전달한다.
- 결제가 실패하면 Payment는 `FAILED`, Order는 `PENDING`으로 유지한다. 배송은 생성하지 않으며 현재 주문 화면에서 다른 결제 수단으로 재결제할 수 있다.
- 결제 직전에 선택한 주소의 소유권을 검증하고 배송지 전체를 Order 스냅샷으로 저장한다.
- 결제가 성공하면 Order를 `PAID`로 변경하고 `PaymentCompletedEvent`를 발행한다. 이후 재고 차감, 쿠폰 사용, 포인트 차감, 장바구니 항목 삭제를 처리하고 Delivery를 하나 생성한다.

### 3-5. 결제 완료 및 주문 내역 화면

Payment 상태가 `SUCCESS`가 되면 `GET /api/v1/orders/{orderId}`로 주문 완료·배송 정보를 조회한 뒤 주문 내역 화면으로 이동한다.

- 주문 상태는 `PAID`다.
- 주문 상품과 주문 당시 금액 스냅샷을 표시한다.
- 결제한 배송지 스냅샷을 표시한다.
- 결제 완료로 생성된 Delivery의 `deliveryId`와 `PREPARING` 상태를 표시한다.
- `GET /api/v1/orders`에서는 로그인한 사용자의 주문만 `orderId` 기준으로 조회한다.
- 주문 목록은 페이지 기반으로 조회하며 최근 3개월·6개월·1년 preset과 직접 지정한 기간을 지원한다. 목록에는 `PAID`(결제완료), `CANCELED`(취소), `EXPIRED`(만료) 주문만 간단한 요약으로 표시하며, 구매 상품 설명 키워드 검색은 심화사항이다.

배송 상태는 [P5 Delivery](p5-delivery.md)의 단순 상태 전이를 따른다. 결제 완료 직후에는 `PREPARING`이며, 배송 처리에 따라 `SHIPPED`, `IN_TRANSIT`, `DELIVERED`로 변경할 수 있다.

## 4. 도메인 책임과 상태

| 도메인 | 책임 | 주요 상태 |
|---|---|---|
| `Order` | 주문 생성·금액 계산·주문 스냅샷·주문 조회·취소·만료 | `PENDING`, `PAID`, `CANCELED`, `EXPIRED` |
| `Payment` | 결제 수단·PG 승인 결과·결제 이력 | `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED` |
| `Delivery` | 결제 완료 후 배송 생성·배송 상태 | `PREPARING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`, `CANCELED` |

정상 흐름은 다음과 같다.

```text
장바구니 화면 검증
 → Order 생성 검증·금액 계산
 → PENDING Order
 → PaymentRequestedEvent
 → Payment PROCESSING
 → Payment Simulator Webhook
 → Payment SUCCESS
 → PAID Order
 → PaymentCompletedEvent
 → 재고·쿠폰·포인트·장바구니 처리
 → PREPARING Delivery
```

이벤트 발행, 멱등 소비, 실패 보상 흐름은 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.
