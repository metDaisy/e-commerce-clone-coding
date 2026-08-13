# P11 Auth (인증)

공통 응답 봉투, 인증 오류, 페이지네이션은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P11 Auth는 로컬·소셜 인증 수단, 회원가입 인증 흐름, 로그인·로그아웃, 비밀번호, 토큰을 담당한다.

- `user_credentials`는 로컬 로그인 이메일·비밀번호 해시·로그인 실패 누적을 소유한다.
- `social_credentials`는 OAuth 공급자와 외부 사용자 식별자의 연결을 소유한다.
- `users` 프로필 생성은 User가 소유한다. Auth는 인증이 완료된 회원가입 요청을 User에 전달한다.
- 비밀번호·토큰·OAuth 비밀값은 로그와 이벤트 payload에 평문으로 남기지 않는다.
- 로그인 실패 5회 누적 시 Credential을 잠그며, 잠금 여부는 violation count와 잠금 정책으로 판단한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 공개 | 로컬 회원가입 인증 흐름 시작 |
| POST | `/api/v1/auth/social-signup` | Guest Token | 소셜 회원가입 완료 |
| POST | `/api/v1/auth/login` | 공개 | 이메일·비밀번호 로그인 |
| GET | `/api/v1/auth/oauth/{provider}/authorize` | 공개 | OAuth 인증 시작 |
| GET | `/api/v1/auth/oauth/{provider}/callback` | 공개 | OAuth callback 처리 |
| POST | `/api/v1/auth/refresh` | Refresh Token | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | 로그인 | 현재 기기 로그아웃 |
| POST | `/api/v1/auth/logout-all` | 로그인 | 전체 기기 로그아웃 |
| POST | `/api/v1/auth/password/verify` | 로그인 | 기존 비밀번호 확인 |
| POST | `/api/v1/auth/update` | 로그인 | 로컬 인증수단 수정 |

## 3. 요구사항

### 3-1. 로컬 회원가입

`POST /api/v1/auth/signup`

