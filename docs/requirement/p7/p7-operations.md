# P7 Operations (운영 인프라·정책)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

## 1. 쿠폰 관리

- 쿠폰 생성·수정·비활성화·관리 목록의 상세 규칙은 [P4 쿠폰 요구사항](../p4/p4-coupon.md)을 따른다.
- 쿠폰 발급·내 쿠폰 조회는 구매자 기능이므로 P4에 남긴다.

## 2. 배송 운영

배송 상태는 [P5 배송 요구사항](../p5/p5-delivery.md)에 따라 애플리케이션 내부 스케줄러가 자동으로 전환한다. 배송 상태를 관리자가 직접 변경하는 P7 API는 제공하지 않는다.

- `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED` 전이는 각 90초 후 자동으로 수행한다.
- 주문 취소에 따른 `PREPARING → CANCELED` 전이는 주문 취소 흐름에서 처리한다.
- 현재 프로젝트는 운송장·택배사 연동을 지원하지 않는다.

## 3. 이벤트·Saga 운영

- Outbox 조회·실패 이벤트 재처리·Saga 조회·보상 재시도는 P6의 상태 전이와 멱등성 규칙을 따른다.
- 이미 성공 처리된 이벤트 또는 재시도할 수 없는 Saga는 다시 처리하지 않는다.
- 운영 API 응답에는 비밀번호·토큰·OAuth secret 등 민감 정보를 포함하지 않는다.

### 3-1. Outbox 실패 이벤트 화면

관리자 전용 화면에서 이벤트 전체 이력을 기본으로 노출할 필요는 없다. 기본 목록은 운영 조치가 필요한 `FAILED` 이벤트를 보여주고, 필요할 때 `PENDING`, `PUBLISHED`를 상태 필터로 조회한다.

목록에는 다음 정보를 표시한다.

- `eventId`, `eventType`, `aggregateType`, `aggregateId`
- `status`, `retryCount`, `createdAt`, `publishedAt`
- `lastError`, `correlationId`

상세 화면에서는 오류 요약, 발행 시도 이력, 마스킹된 payload를 확인할 수 있다. 비밀번호·토큰·OAuth secret·세션 비밀값은 표시하지 않는다.

`POST /api/v1/admin/outbox/events/{eventId}/retry`는 `FAILED` 이벤트만 대상으로 하며, 재처리 요청 시각·처리 관리자·결과를 기록한다. `PUBLISHED` 이벤트를 임의로 재발행하는 기능은 제공하지 않는다.

### 3-2. Saga 실패 화면

Saga 화면의 기본 목록은 `COMPENSATION_FAILED`를 우선 표시하고, `RUNNING`, `COMPENSATING`, `COMPLETED` 상태도 필터로 조회할 수 있게 한다.

목록과 상세에는 다음 정보를 표시한다.

- `sagaId`, `sagaType`, `aggregateType`, `aggregateId`, `correlationId`
- `status`, `currentStep`, 단계별 상태와 `retryCount`
- `lastError`, `createdAt`, `updatedAt`

`POST /api/v1/admin/sagas/{sagaId}/retry`는 `COMPENSATION_FAILED` 상태에서만 허용한다. 재시도 전에 현재 상태를 다시 확인하고, 처리 중인 Saga에 대한 중복 재시도를 거부한다.

운영 화면에는 실패 건수와 마지막 실패 시각을 표시하고, 재시도할 수 없는 상태는 원인과 함께 안내한다. 관리자가 업무 데이터를 직접 수정하는 기능은 제공하지 않고, P6가 정의한 재시도·보상 흐름만 호출한다.

관리자 진입점:

- `GET /api/v1/admin/outbox/events`
- `GET /api/v1/admin/outbox/events/{eventId}`
- `POST /api/v1/admin/outbox/events/{eventId}/retry`
- `GET /api/v1/admin/sagas/{sagaId}`
- `POST /api/v1/admin/sagas/{sagaId}/retry`
