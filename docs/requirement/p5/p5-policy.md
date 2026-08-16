# P5 Policy

이 문서는 주문·결제·배송의 API와 독립적으로 유지되는 업무 정책을 정의한다. 데이터 모델과 API 형식은 [Order API](p5-order.md), [Payment API](p5-payment.md), [Delivery API](p5-delivery.md)를 따른다.

## 1. 범위와 책임

### 범위

- 장바구니 선택 항목의 주문 전환
- 주문 금액 계산과 주문 상태 관리
- 결제 수단 선택과 Payment Simulator를 이용한 비동기 결제
- 결제 완료 후 배송 상태 생성·전환
- 주문 화면을 한 디바이스로 제한하는 임시 세션

### 책임

| 리소스 | 책임 |
|---|---|
| Order | 주문 항목, 가격 스냅샷, 할인·포인트, 배송지 스냅샷, 주문 상태 |
| Payment | 결제 시도, 결제 상태, 외부 거래 식별자, Webhook 결과 |
| Delivery | 주문별 배송 상태와 단계 전환 |
| OrderSession | 현재 `PENDING` 주문 화면에 대한 디바이스 점유 |

## 2. 용어와 행위자

| 용어·행위자 | 의미 |
|---|---|
| 사용자 | 장바구니를 주문하고 결제하는 인증 사용자 |
| 주문 화면 | 선택 상품, 최신 가격·재고, 쿠폰·포인트, 주소, 결제 수단을 보여주는 화면 |
| 주문 생성 | 선택한 Cart Item을 검증하고 `PENDING` Order를 생성하거나 갱신하는 작업 |
| 결제 요청 | Order를 다시 검증한 뒤 `PROCESSING` Payment를 만들고 Simulator에 요청하는 작업 |
| Payment Simulator | 실제 카드 인증 대신 지연된 성공·실패와 Webhook을 제공하는 개발용 외부 결제 서비스 |
| Webhook | Payment Simulator가 서버의 내부 URL로 최종 결과를 전달하는 서버 간 요청 |

## 3. 핵심 업무 규칙

1. 장바구니 화면 진입 시 선택 항목의 소유권, Offer 상태, 현재 가격, 재고, 수량을 검증한다.
2. 주문 생성 시 같은 검증을 다시 수행하고 주문 금액을 계산한다.
3. 결제 요청 직전에 가격·Offer·재고·수량·쿠폰·포인트·주소·결제 수단을 세 번째로 검증한다.
4. 주문 생성 요청 본문에는 `cartItemIds`, 적용할 쿠폰 목록, 사용할 포인트만 전달한다. 배송지는 별도 주소 API로 선택하고 결제 요청 때 `addressId`를 전달한다.
5. 동일 사용자가 같은 `cartItemIds`를 선택하면 기존 `PENDING` Order를 갱신한다. 수량과 쿠폰 매핑은 최신 요청으로 덮어쓰고, 사용할 포인트도 마지막 요청값을 사용한다.
6. 쿠폰은 Cart Item별로 매핑할 수 있으며 한 Cart Item에 중복 적용하지 않는다. 한 주문에는 최대 5개의 쿠폰 적용을 허용한다.
7. 주문 생성 오류는 주문을 생성하거나 갱신하지 않고 `code`, `message`, `details`를 반환한다. SPA는 오류 메시지를 표시한 뒤 장바구니 화면으로 이동한다.
8. 결제 요청은 Payment를 먼저 `PROCESSING`으로 저장한 뒤 Simulator에 전달한다. API는 `202 Accepted`를 반환하고 별도의 결제 페이지를 만들지 않는다.
9. Webhook 성공 시 Payment를 `SUCCESS`, Order를 `PAID`로 변경하고 Delivery를 `PREPARING`으로 생성한다. 실패 시 Payment는 `FAILED`, Order는 `PENDING`으로 남겨 같은 주문 화면에서 재시도할 수 있다.
10. `PENDING` Order는 생성 시점부터 24시간 후 만료되어 `EXPIRED`가 된다. `PAID`·`CANCELED`·`EXPIRED` Order는 주문 화면 점유 대상이 아니다.

## 4. 불변식과 상태 전이

### 불변식

- Order와 OrderItem은 동일한 `userId`의 장바구니 항목에서만 생성된다.
- 주문 금액은 주문 생성·결제 직전 검증 결과로 계산하며 클라이언트 금액을 신뢰하지 않는다.
- `orderId`는 사용자별 소유권을 확인한 뒤에만 상세 조회할 수 있다.
- `PAID` Order의 금액·항목·배송지 스냅샷은 변경하지 않는다.
- Payment Webhook은 `webhookEventId`로 멱등 처리한다.
- 하나의 `PENDING` Order에는 한 디바이스의 OrderSession만 유효하다.

### 상태 전이

| 현재 상태 | 조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| `PENDING` | 결제 요청 접수 | `PENDING` + Payment `PROCESSING` | Order·Payment |
| `PENDING` | Webhook 성공 | `PAID` | Payment Webhook |
| `PENDING` | 24시간 경과 | `EXPIRED` | 만료 처리 |
| `PENDING` | 사용자 취소 | `CANCELED` | Order API |
| `PAID` | 취소 정책상 허용되는 경우 | `CANCELED` | Order API |
| Payment `PROCESSING` | Webhook 성공 | `SUCCESS` | Payment Webhook |
| Payment `PROCESSING` | Webhook 실패 | `FAILED` | Payment Webhook |
| `PREPARING` | 90초 경과 | `SHIPPED` | 배송 스케줄러 |
| `SHIPPED` | 90초 경과 | `IN_TRANSIT` | 배송 스케줄러 |
| `IN_TRANSIT` | 90초 경과 | `DELIVERED` | 배송 스케줄러 |

## 5. 도메인 간 규칙과 예외 소유권

- P3 Cart의 항목이 없거나 소유자가 다르면 P5는 주문을 만들지 않고 Cart 검증 오류를 반환한다.
- 가격·재고·Offer 검증은 P9 공개 인터페이스를 사용한다. P5는 P9의 내부 예외를 그대로 노출하지 않는다.
- 쿠폰 검증·할인 계산은 P4에 위임하고, P5는 주문 금액에 반영한다.
- 주소 소유권 검증은 [P1 Address API](../p1/p1-address.md)를 사용한다. 최종 결제 시 주문에 배송지 스냅샷을 저장한다.
- Payment Simulator 연결·Webhook 오류는 Payment Process 문서의 Payment 예외로 변환한다.
- 이벤트 저장·재시도·운영 처리는 [P6 Infrastructure](../p6/p6-infrastructure.md)가 소유한다.

## 6. API 문서와의 관계

API URI, 요청·응답 JSON, HTTP 상태, `exceptionCode`는 각 리소스 문서에서 정의한다. 공통 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