요청:

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "Password1!",
  "phone": "010-1234-5678"
}
```

- `name`, `email`, `password`는 필수다.
- 이메일은 `user_credentials.email`에 저장하는 로컬 로그인 식별자이며 RFC 5322 형식과 대소문자 비구분 규칙을 따른다.
- 이메일은 정규화 후 UNIQUE다.
- 비밀번호는 8자 이상이며 영문 대소문자·숫자·특수문자 중 2종 이상을 포함한다.
- 비밀번호는 Bcrypt로 저장한다.
- Auth는 인증수단을 생성하고 User에 프로필 생성 요청을 전달한다.
- 가입 성공 시 User에 `USER` 권한과 포인트 잔액 0이 생성된다.
- 응답에는 `userId`, `name`, `loginEmail`, `role`, `createdAt`만 포함한다. 비밀번호는 포함하지 않는다.

성공 응답 `201`:

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "loginEmail": "user@example.com",
  "role": "USER",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 로그인·토큰

`POST /api/v1/auth/login`

- 성공 시 HttpOnly Secure 쿠키에 Access Token(30분)과 Refresh Token(7일)을 설정한다.
- JWT에는 `sub=userId`, `role`, `deviceId`, `iat`, `exp`를 포함한다.
- 비밀번호 오류 5회 누적 시 30분간 Credential을 잠근다.
- 로그아웃 시 Access Token을 블랙리스트에 등록하고 Refresh Token 쿠키를 삭제한다.
- 전체 로그아웃은 사용자 기준으로 발급 시각 이전의 모든 토큰을 무효화한다.
- 비활성화된 User는 로그인할 수 없다.

#### 심화 사항

- 로그인 성공 시 게스트 장바구니를 회원 장바구니로 병합한다.
- 이메일 인증, 비밀번호 재설정, 기기별 세션 관리, MFA를 지원한다.

### 3-3. 소셜 인증 식별자

- `provider`는 Amazon이 정한 값이 아니라 이 프로젝트가 여러 OAuth 공급자를 통일하기 위해 정의한 애플리케이션 enum이다.
- 요구사항에서 허용하는 값은 `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB`다.
- `providerId`는 OAuth 공급자가 반환하는 외부 사용자 식별자이며 이메일 주소를 대신 사용하지 않는다.
- `provider + providerId` 조합은 UNIQUE다.
- 소셜 로그인에 의한 회원가입은 User의 `users`와 Auth의 `social_credentials`를 생성하지만 `user_credentials`를 생성하지 않는다.
- 신규 소셜 사용자는 OAuth callback에서 즉시 정식 회원으로 생성하지 않고 회원가입 화면(`/signup`)으로 이동한다.
- 신규 소셜 사용자는 회원가입 화면에서 추가 프로필 정보를 입력한 뒤 가입을 완료한다. OAuth 인증 결과와 입력값을 결합해 User 프로필과 `social_credentials`를 생성한다.
- 이미 동일한 `(provider, providerId)`가 존재하면 기존 계정으로 로그인한다.
- 신규 소셜 사용자의 가입 완료 전에는 `Guest Token`을 사용한다. 이 토큰은 회원가입 완료 용도로만 사용할 수 있다.
- 공급자 access token, client secret, 원본 OAuth 응답은 DB·로그·이벤트에 저장하지 않는다.

`POST /api/v1/auth/social-signup`

요청자는 OAuth callback이 발급한 Guest Token을 사용한다.

```json
{
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

- `name`은 필수이고 `phone`은 선택이다.
- 서버는 Guest Token의 `provider`, `providerId`와 요청값을 결합해 User 프로필과 `social_credentials`를 하나의 트랜잭션으로 생성한다.
- 이 API는 `email`, `password`를 받지 않으며 `user_credentials`를 생성하지 않는다.
- 성공 응답은 `userId`, `name`, `role`, `createdAt`을 반환한다. 소셜 계정에는 로컬 로그인 이메일이 없으므로 `loginEmail`을 반환하지 않는다.

성공 응답 `201`:

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "role": "USER",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

#### 심화 사항

- Google·Naver·Kakao·GitHub OAuth2 로그인과 게스트 토큰을 지원한다.
- Login with Amazon을 별도 OAuth 공급자 `AMAZON`으로 연동할 수 있다.
- 다중 소셜 인증 수단 연결·해제를 지원한다.

OAuth callback의 성공 응답은 JSON이 아니라 redirect다. 기존 계정이면 Access·Refresh Token 쿠키를 설정한 뒤 프론트엔드 시작 경로로 `302` redirect한다. 신규 계정이면 Guest Token 쿠키를 설정한 뒤 `/signup`으로 `302` redirect한다.

### 3-4. 로컬 인증수단 관리

- 이메일 변경은 별도 API로 분리하고 기존 비밀번호를 재확인한다.
- 마지막 인증수단은 제거할 수 없다.
- 소셜 계정만 가진 사용자가 로컬 인증수단을 추가하면 `user_credentials`를 생성한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_PASSWORD_FORMAT` | 비밀번호 정책 위반 |
| 400 | `UNSUPPORTED_OAUTH_PROVIDER` | 허용하지 않은 provider 값 |
| 400 | `INVALID_OAUTH_CALLBACK` | state·code 검증 실패 |
| 401 | `BAD_CREDENTIALS` | 이메일·비밀번호 불일치 |
| 401 | `INVALID_TOKEN` | 토큰 서명·만료 오류 |
| 401 | `BLACKLISTED_TOKEN` | 로그아웃된 토큰 사용 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이메일 중복 |
| 409 | `SOCIAL_CREDENTIAL_ALREADY_LINKED` | 소셜 계정 중복 연결 |
| 409 | `LAST_AUTH_METHOD` | 마지막 인증 수단 해제 |
| 423 | `ACCOUNT_LOCKED` | 로그인 실패 누적 잠금 |
