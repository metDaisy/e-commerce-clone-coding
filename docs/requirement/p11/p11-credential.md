# P11 Credential API

이 문서는 P11이 소유하는 장기 인증수단을 정의한다. 인증수단의 업무 규칙은 [P11 Policy](p11-policy.md), 공통 응답·인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `UserCredential` | 로컬 로그인 이메일·비밀번호 해시·실패 누적 | 비밀번호 검증·수정, Session API의 로그인 |
| `SocialCredential` | OAuth `provider`·`providerId`와 User 연결 | Sign-up API의 OAuth 가입·기존 User 로그인 |

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
