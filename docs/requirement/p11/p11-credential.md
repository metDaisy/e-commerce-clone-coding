# P11 Credential API

이 문서는 P11이 소유하는 장기 인증수단을 정의한다. 인증수단의 업무 규칙은 [P11 Policy](p11-policy.md), 공통 응답·인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `UserCredential` | 로컬 로그인 이메일·비밀번호 해시·실패 누적 | 비밀번호 검증·수정, Session API의 로그인 |
| `SocialCredential` | OAuth `provider`·`providerId`와 User 연결 | Sign-up API의 OAuth 가입·기존 User 로그인 |
| `ReauthenticationGrant` | 최근 재인증 상태를 30분 동안 증명하는 서버 기록과 쿠키 | P1 User 프로필·계정 상태 API |

P1 `User`는 인증수단의 소유 User 식별자만 공개한다. P11은 User 프로필·역할·활성 상태 원본을 복제하지 않는다.

## 2. 데이터 모델

### 2-1. `UserCredential`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | UUID | 예 | User당 하나 연결되는 PK/FK |
| `email` | string | 예 | 정규화된 로컬 로그인 이메일. 전역 UNIQUE |
| `passwordHash` | string | 예 | 평문이 아닌 Bcrypt 해시 |
| `violationCount` | integer | 예 | 로그인 실패 누적. 0 이상 |
| `untilLocked` | Instant | 아니오 | 잠금 만료 시각. null이면 잠금 없음 |

### 2-2. `SocialCredential`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | UUID | 예 | 인증수단 식별자 |
| `userId` | UUID | 예 | User 식별자 |
| `provider` | enum | 예 | `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB` |
| `providerId` | string | 예 | 이메일이 아닌 공급자 외부 식별자 |

`(provider, providerId)` 조합은 UNIQUE다. 소셜 전용 User에는 `UserCredential`을 만들지 않는다.

### 2-3. `ReauthenticationGrant`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | UUID | 예 | 재인증을 완료한 User |
| `purpose` | Enum | 예 | `USER_ACCOUNT_MANAGEMENT` |
| `expiresAt` | Instant | 예 | 발급 후 30분 뒤의 만료 시각 |
| `lastUsedAt` | Instant | 아니오 | 마지막 보호 API 사용 시각 |
| `revokedAt` | Instant | 아니오 | 로그아웃·인증수단 변경 등으로 무효화한 시각 |
| `grantHash` | string | 예 | 쿠키 원문의 해시. 쿠키 원문은 저장·로그 기록하지 않는다. |

Grant는 User·목적·만료 시각에 묶이며 만료 전에는 세 P1 보호 API에 재사용할 수 있다.

## 3. API 정의

### 3-1. 기존 비밀번호 검증

`POST /api/v1/auth/password/verify`

권한: 로그인 사용자

요청:

```json
{
  "password": "Password1!"
}
```

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-001` | Access Token이 없음·유효하지 않음 | 로그인이 필요합니다. | 없음 | 인증 실패와 requestId |
| 401 | `AUTH-022` | 기존 비밀번호 불일치 | 비밀번호를 확인해 주세요. | 없음 | User 식별자와 검증 원인 |
| 403 | `AUTH-023` | 로컬 UserCredential이 없음 | 로컬 비밀번호를 사용할 수 없습니다. | 없음 | 인증수단 상태와 requestId |

### 3-2. 로컬 인증수단 수정

`POST /api/v1/auth/update`

권한: 로그인 사용자

이메일·비밀번호 변경은 기존 비밀번호를 재확인한다. 이메일은 정규화·중복 검사를 다시 수행하며, 비밀번호 변경 성공 시 기존 Login Session을 무효화한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-003` | 새 이메일·비밀번호 형식 오류 | 변경 정보를 확인해 주세요. | 실패 필드와 수정 방법 | 검증 필드와 requestId |
| 401 | `AUTH-022` | 기존 비밀번호 불일치 | 비밀번호를 확인해 주세요. | 없음 | User 식별자와 검증 원인 |
| 409 | `AUTH-004` | 새 이메일이 이미 존재 | 사용할 수 없는 이메일입니다. | 없음 | 이메일 중복 원인과 requestId |
| 403 | `AUTH-023` | 로컬 UserCredential이 없음 | 로컬 인증수단을 사용할 수 없습니다. | 없음 | 인증수단 상태와 requestId |

