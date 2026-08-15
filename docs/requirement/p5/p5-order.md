# P5 Order (주문)

P5 Order는 선택한 장바구니 항목을 하나의 주문으로 확정하고 주문 당시의 구매 정보를 보존한다. 결제와 배송은 각각 [P5 Payment](p5-payment.md), [P5 Delivery](p5-delivery.md)가 소유한다.

## 1. 범위와 책임

- 주문 생성 시 선택한 `Cart Item`만 주문한다. 선택하지 않은 장바구니 항목은 유지한다.
- 주문 생성·금액 계산·주문 상태 전이를 소유한다.
- 주문 일부 항목만 취소하는 부분 취소는 지원하지 않고 전체 주문 단위로 취소한다.
- Fulfillment, Shipment, 운송사 연동, 분할 배송은 범위에 포함하지 않는다.
- 주문 금액은 `KRW`로 고정하며 통화 필드는 저장·응답하지 않는다.
- `Order`는 `OrderItem`을 소유하는 집계 루트다. `Payment`와 `Delivery`의 내부 구현은 소유하지 않고 공개 이벤트·인터페이스로 협력한다.
- 구매자용 주문 API는 주문 소유자 본인만 사용할 수 있다. 다른 사용자의 `orderId`는 `403 ORDER_ACCESS_DENIED`로 처리한다.

## 2. 세부 문서

| 문서 | 내용 |
|---|---|
| [P5 Order Core](p5-order-core.md) | Order·OrderItem 데이터 모델, `checkoutKey`, 상태, 만료, 취소 |
| [P5 Order Checkout](p5-order-checkout.md) | 주문서 생성, 3단계 검증, 금액 계산, 주문 화면 |
| [P5 Order Session](p5-order-session.md) | 주문 화면 단일 기기 점유, Cookie, Caffeine·JPA 저장소 |
| [P5 Order History](p5-order-history.md) | 주문 상세, 페이지 기반 주문 목록, 심화 검색 |
| [P5 Payment](p5-payment.md) | 결제 도메인 개요와 책임 |
| [P5 Payment Method](p5-payment-method.md) | 결제수단 등록·조회·삭제 |
| [P5 Payment Process](p5-payment-process.md) | 최종 결제, 결제 상태, Webhook, 결제 이벤트 |
| [P5 Delivery](p5-delivery.md) | 배송 생성과 배송 상태 |

## 3. API 목록

| Method | URI | 권한 | 상세 문서 |
|---|---|---|---|
| POST | `/api/v1/orders` | 로그인 | [주문서 생성](p5-order-checkout.md#1-주문서-생성) |
| GET | `/api/v1/orders` | 로그인 | [주문 목록](p5-order-history.md#2-주문-목록) |
| GET | `/api/v1/orders/{orderId}` | 로그인 | [주문 상세](p5-order-history.md#1-주문-상세) |
| POST | `/api/v1/orders/{orderId}/pay` | 로그인 | [P5 Payment Process 최종 결제](p5-payment-process.md#3-최종-결제) |
| POST | `/api/v1/orders/{orderId}/cancel` | 로그인 | [P5 Order 취소](p5-order-core.md#5-주문-취소) |

## 4. 공통 예외

공통 응답 봉투와 인증 예외는 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 주문 요청 필드 오류 |
| 400 | `POINT_LIMIT_EXCEEDED` | 포인트 사용 한도 초과 |
| 400 | `INVALID_ORDER_STATUS_TRANSITION` | 주문 상태 전이 오류 |
| 403 | `ORDER_ACCESS_DENIED` | 타인 주문 접근 |
| 404 | `CART_ITEM_NOT_FOUND` | 주문 대상 장바구니 항목 없음 |
| 404 | `ORDER_NOT_FOUND` | 주문 없음 |
| 409 | `OUT_OF_STOCK` | 주문 생성 시 재고 부족 |
| 409 | `COUPON_NOT_APPLICABLE` | 쿠폰 적용 조건 불충족 |
| 409 | `INSUFFICIENT_POINT` | 포인트 잔액 부족 |
| 409 | `ORDER_CANNOT_BE_CANCELED` | 배송 시작 후 취소 |

정상 흐름은 [P5 Index](p5-index.md)의 화면 흐름을 따르며, 이벤트 발행·멱등 소비·실패 보상 흐름은 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.
