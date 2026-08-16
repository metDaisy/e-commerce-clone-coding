# ADR-0010: 가산형 사용자 역할 집합

- Status: Accepted
- Date: 2026-08-16
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

기존 User 모델은 `role` 하나만 저장해 `USER`와 `PRODUCT_MANAGER`를 서로 대체하는 방식이었다. 그러나 한 User가 구매자이면서 판매자일 수 있고, 플랫폼 운영 역할도 기존 구매자·판매자 역할과 독립적으로 부여될 수 있다.

단일 역할을 유지하면 판매자 승인 시 구매자 권한을 잃거나, 관리자 역할을 부여할 때 기존 판매자 권한을 별도로 복구해야 한다.

## Decision

- User는 복수의 역할을 동시에 보유하는 역할 집합을 가진다.
- `USER`는 모든 User가 기본으로 보유하는 구매자 역할이며 삭제할 수 없다.
- `PRODUCT_MANAGER`는 `ACTIVE Seller`가 있을 때 추가하고, Seller가 정지·승인 취소되면 제거한다. 이때 `USER`는 유지한다.
- `ADMIN`은 플랫폼 운영 역할이며 다른 역할과 함께 보유할 수 있다.
- 관리자 역할 API는 역할 교체가 아니라 역할 추가·삭제 API로 제공한다.
- JWT와 인증 응답은 단일 `role` 대신 전체 `roles` 배열을 사용한다.
- 역할 집합이 실제로 변경되면 `UserRolesChangedEvent`를 발행하고, 기존 로그인 세션을 모두 무효화한다.

## Consequences

### Positive

- 구매자·판매자·관리자 기능을 독립적으로 조합할 수 있다.
- 판매자 승인·정지 시 구매자 기능을 보존할 수 있다.
- 토큰과 권한 검사가 실제 User의 전체 권한 집합을 표현한다.

### Negative

- `users.role` 단일 컬럼과 JWT `role` claim을 다중 값 구조로 변경해야 한다.
- 모든 권한 검사와 역할 변경 이벤트가 단일 값이 아닌 집합을 처리해야 한다.
- 역할 제거와 세션 무효화에 대한 멱등성·경계 테스트가 필요하다.

## Follow-up

- User 역할 저장 구조와 Flyway 마이그레이션을 설계한다.
- `user-api`, 인증 DTO, JWT 생성·검증, Security 권한 검사를 `roles` 집합 기준으로 변경한다.
- P7 역할 추가·삭제 API와 Seller 상태 전이 테스트를 추가한다.

## Evidence

- [P1 User](../requirement/p1/p1-user.md)
 - [P7 권한·판매자 심사](../requirement/p7/p7-access.md)
- [P11 Auth](../requirement/p11/p11-index.md)
- [Project Context](../../CONTEXT.md)
