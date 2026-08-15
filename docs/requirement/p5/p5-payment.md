# P5 Payment (결제)

P5 Payment는 결제 수단 관리와 주문 결제 과정을 분리해 관리한다. 공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 세부 문서

| 문서 | 책임 |
|---|---|
| [P5 Payment Method](p5-payment-method.md) | 결제 수단 등록·조회·삭제 |
| [P5 Payment Process](p5-payment-process.md) | 최종 결제·결제 상태·비동기 Webhook·결제 이벤트 |

주문 생성·취소와의 연결은 [P5 Order](p5-order.md), 이벤트·보상 흐름은 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.

## 2. API 영역

| 영역 | API |
|---|---|
| 결제 수단 관리 | `POST/GET/DELETE /api/v1/payment-methods` |
| 최종 결제 | `POST /api/v1/orders/{orderId}/pay` |
| 결제 상태 조회 | `GET /api/v1/payments/{paymentId}` |
| 결제 결과 수신 | `POST /internal/payment-webhooks/payment-simulator` |

## 3. 결제 인프라 경계

결제 승인 연동은 `PaymentGateway` 포트로 추상화한다. 현재 구현은 `Payment Simulator`를 인프라 어댑터로 사용하며, P5 결제 모듈은 특정 시뮬레이터 구현에 직접 의존하지 않는다.

Payment Simulator의 지연 시간·성공·실패·Webhook 전달 방식은 인프라 설정으로 관리한다. P5는 결제 요청 시 `PROCESSING` Payment를 저장하고, 최종 결과를 받은 뒤 `SUCCESS` 또는 `FAILED`로 확정한다.

## 4. 결제 상태

`Payment.status`는 `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`를 사용한다. 현재 범위에서는 `AUTHORIZED`, `CAPTURED`를 사용하지 않는다.

결제 시도마다 별도의 Payment를 생성한다. 실패한 Payment를 성공 상태로 덮어쓰지 않으며, 재시도는 새로운 Payment로 기록한다.
