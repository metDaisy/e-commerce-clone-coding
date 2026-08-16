# P11 Sign-up API

이 문서는 정식 User 생성 전 인증 흐름을 정의한다. 가입 규칙은 [P11 Policy](p11-policy.md), 인증수단 모델은 [Credential API](p11-credential.md), 공통 응답·인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. 이메일 인증은 활성 프로필이 `dev`일 때 생략하고, `prod` 및 그 외 프로필에서는 수행한다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `SignUpSession` | `prod` 가입의 입력·이메일 OTP·만료 상태 | 로컬 가입 시작·OTP 검증·재전송 |
| `Guest Token` | 신규 소셜 가입 완료 전의 임시 인증 | OAuth callback·소셜 가입 완료 |

`prod`에서는 가입 완료 전 정식 `User`를 만들지 않는다. `dev`에서는 이메일 인증을 생략하므로 가입 요청 검증 후 즉시 P1 User에 프로필 생성을 요청한다. 두 환경 모두 P11 Credential 연결은 가입 완료 흐름에서 처리한다.

## 2. 데이터 모델

### 2-1. `SignUpSession`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `sessionId` | UUID | 예 | 가입 흐름 식별자 |
| `email` | string | 예 | 정규화된 가입 이메일 |
| `name` | string | 예 | User 생성 요청에 전달할 이름 |
| `passwordHash` | string | 예 | 평문을 보관하지 않는 가입 비밀번호 해시 |
| `status` | enum | 예 | `EMAIL_VERIFICATION_PENDING`, `COMPLETED`, `EXPIRED` |
| `expiresAt` | Instant | 예 | 생성 후 24시간 뒤 |
| `otpExpiresAt` | Instant | 아니오 | 현재 OTP 만료 시각 |
| `otpAttempts` | integer | 예 | 현재 OTP 실패 횟수 |

`SignUpSession`은 `prod`에서만 생성한다. OTP 원문은 DB·로그·이벤트·응답에 남기지 않는다. Guest Token은 `provider`, `providerId`, `iat`, `exp`만 가입 완료 목적으로 담으며 목표 만료시간은 10분이다.

## 3. API 정의

### 3-1. 로컬 가입 시작

`POST /api/v1/auth/signup`

권한: 공개

요청:

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "Password1!"
}
```

#### 성공 응답: `202 Accepted` (`prod`)

```json
{
  "signupSessionId": "uuid",
  "status": "EMAIL_VERIFICATION_PENDING",
  "email": "u***@example.com",
  "expiresAt": "2026-08-10T12:00:00Z",
  "otpExpiresAt": "2026-08-09T12:10:00Z"
}
```

`dev`에서는 메일·OTP·가입 세션을 생성하지 않고 즉시 가입을 완료한다.

#### 성공 응답: `201 Created` (`dev`)

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "loginEmail": "user@example.com",
  "roles": ["USER"],
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-003` | 이름·이메일·비밀번호 형식 오류 | 가입 정보를 확인해 주세요. | 실패 필드 | 검증 원인과 requestId |
| 409 | `AUTH-004` | 정규화된 이메일 중복 | 이미 가입된 이메일입니다. | 없음 | 이메일 중복과 requestId |
| 503 | `AUTH-010` | 이메일 Provider 발송 실패 | 인증 메일을 보낼 수 없습니다. | 재시도 가능 여부 | Provider 오류와 requestId |

### 3-2. 이메일 OTP 검증 (`prod`)

`POST /api/v1/auth/signup/email/verify`

권한: 가입 세션

요청:

```json
{
  "signupSessionId": "uuid",
  "otp": "123456"
}
```

#### 성공 응답: `201 Created`

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "loginEmail": "user@example.com",
  "roles": "USER",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-005` | OTP 불일치 | 인증코드를 확인해 주세요. | 남은 시도 횟수 | 세션과 검증 원인 |
| 400 | `AUTH-006` | OTP 만료 | 인증코드가 만료되었습니다. | 재전송 안내 | OTP 만료와 requestId |
| 400 | `AUTH-007` | OTP 입력 횟수 초과 | 새 인증코드를 요청해 주세요. | 없음 | 시도 횟수와 세션 |
| 400 | `AUTH-008` | 가입 세션 만료·완료 | 가입을 다시 시작해 주세요. | 없음 | 세션 상태와 requestId |
| 409 | `AUTH-024` | `dev` 프로필에서 OTP API 호출 | 개발 프로필에서는 이메일 인증이 필요하지 않습니다. | 없음 | 활성 프로필과 requestId |

### 3-3. 이메일 OTP 재전송 (`prod`)

`POST /api/v1/auth/signup/otp/resend`

권한: 가입 세션

#### 성공 응답: `202 Accepted`

```json
{
  "status": "EMAIL_VERIFICATION_PENDING",
  "email": "u***@example.com",
  "otpExpiresAt": "2026-08-09T12:20:00Z",
  "nextResendAt": "2026-08-09T12:11:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-008` | 가입 세션이 유효하지 않음 | 가입을 다시 시작해 주세요. | 없음 | 세션 상태와 requestId |
| 429 | `AUTH-009` | 60초 쿨다운·시간당 한도 초과 | 잠시 후 다시 시도해 주세요. | `nextResendAt` | 제한 기준과 식별자 |
| 503 | `AUTH-010` | 이메일 Provider 발송 실패 | 인증 메일을 보낼 수 없습니다. | 재시도 가능 여부 | Provider 오류와 requestId |
| 409 | `AUTH-024` | `dev` 프로필에서 OTP API 호출 | 개발 프로필에서는 이메일 인증이 필요하지 않습니다. | 없음 | 활성 프로필과 requestId |

### 3-4. OAuth 인증 시작·callback

`GET /api/v1/auth/oauth/{provider}/authorize`

`GET /api/v1/auth/oauth/{provider}/callback`

권한: 공개

- 허용 provider는 `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB`다. `AMAZON`은 심화 연동 후 추가한다.
- authorize는 state와 redirect URI를 설정해 공급자로 `302 Found` redirect한다.
- callback에서 기존 `(provider, providerId)`가 있으면 Session API의 로그인 세션을 발급하고 기본 화면으로 redirect한다.
- 신규 사용자면 User를 만들지 않고 Guest Token을 발급해 `/signup`으로 redirect한다.
- 공급자 access token·client secret·원본 OAuth 응답은 저장하지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-014` | 지원하지 않는 provider | 지원하지 않는 로그인 방식입니다. | 없음 | provider와 requestId |
| 400 | `AUTH-015` | state·code·redirect URI 검증 실패 | 소셜 로그인을 완료할 수 없습니다. | 없음 | OAuth 검증 원인과 requestId |
| 503 | `AUTH-016` | OAuth Provider 장애 | 소셜 로그인에 실패했습니다. | 재시도 안내 | Provider 오류와 requestId |

### 3-5. 소셜 가입 완료

`POST /api/v1/auth/social-signup`

권한: Guest Token

요청:

```json
{
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

#### 성공 응답: `201 Created`

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "roles": "USER",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-017` | 이름·전화번호·가입 정보 형식 오류 | 가입 정보를 확인해 주세요. | 실패 필드 | 검증 원인과 requestId |
| 401 | `AUTH-018` | Guest Token 서명·만료·용도 오류 | 소셜 인증을 다시 시작해 주세요. | 없음 | Token 검증 원인과 requestId |
| 409 | `AUTH-019` | SocialCredential 중복 연결 | 이미 가입된 소셜 계정입니다. | provider | 식별자와 requestId |
