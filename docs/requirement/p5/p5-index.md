# P5 주문·결제·배송 문서 안내

P5는 장바구니에서 선택한 상품을 주문으로 확정하고, 결제를 처리한 뒤 배송 상태를 보여주는 흐름을 정의한다. 주문·결제·배송은 서로 다른 리소스와 책임을 가지지만 사용자는 하나의 구매 흐름으로 경험한다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P5 Policy](p5-policy.md) | 정책 | 범위, 책임, 용어, 상태, 불변식, 도메인 경계 |
| [Order API](p5-order.md) | 리소스 API | Order·OrderItem 모델, 주문 조회·생성·취소 API |
| [Order Core](p5-order-core.md) | 주문 정책 보조 문서 | 주문 생명주기와 변경 규칙 |
| [Order Checkout](p5-order-checkout.md) | 주문서 흐름 | 장바구니 검증, 주문 화면 구성, 주문 생성 오류 처리 |
| [Order History](p5-order-history.md) | 주문 조회 보조 문서 | 주문 상세·주문 목록·검색 조건 |
| [Order Session](p5-order-session.md) | 리소스·보안 정책 | 단일 디바이스 주문 화면 제한과 Caffeine 세션 |
| [Payment API](p5-payment.md) | 리소스 API | Payment 모델과 결제 상태 조회 |
| [Payment Method API](p5-payment-method.md) | 리소스 API | 결제 수단 등록·조회·삭제 |
| [Payment Process](p5-payment-process.md) | 결제 흐름 | Payment Simulator 요청, 처리 중 응답, Webhook, 재시도 |
| [Delivery API](p5-delivery.md) | 리소스·상태 정책 | 배송 모델과 90초 단계 전환 |

정책은 `p5-policy.md`를 먼저 읽고, 데이터 모델과 API는 각 리소스 문서를 읽는다. 화면 흐름은 Checkout·History·Payment Process 문서를 함께 읽는다.

## 2. 책임과 경계

| 책임 | 소유 문서·도메인 | 참조 |
|---|---|---|
| 장바구니 항목 선택·보관 | P3 Cart | [P3 Cart](../p3/p3-cart.md) |
| 상품명·Variant 표시 정보 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| 현재 가격·재고·Offer 검증 | P9 Offer | [P9 Offer](../p9/p9-offer.md) |
| 쿠폰 검증·할인 계산 | P4 Coupon | [P4 Policy](../p4/p4-policy.md), [Coupon API](../p4/p4-coupon.md) |
| 사용자 주소 | P1 User | [P1 Address](../p1/p1-address.md) |
| 주문·주문 금액·주문 상태 | P5 Order | [Order API](p5-order.md) |
| 결제 수단·결제 상태 | P5 Payment | [Payment API](p5-payment.md) |
| 배송 상태 | P5 Delivery | [Delivery API](p5-delivery.md) |
| 이벤트 전달과 운영 | P6·P7 | [P6 Infrastructure](../p6/p6-infrastructure.md), [P7 Admin](../p7/p7-admin.md) |

P5는 fulfillment, 창고, 운송사, 운송장, 분할 배송을 다루지 않는다. 결제 성공 직후 배송을 `PREPARING`으로 만들고, 개발용 스케줄러가 배송 상태를 전환한다.

## 3. 문서 작성 순서

1. [P5 Policy](p5-policy.md)에서 정책과 상태 전이를 정한다.
2. [Order API](p5-order.md), [Payment API](p5-payment.md), [Payment Method API](p5-payment-method.md), [Delivery API](p5-delivery.md), [Order Session](p5-order-session.md)에서 모델과 API를 정의한다.
3. Checkout·History·Payment Process에서 여러 리소스를 조합하는 화면과 흐름을 정의한다.
4. 정책과 API가 충돌하면 Policy를 기준으로 API 문서를 수정한다.

## 4. 작성 원칙

- 금액은 KRW 정수로 표현하며 `currency`와 `shippingFee`를 사용하지 않는다.
- 사용자가 보는 주문 식별자는 `orderId`이며, 주문 상세는 소유자 본인만 조회할 수 있다.
- 다른 도메인의 Entity·Repository를 직접 참조하지 않고 공개 API 또는 이벤트를 사용한다.
- 공통 URI, 응답, 예외, 페이지네이션, 인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
