# P11 Session API

이 문서는 로그인 후 인증 세션과 Token의 생명주기를 정의한다. 세션 규칙은 [P11 Policy](p11-policy.md), Credential 모델은 [Credential API](p11-credential.md), 공통 응답·인증 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `RefreshToken` | 기기별 현재·이전 Refresh Token과 만료 상태 | 로그인·갱신·로그아웃 |
| `Access Token` | 단기 API 인증 JWT | 로그인·갱신·공통 인증 |
| `Login Session` | 한 기기의 Access·Refresh Token 생명주기 | 현재 기기·전체 로그아웃, 역할 변경 무효화 |

P11은 Token과 Session을 소유하지만 User의 역할 원본은 P1 User가 소유한다. 역할 집합 변경 사실은 `UserRolesChangedEvent`로 전달한다.

## 2. 데이터 모델

### 2-1. `RefreshToken`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | UUID | 예 | User 식별자 |
| `deviceId` | string | 예 | 기기별 세션 식별자 |
| `token` | string | 예 | 현재 Refresh Token의 `jti` |
| `preToken` | string | 아니오 | 회전 직전 Token의 `jti` |
| `expiredAt` | Instant | 예 | Refresh Token 만료 시각 |

Access Token은 목표 만료시간 30분, Refresh Token은 7일이다. 두 Token은 HttpOnly Secure 쿠키로 전달하며 응답 body에 원문을 넣지 않는다.

## 3. API 정의

### 3-1. 로컬 로그인

`POST /api/v1/auth/login`

권한: 공개

요청:

```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

성공 시 현재 User 역할 집합으로 Access·Refresh Token을 발급하고, 로그인 성공 후 P3의 Guest Cart 병합 계약을 호출할 수 있다.

#### 성공 응답: `200 OK`

```json
{
  "userId": "uuid",
  "roles": "USER"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-011` | 이메일·비밀번호 불일치 | 이메일 또는 비밀번호를 확인해 주세요. | 없음 | 인증 실패와 requestId |
| 403 | `AUTH-002` | User가 비활성 상태 | 계정을 사용할 수 없습니다. | 없음 | User 식별자와 requestId |
| 423 | `AUTH-013` | Credential 잠금 | 잠시 후 다시 시도해 주세요. | `untilLocked` | User 식별자와 잠금 원인 |

### 3-2. Access Token 갱신

`POST /api/v1/auth/refresh`

권한: Refresh Token 쿠키

현재 Token의 서명·만료·저장 값·User 활성 상태를 확인하고 Token을 회전한다. 이전 Token 재사용은 탈취로 간주한다.

#### 성공 응답: `200 OK`

```json
{
  "userId": "uuid",
  "roles": "USER"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-020` | Refresh Token이 없음·유효하지 않음 | 다시 로그인해 주세요. | 없음 | 검증 원인과 requestId |
| 401 | `AUTH-021` | 회전된 이전 Token 재사용 | 다시 로그인해 주세요. | 없음 | User·기기·jti와 보안 이벤트 |
| 403 | `AUTH-002` | User가 비활성 상태 | 계정을 사용할 수 없습니다. | 없음 | User 식별자와 requestId |

### 3-3. 현재 기기 로그아웃

`POST /api/v1/auth/logout`

권한: 로그인 사용자

현재 기기의 Login Session을 무효화하고 인증 쿠키를 삭제한다. 이미 무효화된 요청도 같은 최종 결과로 처리한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-001` | Access Token이 없음·유효하지 않음 | 로그인이 필요합니다. | 없음 | 인증 실패와 requestId |

### 3-4. 전체 기기 로그아웃

`POST /api/v1/auth/logout-all`

권한: 로그인 사용자

사용자 기준 세션 무효화 시각을 갱신해 모든 기기의 Access·Refresh Token을 무효화한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-001` | Access Token이 없음·유효하지 않음 | 로그인이 필요합니다. | 없음 | 인증 실패와 requestId |

### 3-5. 역할 변경에 따른 세션 무효화

P1 User가 `UserRolesChangedEvent`를 발행하면 P11은 대상 User의 모든 Login Session을 무효화한다. P7은 P11의 저장소를 직접 호출하지 않는다. P6 Outbox 재전달은 `eventId` 기준으로 멱등 처리한다.
