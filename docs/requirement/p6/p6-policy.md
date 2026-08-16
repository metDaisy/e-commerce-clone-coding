# P6 이벤트 신뢰성·Saga Policy

이 문서는 P6의 범위·책임과 API에 독립적인 업무 정책을 정의한다. Spring Modulith publication 저장과 이벤트 계약은 [P6 Infrastructure](p6-infrastructure.md), 관리자 HTTP 진입점은 [P7 Operations](../p7/p7-operations.md)을 따른다.

## 1. 범위와 책임

### 범위

- Spring Modulith Event Publication Registry를 이용한 모듈 이벤트의 트랜잭션 원자성
- 미완료 이벤트 publication의 재발행과 완료 publication의 보존·정리
- 이벤트 소비자의 멱등 처리 계약
- 결제 완료 이후 재고 처리 실패처럼 여러 모듈의 보상이 필요한 Saga choreography

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| 이벤트 publication 기록과 미완료 publication 재처리 | Spring Modulith Registry와 scheduler | [P6 Infrastructure](p6-infrastructure.md) |
| 이벤트를 발행할 사실과 payload 생성 | 원본 도메인 | [P5 Policy](../p5/p5-policy.md), [P9 Policy](../p9/p9-policy.md) |
| 이벤트 소비 후 업무 상태 변경 | 소비자 도메인 | 해당 도메인 Policy와 API 문서 |
| Saga 이벤트 흐름과 보상 규칙 | P6 정책·각 업무 도메인 | [P6 Infrastructure](p6-infrastructure.md) |
| 운영자 인증·HTTP 조회·수동 재시도 | P7 Admin | [P7 Operations](../p7/p7-operations.md) |

