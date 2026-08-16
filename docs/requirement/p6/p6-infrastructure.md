# P6 Infrastructure

이 문서는 Spring Modulith 이벤트 publication과 모듈 간 이벤트 계약을 정의한다. 업무 정책은 [P6 Policy](p6-policy.md), 관리자 HTTP API는 [P7 Operations](../p7/p7-operations.md)을 따른다.

P6는 별도의 Outbox·EventConsumption·Saga Entity를 기본으로 만들지 않는다. 현재 Outbox 구현은 Spring Modulith Event Publication Registry를 사용한다.

## 1. 기본 저장소

### `event_publication`

Spring Modulith JPA Event Publication Registry가 transactional listener별 이벤트 publication과 완료 여부를 저장한다.

| 개념 | 설명 |
|---|---|
| publication 식별자 | Spring Modulith이 listener별로 관리하는 식별자 |
| listener | 이벤트를 처리할 모듈 listener |
| 이벤트 유형·직렬화 payload | 재처리할 이벤트 정보 |
| publication 시각 | publication 기록 시각 |
| 완료 시각 | listener 트랜잭션 성공 시각. 미완료면 재처리 대상이다. |

- 이 테이블은 Spring Modulith 기능에 필요한 저장소이며, 애플리케이션이 `EventPublication` Entity를 직접 만들지 않는다.
- 하나의 이벤트를 여러 listener가 받으면 listener별 publication이 관리된다.
- 업무 이벤트의 `eventId`와 Spring Modulith publication 식별자는 서로 다를 수 있다.
- 별도 `outbox_events`, `event_consumptions`, `saga_instances`, `saga_steps` 테이블은 현재 기본 범위에 포함하지 않는다.

## 2. 이벤트 계약

이벤트는 persistence Entity가 아니라 불변 Java `record`를 기본으로 한다. 공통 envelope 타입은 강제하지 않으며, 멱등 처리에 필요한 식별자와 업무 payload만 이벤트별로 정의한다.

| 필드 | 필수 | 설명 |
|---|---:|---|
| `eventId` | 예 | 원본 도메인이 생성하는 업무 이벤트 식별자 |
| `eventType` | 예 | 이벤트의 공개 유형. record 타입 또는 명시된 계약으로 식별한다. |
| `eventVersion` | 아니오 | payload 호환성 관리가 필요할 때 사용한다. |
| `aggregateId` | 예 | 원본 업무 대상 식별자 |
| `correlationId` | 아니오 | 하나의 요청·Saga 흐름 추적 ID |
| `causationId` | 아니오 | 원인이 된 이벤트 식별자 |
| 업무 payload | 예 | 소비에 필요한 최소 업무 값 |

### 관계와 제약

- 이벤트 재발행 때 `eventId`를 변경하지 않는다.
- payload에 persistence Entity, 비밀번호, 토큰, OAuth secret, 카드번호, CVC 원문을 넣지 않는다.
- 소비자는 다른 모듈의 내부 Entity를 조회하지 않고 공개 계약과 이벤트 payload를 사용한다.
- 이벤트 의미가 변경되면 기존 소비자를 깨뜨리지 않도록 새 이벤트 유형 또는 버전을 사용한다.

## 3. 내부 처리 계약

### 3-1. 이벤트 발행

발행 모듈은 업무 데이터 변경과 이벤트 발행을 같은 로컬 트랜잭션에서 처리한다.

```java
events.publishEvent(new PaymentCompletedEvent(eventId, orderId, paymentId));
```

다른 모듈의 내부 Bean·Repository를 직접 호출하지 않는다. 즉시 결과가 필요한 조회·검증만 Named Interface를 사용하고, 후속 작업은 이벤트로 전달한다.

### 3-2. 이벤트 소비

신뢰성이 필요한 모듈 listener는 `@ApplicationModuleListener` 또는 동등한 transactional listener를 사용한다.

```java
@ApplicationModuleListener
void handle(PaymentCompletedEvent event) {
    // 자기 모듈의 상태만 변경
}
```

- listener의 로컬 트랜잭션이 성공하면 publication을 완료한다.
- listener가 실패하면 트랜잭션을 롤백하고 publication은 미완료로 남는다.
- 재발행으로 같은 이벤트를 다시 받을 수 있으므로 업무 상태 전이와 유일성 제약으로 멱등성을 보장한다.
- 이미 완료된 작업이면 비즈니스 변경 없이 성공 처리한다.

### 3-3. Publication scheduler

- 미완료 publication은 Spring Modulith Registry의 재발행 기능으로 처리한다.
- 현재 scheduler의 실행 주기는 60초이며, 30초보다 오래 미완료인 publication을 재발행한다.
- 완료 후 7일이 지난 publication은 정리한다.
- `PENDING`, `PUBLISHED`, `FAILED`라는 별도 업무 상태와 retry count는 기본 계약에 포함하지 않는다.

## 4. 최소 Saga 흐름

P6는 중앙 Saga coordinator나 Saga 상태 저장소를 사용하지 않고, 이벤트 choreography로 보상 흐름을 연결한다.

```text
Order PENDING
  → PaymentRequestedEvent
  → PaymentCompletedEvent
  → 재고 차감
  → StockDeductedEvent

재고 차감 실패
  → StockDeductionFailedEvent
  → 결제 환불
  → PaymentRefundedEvent
  → 주문 취소
  → OrderCanceledEvent
```

| 이벤트 | 발행 주체 | 소비 주체 | 의미 |
|---|---|---|---|
| `PaymentRequestedEvent` | Order | Payment | 결제 처리 요청 |
| `PaymentCompletedEvent` | Payment | Order, Inventory, Delivery | 결제 성공 사실 |
| `PaymentFailedEvent` | Payment | Order | 결제 실패 사실 |
| `StockDeductedEvent` | Inventory | Order | 재고 차감 성공 사실 |
| `StockDeductionFailedEvent` | Inventory | Payment, Order | 환불·주문 취소 보상 원인 |
| `PaymentRefundedEvent` | Payment | Order | 환불 완료 사실 |
| `OrderCanceledEvent` | Order | Coupon, Point | 쿠폰·포인트 복원 후속 작업 |
| `DeliveryCompletedEvent` | Delivery | Point, Review | 적립·리뷰 자격 활성화 |
| `UserRolesChangedEvent` | User | Auth | 역할 변경 후 세션 무효화 |

- 각 소비자는 자기 모듈의 로컬 트랜잭션만 처리한다.
- 보상 이벤트도 일반 이벤트와 동일하게 publication 재처리·멱등성 규칙을 적용한다.
- Saga 전체 상태 조회, 단계별 retry count, 수동 보상 재시도가 필요해질 때만 별도 projection 또는 저장 모델을 추가한다.

## 5. HTTP API와 예외

- P6는 일반 고객용 HTTP API를 제공하지 않는다.
- 이벤트 publication 재처리도 기본적으로 Spring Modulith scheduler가 담당한다.
- 관리자 조회·수동 재시도가 필요해질 때 P7이 HTTP 진입점을 정의한다.
- 별도 Saga 상태 저장을 도입하기 전에는 `SagaInstance`, `SagaStep`, `SAGA_NOT_RETRYABLE`, `COMPENSATION_FAILED`를 P6의 필수 API 계약으로 정의하지 않는다.
- 공통 인증·권한·HTTP 예외는 [P7 Access](../p7/p7-access.md)와 [P7 Operations](../p7/p7-operations.md)가 소유한다.
