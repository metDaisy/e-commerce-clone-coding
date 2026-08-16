# ADR-0009: 사용자 역할 변경 이벤트와 세션 무효화

- Status: Accepted
- Date: 2026-08-16
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

사용자 역할 집합은 P1 User가 소유하지만 역할 추가·삭제 HTTP 진입점은 P7 Admin이 제공한다. 역할 집합이 바뀐 뒤 기존 Access Token이 만료 시각까지 유효하면 이전 권한으로 API를 계속 호출하거나, Refresh Token으로 이전 역할 집합의 세션을 연장할 수 있다.

P7이 P11 Auth의 세션 저장소를 직접 호출하면 관리자 모듈과 인증 모듈의 내부 구현이 결합된다. 또한 역할 변경 사실을 다른 모듈이 감사·운영 목적으로 사용할 수 있어 변경 사실을 이벤트로 남길 필요가 있다.

## Decision Drivers

- 권한 변경 직후 기존 모든 기기의 인증 세션을 무효화한다.
- P7과 P11의 내부 구현 의존을 만들지 않는다.
- 역할 변경과 이벤트 기록의 유실을 허용하지 않는다.
- 이벤트 재전달에도 세션 무효화가 중복 부작용 없이 동작해야 한다.

## Considered Options

### Option A: P7이 Auth 세션 저장소를 직접 호출

즉시 무효화는 단순하지만 P7이 P11의 내부 세션 구현에 의존하고 모듈 경계가 깨진다.

### Option B: `UserRolesChangedEvent` 발행 및 Auth 소비

User가 역할 변경 사실을 이벤트로 발행하고 Auth가 세션 무효화를 담당한다. Outbox를 사용하면 역할 변경과 이벤트 기록을 같은 트랜잭션으로 보장할 수 있다.

## Decision

- P1 User는 실제 역할 집합 변경을 저장한 뒤 `UserRolesChangedEvent`를 발행한다.
- 이벤트 payload는 `userId`, `previousRoles`, `roles`, `addedRoles`, `removedRoles`, `changedByUserId`, `changedAt`로 제한한다. 인증 비밀값은 포함하지 않는다.
- 역할 집합 변경과 Outbox 기록은 같은 트랜잭션으로 처리한다.
- P11 Auth는 권한 변경 처리 중 이벤트를 동기 소비해 해당 사용자의 모든 Access Token·Refresh Token·기기 세션을 무효화한다. 이 처리가 실패하면 역할 변경을 성공으로 커밋하지 않는다.
- 커밋 이후 장애나 재전달은 Outbox가 담당하며, Auth 소비자는 `eventId` 기준으로 멱등 처리한다.
- 기존 토큰은 만료 전에도 사용할 수 없고 Refresh할 수 없으며, 사용자는 다시 로그인해야 한다.
- P7은 P11 Auth의 내부 세션 저장소를 직접 호출하지 않는다.

## Consequences

### Positive

- 역할 변경과 인증 세션 무효화의 책임이 P1/P11 경계에 맞게 분리된다.
- 이벤트 Outbox로 역할 변경 사실의 유실을 방지하고 운영 추적·재시도를 지원할 수 있다.
- 이벤트 소비자가 추가되어도 P7 API의 내부 의존성이 늘어나지 않는다.

### Negative

- 이벤트 소비 실패·재시도와 멱등성 테스트가 필요하다.
- 이벤트 처리 시각과 API 응답 시각 사이의 운영 상태를 추적해야 한다.

### Follow-up

- `UserRolesChangedEvent` 발행·소비와 기존 토큰 즉시 거부를 테스트한다.
- P6 Outbox에 `USER` aggregate type과 이벤트 처리 기록을 추가한다.
- Modulith 공개 이벤트 계약과 허용 의존성을 `ApplicationModules.verify()`로 검증한다.

## Evidence

- [P1 User 권한](../requirement/p1/p1-user.md)
- [P6 Outbox & Saga](../requirement/p6/p6-infrastructure.md)
 - [P7 사용자 권한 변경](../requirement/p7/p7-access.md)
- [P11 Auth 로그인·토큰](../requirement/p11/p11-session.md)
- [Architecture](../architecture.md)