### 3-3. 민감 작업 재인증

P1의 프로필 조회·수정·계정 비활성화처럼 추가 보호가 필요한 작업은 Access Token만으로 수행하지 않는다. P11은 로컬 비밀번호 또는 연결된 OAuth 인증수단을 다시 확인한 뒤 30분 동안 재사용 가능한 Grant 쿠키를 발급한다.

#### 로컬 UserCredential 재인증

`POST /api/v1/auth/reauthenticate/password`

권한: 로그인 사용자

요청:

```json
{
  "purpose": "USER_ACCOUNT_MANAGEMENT",
  "password": "Password1!"
}
```

`purpose`는 `USER_ACCOUNT_MANAGEMENT`다. 이 목적은 P1의 프로필 조회·수정·계정 비활성화에 공통으로 사용한다.

#### OAuth 전용 User 재인증

`GET /api/v1/auth/reauthenticate/oauth/{provider}/authorize?purpose=USER_ACCOUNT_MANAGEMENT`

`GET /api/v1/auth/reauthenticate/oauth/{provider}/callback`

권한: 로그인 사용자

- `{provider}`는 해당 User에 연결된 `SocialCredential`의 공급자여야 한다.
- authorize는 공급자에게 새 인증을 요청하고, callback은 `state`·공급자 응답·인증된 User를 검증한다.
- 이미 발급된 애플리케이션 Access Token이나 OAuth 공급자의 기존 세션이 새 인증 증명 없이 전달된 것만으로는 성공 처리하지 않는다.
- 여러 SocialCredential이 있으면 연결된 공급자 중 하나를 선택할 수 있다.

#### 성공 응답: `204 No Content`

로컬·OAuth 재인증 모두 다음 쿠키를 설정한다.

```http
Set-Cookie: __Host-REAUTH=<opaque-value>; Max-Age=1800; Path=/; Secure; HttpOnly; SameSite=Strict
```

- 쿠키 값은 opaque 값이며 목적은 `USER_ACCOUNT_MANAGEMENT`로 고정한다.
- 쿠키는 발급 후 30분 동안 유효하고 P1의 세 보호 API에 재사용할 수 있다.
- 서버는 쿠키 원문이 아니라 `grantHash`만 저장한다. 로그아웃·비밀번호 변경·OAuth 인증수단 변경·계정 비활성화 성공 시 쿠키를 폐기한다.
- 쿠키 기반 상태 변경 요청에는 CSRF 토큰 또는 동등한 Origin/Fetch Metadata 검증을 적용한다.
- 비밀번호·OAuth access token·공급자 원본 응답·쿠키 원문은 로그와 이벤트 payload에 남기지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `AUTH-003` | `purpose`가 허용되지 않음 | 인증 목적을 확인해 주세요. | `purpose` | 검증 원인과 requestId |
| 401 | `AUTH-001` | Access Token이 없음·유효하지 않음 | 로그인이 필요합니다. | 없음 | 인증 실패와 requestId |
| 401 | `AUTH-022` | 로컬 비밀번호 불일치 | 비밀번호를 확인해 주세요. | 없음 | User 식별자와 검증 원인 |
| 401 | `AUTH-026` | OAuth 재인증 실패·취소·callback 검증 실패 또는 Grant 검증 실패 | 추가 인증을 완료해 주세요. | provider 또는 purpose | 공급자 응답과 검증 원인 |
| 403 | `AUTH-023` | PASSWORD 방식인데 로컬 UserCredential이 없음 | 로컬 비밀번호를 사용할 수 없습니다. | 없음 | 인증수단 상태와 requestId |
| 409 | `AUTH-027` | OAuth provider가 User에게 연결되지 않음 | 연결된 인증수단을 선택해 주세요. | provider | User와 provider |