P6는 Order·Payment·Delivery·Inventory의 업무 상태를 직접 변경하지 않는다. 각 상태 전이는 해당 도메인의 application interface 또는 이벤트 소비자가 소유한다.

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Domain Event` | 원본 도메인에서 이미 발생한 업무 사실을 전달하는 불변 메시지 |
| `Request Event` | 다른 모듈의 작업을 요청하는 이벤트. 사실 이벤트와 달리 요청 주체·처리 주체·실패 의미를 함께 정의한다. |
| `Event Publication Registry` | 이벤트와 transactional listener별 publication 및 완료 여부를 저장하는 Spring Modulith 기능. 기본 테이블은 `event_publication`이다. |
| `Event Consumer` | 공개 이벤트를 받아 자기 모듈의 상태를 변경하는 모듈 listener |
| `Saga` | 여러 로컬 트랜잭션을 이벤트로 연결하고 실패 시 보상 이벤트를 실행하는 흐름 |
| `P6` | publication 재처리와 Saga 이벤트 계약을 정의하는 내부 정책 |
| `P7 Admin` | 실패한 이벤트와 업무 상태를 조회하는 HTTP 진입점 |

`event_publication.id`는 listener별 publication 식별자이며, 소비 멱등성에 사용하는 업무 `eventId`와 동일하다고 가정하지 않는다. 업무 `eventId`는 원본 도메인이 이벤트 payload에 생성·포함한다.

## 3. 핵심 업무 규칙

### 3-1. 모듈 이벤트와 원자성

1. 원본 도메인은 자기 로컬 업무 데이터를 저장하는 트랜잭션 안에서 이벤트를 발행한다.
2. Spring Modulith Registry는 해당 트랜잭션 안에서 이벤트 publication 기록을 남긴다.
3. 업무 데이터 저장이 커밋됐지만 publication 기록이 없는 상태를 허용하지 않는다.
4. 다른 모듈의 내부 Bean, Entity, Repository를 직접 호출하지 않는다.
5. 즉시 답이 필요한 조회·검증은 Named Interface로 요청하고, 후속 작업 통지는 공개 이벤트로 전달한다.
6. 이벤트 payload에는 persistence Entity를 포함하지 않는다. 식별자, 버전, 시각, 추적 ID, 업무에 필요한 값만 포함한다.

### 3-2. 이벤트 식별·버전·추적

- 모든 공개 이벤트는 업무 `eventId`를 UUID로 갖는다.
- `eventType`과 `eventVersion`은 payload 계약의 일부다.
- `aggregateType`과 `aggregateId`는 이벤트가 발생한 원본 업무 대상을 식별한다.
- `occurredAt`은 원본 도메인에서 이벤트가 발생한 시각이다.
- `correlationId`는 하나의 사용자 요청·업무 흐름을 연결한다.
- `causationId`는 현재 이벤트를 발생시킨 직전 이벤트 또는 요청을 가리킨다.
- payload에는 비밀번호, Access Token, Refresh Token, OAuth secret, 세션 비밀값, 카드번호, CVC 원문을 포함하지 않는다.
- 호환 가능한 필드 추가는 기존 소비자를 깨뜨리지 않아야 한다. 의미 변경이나 필드 삭제는 새 `eventVersion`으로 분리한다.

### 3-3. publication 재처리

- listener 성공 전의 publication은 미완료 상태로 남는다.
- listener가 실패하면 해당 listener의 로컬 트랜잭션을 롤백하고 publication을 완료 처리하지 않는다.
- 미완료 publication은 scheduler가 재발행할 수 있다. 같은 publication이 여러 애플리케이션 인스턴스에서 동시에 재실행되지 않도록 publication 상태 또는 실행 선점을 원자적으로 확인한다.
- 현재 Spring Modulith scheduler 구현의 기본 주기는 60초이며, 30초보다 오래 미완료인 publication을 재발행하고, 완료 후 7일이 지난 publication을 정리한다. 이 값은 운영 설정으로 관리하고 P6 운영 문서에 노출한다.
- Spring Modulith publication registry에는 프로젝트가 별도로 정의한 `PENDING → PUBLISHED → FAILED` 업무 상태를 기본으로 추가하지 않는다.
- bounded retry, `FAILED` 운영 상태, 단계별 수동 재시도가 필요해지는 시점에만 별도 projection 또는 custom model을 검토한다.

### 3-4. 소비 멱등성

1. 소비자는 이벤트의 업무 식별자와 자기 도메인의 현재 상태를 함께 확인한다.
2. 이미 처리된 상태라면 비즈니스 작업을 반복하지 않고 성공으로 끝낸다.
3. 별도 소비 기록을 만들지 않는 경우에도 기존 도메인 Entity의 상태 전이·유일성 제약으로 중복 실행을 방지한다.
4. 처리 도중 실패하면 listener 트랜잭션을 롤백하고 publication 재처리 대상이 된다.
5. 소비자는 이벤트 순서를 전역적으로 보장한다고 가정하지 않는다. 순서가 필요한 경우 상태 조건과 이벤트 식별자를 검증한다.

### 3-5. Saga choreography

P6는 중앙 Saga coordinator나 Saga 상태 Entity를 기본으로 두지 않는다. 각 모듈의 listener가 자기 로컬 트랜잭션을 처리하고 다음 사실 이벤트 또는 보상 이벤트를 발행한다.

정상 결제 흐름:

```text
PENDING Order
  → PaymentRequestedEvent (요청)
  → Payment PROCESSING
  → Payment Simulator Webhook
  → PaymentCompletedEvent (결제 성공 사실)
  → Order PAID
  → 재고 차감·Delivery PREPARING
```

재고 차감 실패 보상 흐름:

```text
PaymentCompletedEvent 이후 재고 차감 실패
  → 결제 환불 요청
  → PaymentRefundedEvent
  → OrderCanceledEvent
  → 쿠폰 복원
  → 포인트 복원
