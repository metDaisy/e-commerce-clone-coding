# ADR-0015: Address를 User와 별도 모듈로 분리

- Status: Accepted
- Date: 2026-08-18
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

P1 문서는 User와 Address를 함께 다루지만, Address는 User API에 중첩되지 않는 독립 리소스다. Address는 별도의 등록·수정·삭제 생명주기와 기본 배송지 불변식을 가지며, 주문 시점에는 현재 값을 별도 스냅샷으로 복사한다.

구현에서 Address를 `user` 모듈에 함께 두면 같은 요구사항 그룹이라는 이유로 User 엔티티·저장소와 결합되기 쉽다. 그러나 Address가 User의 필드를 직접 사용하지 않는 현재 모델에서는 두 엔티티를 같은 aggregate로 취급할 이유가 없다.

## Decision Drivers

- User와 Address의 생명주기와 저장 구조를 독립적으로 변경한다.
- User 엔티티와 Address persistence 구조의 결합을 줄인다.
- P1 요구사항 그룹과 Java 구현 모듈의 경계를 독립적으로 관리한다.
- 향후 주문 배송지 스냅샷과 주소 관리 기능을 별도로 확장한다.

## Considered Options

### Option A: User 모듈 안에 Address를 유지

구현 초기에는 파일 수가 적지만 Address 서비스·저장소·예외가 User 모듈의 내부 구조와 함께 확장된다. User와 Address를 하나의 aggregate로 오해하기 쉽다.

### Option B: Address를 별도 Modulith 모듈로 분리

P1 문서 그룹은 유지하면서 Java 최상위 패키지와 모듈을 `address`로 분리한다. Address는 `userId`만 저장하고 User 엔티티를 직접 참조하지 않는다.

## Decision

Option B를 선택한다.

- Address의 구현은 `io.github.metdaisy.amaazon.address` 모듈이 소유한다.
- `Address`는 User 엔티티 대신 `UUID userId`로 소유자를 식별한다.
- Address API의 외부 URI는 기존 P1 계약인 `/api/v1/me/addresses`를 유지한다.
- Address 모듈은 `common`의 공개 인터페이스만 사용하며 User 모듈의 내부 Entity·Repository를 참조하지 않는다.
- 향후 User의 활성 상태나 존재 여부 확인이 필요하면 공개 Query Port 또는 인증 경계의 계약을 사용한다.
- P1은 요구사항·제품 범위 그룹으로 유지하고, 구현 모듈 경계와 동일하다고 간주하지 않는다.

## Consequences

### Positive

- Address CRUD와 기본 배송지 규칙을 User 변경과 독립적으로 구현·검증할 수 있다.
- User 엔티티를 Address 조회에 로딩하지 않아 불필요한 연관관계와 쿼리를 피한다.
- 향후 P5 주문 스냅샷이 Address API 계약만 사용하도록 경계를 명확히 한다.

### Negative

- P1 안에 `user`와 `address` 두 구현 모듈이 생긴다.
- User 존재 여부나 활성 상태를 Address가 직접 확인해야 하는 경우 공개 Port 또는 별도 협력이 필요하다.
- Address 전용 예외 코드 타입이 추가된다.
