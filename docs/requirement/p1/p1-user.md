# P1 User API

이 문서는 `User` 데이터 모델과 프로필·계정 상태 API를 정의한다. 업무 정책은 [P1 User Policy](p1-policy.md), 주소 모델과 주소 API는 [Address API](p1-address.md), 재인증 쿠키는 [P11 Credential API](../p11/p11-credential.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `User` | 프로필, 역할, 활성 상태와 가입 시각 | 프로필 조회·수정, 계정 비활성화 |

- P1은 `User`의 원본을 소유한다.
- `Address`는 User가 소유하는 별도 리소스이며 [Address API](p1-address.md)에서 정의한다. User API 응답에 Address를 중첩하지 않는다.
- `UserCredential`, `SocialCredential`, Token, Seller, Order와 같은 외부 모델의 필드를 복제하지 않는다.

## 2. 데이터 모델

### 2-1. `User`

| 필드 | 도메인 타입 (API 응답 타입) | 필수 | 설명 |
|---|---|---:|---|
| `id` | UUID | 예 | 사용자 식별자 |
| `name` | String | 예 | 사용자 표시 이름. 공백만 입력할 수 없으며 활성 User 간 중복 불가 |
| `phoneNumber` | String | 아니오 | 선택 연락처. 입력 시 유효한 전화번호 형식이며 활성 User 간 중복 불가 |
| `roles` | `Set<UserRole>` (`List<UserRole>`) | 예 | 도메인에서는 중복 없는 역할 집합이며, API 응답에서는 JSON 배열로 반환한다. 예: `["USER","PRODUCT_MANAGER"]`. 항상 `USER` 포함 |
| `isEnabled` | Boolean | 예 | 로그인·기능 사용 가능 여부. 비활성화 시 `false` |
| `createdAt` | Instant | 예 | User 생성 시각. ISO-8601 UTC |
| `updatedAt` | Instant | 예 | User 마지막 변경 시각. ISO-8601 UTC |

`loginEmail`은 User 필드가 아니라 P11 Auth가 소유하는 로컬 인증수단 정보다. P1 User API는 이를 반환하지 않으며, 비밀번호·OAuth 식별자·토큰은 어떤 프로필 API에도 반환하지 않는다.

`pointBalance`는 가입 시 `0`으로 초기화되는 내부 사용자 잔액이다. P1은 이 API에서 잔액을 수정하거나 결제·적립 정책을 재정의하지 않는다.

#### `roles` 표현 경계

`roles`는 동일한 역할 정보를 경계에 따라 다르게 표현한다.

| 경계 | 표현 | 규칙 |
|---|---|---|
| User 도메인 | `Set<UserRole>` | 한 User가 여러 역할을 가질 수 있다. 중복은 불가능하며 `USER`를 항상 포함한다. |
| 저장 모델 | User별 역할 행의 집합 | 역할마다 하나의 행으로 저장하며 User와 역할의 조합은 중복될 수 없다. |
| User API·Auth DTO 응답 | `List<UserRole>` | `["USER","PRODUCT_MANAGER"]`처럼 JSON 배열 또는 컬렉션으로 반환한다. 배열의 순서는 권한 의미가 아니다. |
| JWT·이벤트 내부 계약 | `String` | JWT claim과 이벤트 payload에서만 `USER,PRODUCT_MANAGER`처럼 쉼표로 구분한다. 문자열의 순서는 권한 의미가 아니다. |

따라서 `User.java`에서 `Set<UserRole>`을 사용하는 것은 도메인 모델의 표현이고, API·Auth DTO의 `roles`는 `List<UserRole>`로 반환한다. JWT claim과 이벤트 payload만 내부 호환 계약상 CSV 문자열로 유지한다. API 요청에서 `roles`를 보내거나 프로필 수정으로 변경할 수는 없다.

### 2-2. 관계와 제약

- `User.id`는 전역적으로 유일하다.
- `User.roles`는 도메인에서 중복 없는 역할 집합이며 항상 `USER`를 포함한다. API·Auth DTO 응답에서는 JSON 배열로, JWT claim과 이벤트 payload에서는 쉼표로 구분한 문자열로 표현한다.
- 비활성화는 물리 삭제가 아니며 `isEnabled=false`로 표현한다.
- 이름과 연락처는 활성 User 간 유일해야 한다. 비활성 User의 이름과 연락처는 재사용할 수 있다.
- User가 소유한 Address의 모델과 제약은 [Address API](p1-address.md)에서 정의한다.

## 3. API 정의

모든 API는 로그인한 User를 `principal`로 식별한다. `ADMIN`의 전체 사용자 운영 API와 역할 변경 API는 P7 문서에서 정의한다.

### 3-0. 보호 API의 재인증

다음 API는 유효한 Access Token만으로 접근할 수 없으며, 해당 작업을 위한 P11 재인증이 성공해야 한다.

| API | 재인증 목적 | P1 요청에 필요한 값 |
|---|---|---|
| `GET /api/v1/me` | `USER_ACCOUNT_MANAGEMENT` | 유효한 `__Host-REAUTH` 쿠키 |
| `PATCH /api/v1/me` | `USER_ACCOUNT_MANAGEMENT` | 유효한 `__Host-REAUTH` 쿠키 |
| `POST /api/v1/me/deactivate` | `USER_ACCOUNT_MANAGEMENT` | 유효한 `__Host-REAUTH` 쿠키 |

- 클라이언트는 재인증 성공 후 P11이 발급한 `__Host-REAUTH` 쿠키를 자동으로 전송한다. P1 API마다 비밀번호를 다시 보내지 않는다.
- 쿠키는 재인증 성공 시점부터 30분 동안 세 P1 보호 API에 재사용할 수 있다. 만료·변조·무효화된 쿠키는 거절하고 다시 재인증을 요구한다.
- 쿠키는 `Secure; HttpOnly; SameSite=Strict; Path=/` 속성을 사용한다. P1 보호 API는 Access Token을 `Authorization: Bearer` 헤더로 인증하고 `__Host-REAUTH` 쿠키는 보조적인 재인증 Grant로만 사용하므로, P1은 CSRF 토큰을 요구하지 않는다. 향후 쿠키만으로 인증하는 구조나 BFF를 도입하면 상태 변경 요청에 CSRF 토큰 또는 동등한 Origin/Fetch Metadata 검증을 적용한다.
- P1은 비밀번호 원문이나 OAuth 공급자 응답을 받거나 저장하지 않는다. 재인증의 검증과 Grant 발급은 P11이 소유한다.
- 로컬 인증수단이 있는 User는 기존 비밀번호를 입력한다. OAuth 전용 User는 연결된 OAuth 공급자의 새 인증을 완료한다. 세부 절차는 [P11 Credential API](../p11/p11-credential.md#3-3-민감-작업-재인증)를 따른다.

### 3-0-1. CSR 프로필 화면 조합

SPA CSR 클라이언트는 프로필 화면을 표시할 때 다음 공개 API를 병렬 호출해 UI 전용 `MyProfileView`를 조합한다.

- P1 `GET /api/v1/me`: User가 소유하는 프로필·역할·활성 상태
- P11 `GET /api/v1/auth/me/credential-summary`: Auth가 소유하는 로컬 로그인 이메일 요약

두 요청 모두 로그인 사용자와 `USER_ACCOUNT_MANAGEMENT` 목적의 유효한 `__Host-REAUTH` 쿠키가 필요하다. 어느 한 요청이라도 재인증 실패 또는 만료로 실패하면 부분 프로필을 표시하지 않고 재인증 흐름으로 보낸다.

P1 User는 응답을 보강하려고 Auth를 동기 조회하지 않는다. 여러 클라이언트가 같은 조합을 반복하거나 조합·권한 정책이 복잡해질 때에만 별도 BFF/Account composition API 도입을 검토한다. 결정 배경은 [ADR-0014](../../adr/0014-csr-profile-composition-and-auth-user-query-direction.md)를 따른다.

<a id="user-disabled-error"></a>
### 3-0-2. 비활성 User 공통 예외

User가 비활성화 상태인 경우 User 정보를 조회하거나 변경할 수 없으며 다음 공통 예외를 반환한다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 403 | `USER-004` | User가 비활성 상태임 | 계정을 사용할 수 없습니다. | 없음 | User 식별자와 requestId |

### 3-1. 내 프로필 조회

`GET /api/v1/me`

권한: 로그인 사용자 + 유효한 `__Host-REAUTH` 쿠키

#### 성공 응답: `200 OK`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "name": "홍길동",
  "phoneNumber": "01012345678",
  "roles": ["USER"],
  "isEnabled": true,
  "createdAt": "2026-08-16T12:00:00Z",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

소셜 전용 User도 이 응답 형식은 같으며, 비밀번호·토큰·OAuth 원본 응답은 포함하지 않는다. 로컬 로그인 이메일은 P11의 인증수단 요약 API에서만 `null`로 반환할 수 있다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 401 | [AUTH-026](../p11/p11-credential.md#3-3-민감-작업-재인증) | 재인증 쿠키가 없거나 만료·변조·무효화됨 | 추가 인증이 필요합니다. | `purpose=USER_ACCOUNT_MANAGEMENT` | User와 쿠키 검증 원인 |
| 403 | [`USER-004`](#user-disabled-error) | — | — | — | — |
| 404 | `USER-001` | 인증 주체의 User가 존재하지 않음 | 사용자를 찾을 수 없습니다. | 없음 | User 조회 원인과 requestId |

### 3-2. 내 프로필 수정

`PATCH /api/v1/me`

권한: 로그인 사용자 + 유효한 `__Host-REAUTH` 쿠키

요청:

```json
{
  "name": "김길동",
  "phoneNumber": "01098765432"
}
```

`name`과 `phoneNumber` 중 하나 이상을 보내야 하며, `id`, `roles`, `isEnabled`, `pointBalance`는 수정할 수 없다. 로그인 이메일 등 인증수단 변경은 P11 Auth API에서만 처리한다.

#### 성공 응답: `200 OK`

응답은 [내 프로필 조회](#3-1-내-프로필-조회)와 같은 `User` Response DTO다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `USER-007` | 이름·연락처가 없거나 형식이 잘못됨 | 입력값을 확인해 주세요. | 실패한 필드와 수정 방법 | 검증 필드와 내부 검증 원인 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 401 | [AUTH-026](../p11/p11-credential.md#3-3-민감-작업-재인증) | 재인증 쿠키가 없거나 만료·변조·무효화됨 | 추가 인증이 필요합니다. | `purpose=USER_ACCOUNT_MANAGEMENT` | User와 쿠키 검증 원인 |
| 403 | [`USER-004`](#user-disabled-error) | — | — | — | — |
| 404 | `USER-001` | User가 존재하지 않음 | 사용자를 찾을 수 없습니다. | 없음 | User 조회 원인과 requestId |
| 409 | `USER-002` | 이름이 다른 활성 User에 이미 연결됨 | 이미 사용 중인 이름입니다. | `field=name` | 중복 User 식별자와 충돌 원인 및 요청 userId |
| 409 | `USER-003` | 연락처가 다른 활성 User에 이미 연결됨 | 이미 사용 중인 연락처입니다. | `field=phoneNumber` | 중복 User 식별자와 충돌 원인 및 요청 userId |

중복 예외의 `name`·`phoneNumber` 원문은 기록하지 않는다. 서버 내부 기록에는 요청 User의 `userId`와 공통 `requestId`를 남긴다.

### 3-3. 계정 비활성화

`POST /api/v1/me/deactivate`

권한: 로그인 사용자 + 유효한 `__Host-REAUTH` 쿠키

요청 본문: 없음

계정은 물리 삭제하지 않고 `isEnabled=false`로 변경한다. 이미 비활성화된 계정에 같은 요청을 보내면 `USER-010` 예외를 반환한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 401 | [AUTH-026](../p11/p11-credential.md#3-3-민감-작업-재인증) | 재인증 쿠키가 없거나 만료·변조·무효화됨 | 추가 인증이 필요합니다. | `purpose=USER_ACCOUNT_MANAGEMENT` | User와 쿠키 검증 원인 |
| 404 | `USER-001` | User가 존재하지 않음 | 사용자를 찾을 수 없습니다. | 없음 | User 조회 원인과 requestId |
| 409 | `USER-010` | 이미 비활성화된 User에 다시 비활성화를 요청함 | 이미 비활성화된 계정입니다. | 없음 | User 상태 전이 충돌 원인 |

비활성화 후 로그인·토큰 무효화는 [P11 Session Policy](../p11/p11-policy.md)의 정책을 따른다. 비활성화된 개인정보 마스킹 시점은 P1 보존 정책의 후속 결정으로 정의한다.
