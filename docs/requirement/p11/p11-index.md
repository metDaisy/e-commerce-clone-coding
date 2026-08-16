# P11 Auth 문서 안내

P11은 사용자의 인증수단과 인증 세션을 관리한다. User 프로필·역할·활성 상태는 P1 User가 소유하고, P11은 인증이 완료된 `userId`와 인증 결과만 공개한다.

공통 URI, 성공 응답, 예외 응답, 인증 주체와 권한 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P11 Policy](p11-policy.md) | 정책 | 범위·책임, 인증수단, 가입·로그인·세션 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [Credential API](p11-credential.md) | 인증수단 모델·API | UserCredential·SocialCredential, 비밀번호 검증·수정 |
| [Sign-up API](p11-signup.md) | 가입 세션 모델·API | 로컬·소셜 가입, 이메일 OTP, Guest Token |
| [Session API](p11-session.md) | 로그인 세션 모델·API | 로그인, Access·Refresh Token, 로그아웃·세션 무효화 |

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| `User` 프로필·역할·활성 상태 원본 | P1 User | [P1 Policy](../p1/p1-policy.md), [User API](../p1/p1-user.md) |
| 이메일·비밀번호·OAuth 식별자 원본 | P11 Auth | [P11 Policy](p11-policy.md), [Credential API](p11-credential.md) |
| 가입 세션·OTP·Guest Token | P11 Auth | [P11 Policy](p11-policy.md), [Sign-up API](p11-signup.md) |
| Access·Refresh Token과 로그인 세션 무효화 | P11 Auth | [P11 Policy](p11-policy.md), [Session API](p11-session.md) |
| 역할 변경 사실과 관리자 진입점 | P1 User·P7 Admin | [P7 Access](../p7/p7-access.md) |
| 역할 변경 후 세션 무효화 이벤트 전달·재시도 | P6 Infrastructure·P11 Auth | [P6 Infrastructure](../p6/p6-infrastructure.md), [ADR 0009](../../adr/0009-user-role-change-event-and-session-invalidation.md) |
| 로그인 성공 후 비회원 Cart 병합 | P3 Cart·P11 Auth | [Cart API](../p3/p3-cart.md) |

- P11은 P1의 `User` 내부 모델·Repository를 소유하거나 직접 참조하지 않는다.
- P11이 외부 User를 확인할 때는 공개 계약으로 `userId`, 활성 상태, 역할 집합만 사용한다.
- 비밀번호·OTP·토큰·OAuth 공급자 비밀값은 응답·로그·이벤트 payload에 평문으로 포함하지 않는다.

## 3. 문서 작성 순서

1. [P11 Policy](p11-policy.md)에서 인증수단·세션의 책임과 업무 규칙을 확정한다.
2. [Credential API](p11-credential.md)에서 장기 인증수단을 정의한다.
3. [Sign-up API](p11-signup.md)와 [Session API](p11-session.md)에서 각 생명주기의 API 계약을 정의한다.
4. API별 성공 응답과 P11·외부 도메인 예외를 완성한다.
5. 이 문서의 문서 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 이 문서는 P11의 안내와 책임 경계만 작성하고, 정책·필드·API 계약을 중복하지 않는다.
- 정책 문서는 API 문서보다 우선한다. 충돌하면 정책을 기준으로 API 문서를 수정한다.
- 외부 도메인의 예외 코드·메시지·로그 규칙은 P11에서 재정의하지 않고 원본 문서를 참조한다.
- `loginEmail`은 P11의 로컬 인증수단 공개값이며 P1 `User`의 소유 필드가 아니다.