```

- 결제 성공·주문 확정·배송 생성·재고 차감의 원본 상태 전이는 P5와 P9가 소유한다.
- 각 listener는 독립 로컬 트랜잭션으로 처리한다.
- 보상 이벤트도 동일한 멱등성 규칙을 적용한다.
- 이미 완료된 보상 작업은 도메인 상태 확인을 통해 no-op 처리한다.
- 보상 실패의 재처리와 관리자 알림은 기본 범위에 포함하지 않는다. 필요해지는 시점에 별도 상태 저장 모델을 추가한다.

## 4. 불변식과 상태 전이

### 불변식

- 하나의 업무 이벤트는 하나의 업무 `eventId`를 가진다.
- 하나의 소비자는 동일한 업무 `eventId`를 성공 처리할 수 있다.
- publication 완료는 listener의 로컬 업무 트랜잭션이 성공한 뒤에만 기록한다.
- Saga step의 완료 상태는 해당 step의 업무 결과가 성공적으로 커밋됐을 때만 기록한다.
- 보상 step은 원래 step의 완료 사실과 보상 idempotency key를 확인한 뒤 실행한다.
- 민감 정보는 이벤트 payload, publication 조회, Saga 오류 요약에 포함하지 않는다.

### Publication 상태

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| 미완료 publication | listener 업무 트랜잭션 성공 | 완료 | Spring Modulith listener interceptor |
| 미완료 publication | listener 예외 또는 프로세스 장애 | 미완료 유지 | Spring Modulith Registry·scheduler |
| 완료 publication | 보존 기간 경과 | 삭제 | P6 정리 scheduler |

## 5. 도메인 간 규칙과 예외 소유권

- P5는 주문·결제·배송 상태와 Payment Simulator Webhook을 소유한다. P6는 이벤트 publication 재처리와 보상 이벤트 계약을 정의한다.
- P9는 Offer·Inventory의 가격·판매 상태·재고 원본을 소유한다. P6는 재고 차감 실패를 보상 흐름의 원인으로만 사용한다.
- P4는 쿠폰 원본과 복원 규칙을 소유한다. P6는 `OrderCanceledEvent`를 전달하고 쿠폰 내부 모델을 직접 변경하지 않는다.
- P1은 포인트 원본과 복원 규칙을 소유한다. P6는 포인트 내부 모델을 직접 변경하지 않는다.
- P7은 `ADMIN` 권한, 운영 HTTP API, client message를 소유한다. 별도 Saga 상태 저장을 도입하기 전에는 P6 Saga 자체의 조회·수동 재시도를 제공하지 않는다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P5 Order·Payment·Delivery | 결제 성공·실패와 주문·배송 상태 전이 | [P5 Policy](../p5/p5-policy.md), [Payment Process](../p5/p5-payment-process.md) |
| P9 Offer·Inventory | 결제 후 재고 차감과 실패 보상 | [P9 Policy](../p9/p9-policy.md), [P9 Inventory](../p9/p9-inventory.md) |
| P4 Coupon | 주문 취소 후 쿠폰 복원 | [P4 Policy](../p4/p4-policy.md) |
| P1 User | 주문 취소 후 포인트 복원 | [P1 User](../p1/p1-user.md) |
| P7 Admin | 실패 조회와 허용된 재시도 | [P7 Operations](../p7/p7-operations.md) |

## 6. API 문서와의 관계

- P6는 일반 고객용 HTTP API를 제공하지 않는다.
- 관리자 Outbox·Saga 조회와 재시도 URI, 관리자 권한, HTTP 응답은 [P7 Operations](../p7/p7-operations.md)에서 정의한다.
- P7이 사용하는 조회 DTO는 P6가 소유한 상태와 안전한 오류 요약만 노출하며, 전체 serialized event나 민감 payload를 그대로 반환하지 않는다.
- P6 정책과 P7 운영 API가 충돌하면 상태 전이·멱등성은 이 문서를, HTTP 경계·권한·client message는 P7 문서를 기준으로 조정한다.
