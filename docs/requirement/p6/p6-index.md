# P6 이벤트 신뢰성·Saga 문서 안내

P6는 Spring Modulith 기반 모듈 간 이벤트 전달의 신뢰성, 이벤트 소비 멱등성, 여러 업무 모듈에 걸친 Saga 보상 흐름을 정의한다. 별도의 P6 업무 Entity를 기본으로 두지 않으며, 일반 고객용 기능이 아니라 도메인 모듈과 운영 기능이 사용하는 내부 인프라다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P6 Policy](p6-policy.md) | 정책 | 범위·책임, 용어, 이벤트 원자성·멱등성·재시도, Saga 상태와 보상 규칙 |
| [P6 Infrastructure](p6-infrastructure.md) | 리소스·내부 계약 | Event Publication Registry, 이벤트 record, listener·보상 이벤트 계약 |

정책을 먼저 확정하고, 리소스·내부 계약 문서에서 정책을 만족하는 저장 데이터와 모듈 간 인터페이스를 정의한다.

## 2. 책임과 경계

| 책임 | 담당 | 참조 |
|---|---|---|
| 이벤트 발행 신뢰성과 미완료 publication 재처리 | P6 | [P6 Policy](p6-policy.md) |
| 이벤트 payload와 공개 이벤트 계약 | 이벤트를 발행하는 원본 도메인·P6 공통 계약 | [P6 Infrastructure](p6-infrastructure.md) |
| 이벤트 소비 후 Order·Payment·Delivery·Inventory 상태 변경 | 각 원본 도메인 | [P5 Policy](../p5/p5-policy.md), [P9 Inventory](../p9/p9-inventory.md) |
| Saga 이벤트 흐름·보상 규칙 | P6 정책과 각 업무 도메인 | [P6 Policy](p6-policy.md) |
| 관리자 HTTP 조회·재시도 진입점 | P7 Admin | [P7 Operations](../p7/p7-operations.md) |

- P6는 별도의 Saga·소비 기록 Entity를 기본으로 소유하지 않는다.
- P6는 다른 도메인의 Entity, Repository, 내부 서비스, 상태값을 소유하거나 직접 호출하지 않는다.
- P6의 기본 이벤트 저장·재처리 수단은 Spring Modulith Event Publication Registry다. 이는 프로젝트가 별도로 정의한 `outbox_events` 모델과 구분한다.
- 모듈 간 통신은 공개 이벤트 또는 명시된 Named Interface만 사용한다.
- 공통 URI, 성공 응답, 예외 응답, 인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P6는 고객용 HTTP API를 소유하지 않으며, 운영 API는 P7이 정의한다.

## 3. 작성 원칙

- 업무 데이터와 이벤트 publication 기록의 원자성은 하나의 로컬 트랜잭션으로 보장한다.
- 이벤트는 불변 사실을 전달하는 것을 기본으로 한다. `Requested`처럼 요청 의미로 사용하는 이벤트는 요청 주체·처리 주체·실패 의미를 이 문서에 명시한다.
- Spring Modulith publication 식별자와 payload의 업무 `eventId`를 혼동하지 않는다. 멱등 처리 키는 이벤트 payload의 업무 식별자 또는 원본 도메인의 유일성 제약으로 결정한다.
- 이벤트 소비자는 재발행·동시 실행·프로세스 장애로 같은 이벤트를 다시 받을 수 있다는 전제에서 구현한다.
- Saga 보상은 이미 완료된 보상 단계를 다시 실행하지 않으며, 실패한 단계부터 재개한다.
- P6 문서와 P7 운영 문서의 책임이 충돌하면 P6는 상태 전이·멱등성의 원본이고, P7은 HTTP 진입점과 관리자 권한의 원본이다.
