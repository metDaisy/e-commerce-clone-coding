# P5 Delivery (배송)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. 주문 생성·취소와의 연결은 [P5 Order](p5-order.md), 이벤트·보상 흐름은 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.

배송 상태는 외부 배송 서비스 없이 애플리케이션 내부 스케줄러가 시뮬레이션한다. 이 문서는 배송 도메인의 상태와 자동 전환 규칙을 소유한다.

## 1. 범위와 책임

현재 배송은 결제 완료 후 주문에 연결되는 단순한 상태 기록이다.

- 결제 성공 주문마다 배송을 정확히 1개 생성한다.
- Fulfillment, Shipment, 창고, 택배사 연동, 운송장 추적은 지원하지 않는다.
- 분할 배송·다중 배송지·판매자별 배송은 지원하지 않는다.
- 배송 상태는 외부 배송 서비스가 아니라 애플리케이션 내부 스케줄러가 임의로 전환한다.
- 배송은 `Order`의 취소 여부를 결정하지 않는다. 주문 취소 가능 여부는 [P5 Order](p5-order.md)가 판단한다.

## 2. 요구사항

- 결제 완료마다 배송을 1개 생성하고 초기 상태는 `PREPARING`이다.
- 상태 전이는 `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED` 순서와 `PREPARING → CANCELED`만 허용한다.
- `PREPARING`, `SHIPPED`, `IN_TRANSIT`는 각각 90초 동안 유지한 뒤 다음 상태로 자동 전환한다.
- `DELIVERED`와 `CANCELED`는 종료 상태이며 자동 전환하지 않는다.
- 상태 변경 시 `deliveryStatusUpdatedAt`을 갱신한다.
- `DELIVERED` 도달 시 포인트 적립과 리뷰 작성 가능 이벤트를 발행한다.

## 3. 이벤트와 책임

- `PaymentCompletedEvent`를 소비해 기본 배송을 생성한다.
- 배송 생성과 자동 상태 전환은 배송 모듈이 소유한다. 주문 취소에 따른 `PREPARING → CANCELED` 전이는 주문 모듈의 공개 취소 흐름에서 요청한다.
- 스케줄러는 `deliveryStatusUpdatedAt + 90초`가 지난 배송을 찾아 다음 상태로 원자적으로 전환한다.
- 애플리케이션이 일시 중단되면 전환은 지연될 수 있으며, 재시작 후 만료된 전환을 다시 처리한다.
- `DeliveryCompletedEvent`는 포인트 적립과 리뷰 작성 자격 활성화를 위해 발행한다.
- 배송이 `SHIPPED` 이상이면 주문 취소를 허용하지 않는다. 취소 판단과 보상 흐름은 [P5 Order](p5-order.md)와 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.

배송 상태 변경 응답:

```json
{
  "deliveryId": "uuid",
  "orderId": "uuid",
  "status": "SHIPPED",
  "deliveryStatusUpdatedAt": "2026-08-09T12:00:00Z"
}
```

## 4. 상태

| 상태 | 설명 | 유지 시간 | 허용 전이 |
|---|---|---|---|
| `PREPARING` | 결제 완료 후 배송 준비 | 90초 | `SHIPPED` |
| `SHIPPED` | 배송 시작 | 90초 | `IN_TRANSIT` |
| `IN_TRANSIT` | 배송 중 | 90초 | `DELIVERED` |
| `DELIVERED` | 배송 완료 | 종료 상태 | 없음 |
| `CANCELED` | 주문 취소로 배송 중단 | 종료 상태 | 없음 |

`SHIPPED`, `IN_TRANSIT`, `DELIVERED`는 실제 택배사 연동 결과가 아니라 애플리케이션 스케줄러가 90초 간격으로 전환하는 시뮬레이션 상태다.

## 5. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 배송 상태 변경 요청 필드 오류 |
| 400 | `INVALID_DELIVERY_STATUS_TRANSITION` | 배송 상태 전이 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
