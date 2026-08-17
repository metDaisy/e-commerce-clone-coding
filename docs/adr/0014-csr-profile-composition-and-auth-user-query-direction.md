# ADR-0014: CSR 프로필 조합과 User·Auth 조회 책임 분리

- Status: Accepted
- Date: 2026-08-17
- Deciders: User and Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

P1 User는 프로필, 역할, 활성 상태를 소유한다. P11 Auth는 로컬 로그인 이메일을 포함한 `UserCredential`, 소셜 인증수단, 로그인 세션과 재인증 Grant를 소유한다.

`loginEmail`을 P1 `GET /api/v1/me` 응답에 넣으면 User API가 Auth 소유 데이터를 노출하게 된다. 이를 서버에서 보강하려고 User가 Auth를 동기 조회하면, 현재 필요한 Auth → User 역할·활성 상태 조회와 반대 방향 의존이 함께 생긴다. Modulith 모듈 순환과 소유권 혼동으로 이어진다.

프로젝트의 클라이언트는 SPA CSR이다. 프로필 화면은 두 API를 병렬 호출해 화면 전용 모델을 조합할 수 있다.

## Decision Drivers

- User와 Auth의 데이터 소유권을 한 방향으로 유지한다.
- Auth가 로그인·토큰 갱신에서 현재 역할과 활성 상태를 확인할 수 있어야 한다.
- 민감한 인증수단 정보는 재인증된 사용자에게만 최소한으로 공개한다.
- 단일 SPA 화면을 위해 서버 조합 계층을 성급히 만들지 않는다.

## Considered Options

### Option A: P1 User가 Auth를 동기 조회해 `loginEmail`을 보강한다

거절한다. Auth → User와 User → Auth가 모두 필요한 구조가 되어 모듈 순환이 생기며, User API가 Auth 데이터의 공개 정책까지 떠안는다.

### Option B: 지금 BFF 또는 Account composition API를 만든다

보류한다. 현재 소비자는 SPA 하나이고 병렬 호출로 충분하다. 다수 클라이언트가 같은 조합·권한 규칙을 반복할 때 도입한다.

### Option C: CSR 클라이언트가 P1과 P11 공개 API를 병렬 조합한다

채택한다. 각 API가 자신이 소유한 정보만 반환하고, 화면 모델 조합은 클라이언트의 책임으로 둔다.

## Decision

- P1 `GET /api/v1/me`은 `loginEmail`을 반환하지 않는다.
- P11 `GET /api/v1/auth/me/credential-summary`가 nullable `loginEmail`만 반환한다. 비밀번호, 해시, OAuth 공급자·식별자, 토큰, Grant 값은 반환하지 않는다.
- 두 API는 로그인 사용자와 `USER_ACCOUNT_MANAGEMENT` 목적의 유효한 `__Host-REAUTH` 쿠키를 요구한다.
- CSR 클라이언트는 두 API를 병렬 호출한다. 어느 한 요청의 재인증이 실패하면 부분 프로필을 표시하지 않고 재인증을 유도한다.
- Auth는 현재 역할·활성 상태를 확인하기 위해 User의 작은 공개 seam을 동기 조회할 수 있다. User는 단순 프로필 보강을 위해 Auth를 동기 조회하지 않는다.
- 역할 변경·계정 비활성화는 User가 사실 이벤트로 알리고 Auth가 세션 무효화에 반영한다.

## Consequences

### Positive

- 데이터 소유권과 모듈 의존 방향이 명확하다.
- `loginEmail` 공개 범위를 재인증으로 제한할 수 있다.
- 화면 초기 조회는 병렬 호출로 처리할 수 있다.

### Negative

- 클라이언트가 두 응답을 조합하고 오류 상태를 함께 처리해야 한다.
- 다른 클라이언트가 생기면 조합 로직이 중복될 수 있다.

## Follow-up

- P11 인증수단 요약 API와 재인증 검증을 구현하고 경계 테스트를 작성한다.
- SPA 프로필 조회에서 두 요청을 병렬화하고 재인증 실패 시 공통 흐름으로 처리한다.
- 조합 로직이 복수 클라이언트에 반복되면 BFF/Account composition API ADR을 새로 제안한다.

## Evidence

- [P1 User API](../requirement/p1/p1-user.md)
- [P11 Credential API](../requirement/p11/p11-credential.md)
- [Architecture](../architecture.md)
