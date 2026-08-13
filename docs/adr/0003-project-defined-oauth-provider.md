# ADR-0003: OAuth provider는 프로젝트 정의 enum으로 관리

- Status: Accepted
- Date: 2026-08-09
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

소셜 회원가입 요구사항에서 `provider`는 프로젝트가 임의로 정한 필드다. 외부 OAuth 공급자의 응답 필드를 그대로 내부 사용자 모델에 노출하면 공급자별 필드 차이와 이메일 변경·미제공 문제에 영향을 받는다.

또한 Amazon.com의 일반 회원가입과 Login with Amazon은 구분해야 한다. Login with Amazon은 제3자 웹사이트와 앱이 Amazon 계정으로 인증받기 위한 OAuth 서비스이며, Amazon.com 자체가 Google·Naver·Kakao 회원가입을 제공한다는 근거로 사용할 수 없다.

## Decision Drivers

- 공급자별 사용자 식별자를 안정적으로 저장해야 한다.
- 이메일을 외부 계정의 영구 식별자로 사용하지 않는다.
- OAuth 토큰과 비밀값이 사용자 도메인이나 로그로 유출되지 않아야 한다.
- 초기 구현에서는 정해진 공급자 목록만 지원하고, Amazon 연동은 확장 가능성으로 남긴다.

## Considered Options

### Option A: 외부 공급자의 이름과 응답 필드를 그대로 저장

연동 초기에는 빠르지만 공급자 변경에 내부 모델이 종속되고 식별자 규칙이 일관되지 않는다.

### Option B: 프로젝트 enum과 불투명한 providerId로 정규화

내부 모델은 안정적이지만 공급자별 OAuth adapter와 매핑 규칙을 별도로 구현해야 한다.

## Decision

Option B를 선택한다.

- `provider`는 프로젝트 정의 enum이며 기본 값은 `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB`다.
- `providerId`는 공급자가 반환한 불투명한 외부 사용자 식별자이며 이메일을 저장하지 않는다.
- `(provider, providerId)` 조합은 UNIQUE로 관리한다.
- 공급자 access token, refresh token, client secret은 사용자 도메인과 로그에 저장하지 않는다.
- `AMAZON`은 심화 연동 후보로만 정의한다. 연동 시 Login with Amazon의 `user_id`를 `providerId`로 매핑하고, Amazon 고객 계정 자체를 우리 서비스의 User로 간주하지 않는다.
- 소셜 회원가입은 `User`와 `SocialCredential`만 생성한다. `UserCredential`은 로컬 이메일·비밀번호 인증수단이므로 소셜 회원가입에서 생성하지 않는다.
- 신규 소셜 사용자는 OAuth callback에서 즉시 정식 회원으로 생성하지 않고 회원가입 화면으로 이동한다. 사용자가 추가 프로필 정보를 제출하면 그때 `User`와 `SocialCredential`을 생성한다. 기존 `(provider, providerId)`가 있으면 회원가입 화면 없이 로그인한다.

## Consequences

### Positive

- 공급자별 응답 형식이 내부 User 모델에 전파되지 않는다.
- 동일한 사용자가 여러 OAuth 공급자를 연결하는 규칙을 일관되게 적용할 수 있다.
- Amazon 연동을 추가해도 기존 회원가입 모델을 바꾸지 않고 adapter를 추가할 수 있다.

### Negative

- 공급자별 callback 검증과 계정 연결 정책을 구현해야 한다.
- provider enum을 추가할 때 migration, 허용 목록, callback 검증을 함께 변경해야 한다.
- providerId만으로 서로 다른 공급자의 계정을 자동 병합할 수 없다.

### Follow-up

- OAuth callback state, redirect URI, token 교환은 auth 모듈 내부에 둔다.
- 계정 연결 시 기존 User 확인과 신규 User 생성의 충돌을 멱등하게 처리한다.
- `AMAZON`을 실제로 도입할 때 별도 연동 테스트와 보안 검토를 추가한다.

## Evidence

- [P11 인증 요구사항](../requirement/p11-auth.md)
- [Login with Amazon 공식 문서](https://developer.amazon.com/docs/login-with-amazon/documentation-overview.html)
