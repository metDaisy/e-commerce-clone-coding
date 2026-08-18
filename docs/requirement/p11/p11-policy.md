# P11 Auth Policy

P11은 사용자의 신원을 증명하는 인증수단과 로그인 세션을 관리한다. `User` 프로필·역할·활성 상태의 원본은 [P1 User](../p1/p1-policy.md)가 소유한다.

## 1. 범위와 책임

### 범위

- 로컬 이메일·비밀번호 인증수단과 로그인 실패·잠금 정책
- 이메일 OTP 기반 로컬 회원가입 인증 세션
- OAuth 공급자 식별자와 User 연결, 신규 소셜 회원가입 완료 전의 Guest Token
- Access·Refresh Token 발급·회전·폐기와 기기별·전체 세션 무효화
- Account의 `Login & security` 기능에 필요한 비밀번호 검증·변경·이메일 변경
- P1 보호 API에 사용하는 로컬 비밀번호·OAuth 재인증과 30분 재사용 `ReauthenticationGrant` 쿠키
- 역할 변경·계정 비활성화에 따른 기존 인증 세션 무효화

### 범위 밖

- User 이름·전화번호·역할·활성 상태 원본과 관리자 역할 변경 진입점
- OAuth 공급자 자체의 계정·동의·access token 관리
- 주문·결제·배송·Cart 원본
- 이메일 Provider의 전송 구현과 이벤트 Outbox 운영

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| User 프로필·역할·활성 상태 | P1 User | [P1 Policy](../p1/p1-policy.md) |
| 로컬·소셜 인증수단 | P11 Auth | [Credential API](p11-credential.md) |
| 가입 세션·OTP·Guest Token | P11 Auth | [Sign-up API](p11-signup.md) |
| Access·Refresh Token·로그인 세션 | P11 Auth | [Session API](p11-session.md) |
| 보호 API 재인증·ReauthenticationGrant | P11 Auth | [Credential API](p11-credential.md) |
| 역할 변경 요청·관리자 진입점 | P7 Admin·P1 User | [P7 Access](../p7/p7-access.md) |
| 역할 변경 이벤트 저장·재전달 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| 이메일 발송·OAuth 연동 Adapter | P11 infra | 외부 Provider 계약 |
| 로그인 후 Cart 병합 규칙·원본 | P3 Cart | [P3 Policy](../p3/p3-policy.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `UserCredential` | 정규화된 이메일과 비밀번호 해시로 구성된 로컬 인증수단. User에 선택적으로 하나 연결된다. |
| `SocialCredential` | OAuth `provider`와 외부 `providerId`로 User를 식별하는 인증수단. 한 User가 여러 개 가질 수 있다. |
| `SignUpSession` | 이메일 인증 전의 만료 가능한 가입 흐름. 정식 User를 만들기 전 P11이 소유한다. |
| `Guest Token` | 신규 소셜 사용자가 추가 프로필을 제출할 때만 사용하는 단기 토큰. |
| `Access Token` | API 요청 인증에 쓰는 단기 JWT. |
| `Refresh Token` | Access Token 재발급에 쓰는 장기 JWT와 기기별 저장 세션. |
| `인증된 사용자` | 유효한 Access Token으로 식별되고 P1 User가 활성 상태인 User. |
| `로그인 세션` | 한 기기에서 발급된 Access Token·Refresh Token의 묶음. |
| `Guest` | 정식 User가 아니며 Guest Token으로 소셜 가입 완료만 수행할 수 있는 주체. |
| `ReauthenticationGrant` | 최근 인증수단 확인을 완료했다는 30분·목적 제한 증명. `__Host-REAUTH` 쿠키로 전달하며 Access Token을 대체하지 않는다. |
| `재인증` | 이미 로그인한 User가 민감 작업 전에 로컬 비밀번호 또는 연결된 OAuth 인증수단으로 신원을 다시 증명하는 과정. |
| `P11 Auth` | 인증수단·토큰·세션의 원본과 보안 규칙을 소유하는 도메인. |

용어 기준은 [도메인 용어집](../../domain-glossary.md)이다. 용어 의미를 변경할 때는 용어집과 P1·P3·P7 문서를 함께 검토한다.

## 3. 핵심 업무 규칙

### 인증수단과 User 연결

1. 이메일 인증 또는 OAuth 인증이 완료되기 전에는 정식 User를 생성하지 않는다.
2. 로컬 가입 완료 시 P11은 `UserCredential`을 만들고 P1에 이름·선택적 전화번호·인증 완료 사실을 전달한다. P1은 User와 초기 상태를 생성한다.
3. 소셜 가입 완료 시 P11은 `SocialCredential`을 만들고 `UserCredential`은 만들지 않는다. 소셜 User의 `loginEmail`은 null이거나 응답에서 생략한다.
4. 이메일은 정규화 후 전역 UNIQUE다. OAuth 식별자는 이메일 대신 `(provider, providerId)` 조합으로 유일성을 판단한다.
5. 마지막으로 남은 인증수단은 제거할 수 없다. 단, 소셜 전용 User가 로컬 인증수단을 처음 추가하는 것은 허용한다.

### 로컬 가입과 OTP

1. 활성 프로필이 `dev`이면 이메일 발송과 OTP 검증을 생략하고, 입력 검증 후 즉시 가입을 완료한다. 이때 `SignUpSession`과 OTP를 생성하지 않는다.
2. 활성 프로필이 `prod`이면 24시간 유효한 `SignUpSession`과 6자리 OTP를 생성하고, OTP는 10분만 유효하다.
3. `prod`에서 OTP는 한 번 성공하면 즉시 폐기하고 재발급 시 이전 OTP를 폐기한다.
4. `prod`에서 OTP 입력 실패는 코드당 5회까지 허용한다. 초과하면 해당 코드를 폐기하고 재전송을 요구한다.
5. `prod`에서 재전송에는 60초 쿨다운과 가입 세션·이메일별 시간당 발송 제한을 적용한다.
6. `prod`에서 이메일 Provider 장애가 발생해도 인증을 우회하지 않는다. 세션은 만료 전까지 `EMAIL_VERIFICATION_PENDING`으로 남긴다.
7. `dev` 이외의 프로필은 인증 우회 방지를 위해 `prod`와 같은 이메일 인증 흐름을 사용한다.
8. OTP·비밀번호·가입 세션 비밀값은 DB·로그·이벤트·응답에 원문으로 남기지 않는다.

### 로그인과 세션

1. 로그인 성공 시 현재 P1 User의 전체 역할 집합으로 Access·Refresh Token을 발급한다.
2. 비밀번호 실패가 5회 누적되면 Credential을 30분 잠근다. 잠금 여부는 `violationCount`와 `untilLocked`로 계산한다.
3. Refresh Token은 기기별로 저장하고 갱신 때 회전한다. 이전 값이 다시 사용되면 Token 탈취로 간주하고 해당 사용자 세션을 무효화한다.
4. 현재 기기 로그아웃은 해당 기기의 Refresh Token과 현재 Access Token을 무효화한다. 전체 로그아웃은 사용자 기준 무효화 시각을 갱신해 모든 기기를 무효화한다.
5. 비활성 User는 로그인·Token 갱신·인증이 필요한 기능을 사용할 수 없다.
6. 로그인 성공 후 게스트 Cart가 있으면 P11은 P3의 공개 병합 계약을 호출한다. Cart 원본과 병합 규칙은 P3가 소유한다.

### 민감 작업 재인증

1. P1의 `GET /api/v1/me`, `PATCH /api/v1/me`, `POST /api/v1/me/deactivate`는 유효한 Access Token 외에 공통 계정 관리 재인증을 요구한다.
2. 로컬 UserCredential이 있으면 기존 비밀번호를 검증하고, OAuth 전용 User는 연결된 SocialCredential의 OAuth 공급자 재인증을 완료한다.
3. Access Token이 유효하다는 사실, 애플리케이션 세션이 유지된다는 사실, OAuth providerId가 존재한다는 사실만으로는 재인증을 충족하지 않는다.
4. 재인증 성공 시 P11은 `USER_ACCOUNT_MANAGEMENT` 목적의 `__Host-REAUTH` 쿠키를 발급한다. 쿠키는 30분 동안 세 P1 보호 API에 재사용할 수 있다.
5. OAuth 공급자 재인증이 실패하거나 연결된 인증수단이 없으면 쿠키를 발급하지 않는다. 소셜 User가 비밀번호 방식으로 보호 API를 사용하려면 먼저 P11에서 로컬 인증수단을 추가해야 한다.
6. P11은 비밀번호·OAuth access token·공급자 응답·쿠키 원문을 P1에 전달하거나 로그·이벤트에 기록하지 않는다.

### 역할 변경과 인증 무효화

1. 역할 집합의 원본과 변경 사실은 P1 User가 소유한다. P7은 관리자 역할 변경 진입점을 제공한다.
2. `UserRolesChangedEvent`가 발생하면 P11은 대상 User의 모든 Access·Refresh Token과 기기 세션을 무효화한다.
3. 역할 변경과 Outbox 기록은 같은 트랜잭션에 속하고, P11의 동기 세션 무효화가 실패하면 역할 변경을 성공으로 커밋하지 않는다.
4. 커밋 이후 Outbox가 이벤트를 재전달해도 P11은 `eventId` 기준으로 멱등 처리한다.
5. 무효화된 기존 Token으로 API를 호출하거나 갱신할 수 없다. 사용자는 다시 로그인해야 변경된 역할을 반영받는다.

## 4. 불변식과 상태 전이

### 불변식

- `UserCredential.userId`는 User당 최대 하나이고 `email`은 정규화 후 중복될 수 없다.
- `(SocialCredential.provider, SocialCredential.providerId)`는 중복될 수 없다.
- `USER` 프로필 생성과 인증 완료 전에는 정식 User를 만들지 않는다.
- UserCredential의 `passwordHash`, OTP, Token, OAuth 비밀값은 원문으로 외부에 노출하지 않는다.
- 만료된 가입 세션·OTP·Token은 성공 처리에 사용할 수 없다.
- Access Token의 `roles`는 발급 시점의 역할 집합을 쉼표로 구분한 문자열이며, 역할 변경 후 기존 Token을 재사용하지 않는다.
- Guest Token은 소셜 회원가입 완료 API 외의 API 권한을 갖지 않는다.
- `ReauthenticationGrant`는 목적·User·만료 시각에 묶이고, 만료 전에는 P1의 세 보호 API에 재사용할 수 있다.
- 로그아웃·전체 로그아웃·역할 변경에 따른 무효화는 반복 처리해도 같은 최종 상태가 된다.

### 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| `dev` 가입 요청 | 입력 검증 성공 | `COMPLETED` 및 User 생성 요청 | P11 Auth + P1 User |
| `prod` 가입 요청 | 가입 세션·OTP 생성 | `EMAIL_VERIFICATION_PENDING` | P11 Auth |
| `EMAIL_VERIFICATION_PENDING` | 유효한 OTP 검증 | `COMPLETED` 및 User 생성 요청 | P11 Auth + P1 User |
| `EMAIL_VERIFICATION_PENDING` | 24시간 경과 | `EXPIRED` | P11 Auth |
| `EMAIL_VERIFICATION_PENDING` | OTP 5회 실패 | 현재 OTP 폐기·재전송 대기 | P11 Auth |
| `UserCredential` 정상 | 비밀번호 실패 5회 | 30분 잠금 | P11 Auth |
| `UserCredential` 잠금 | 잠금 만료 | 정상 로그인 가능 | P11 Auth |
| `RefreshToken` 현재 값 | 정상 갱신 | 새 현재 값, 이전 값 기록 | P11 Auth |
| `RefreshToken` 현재 값 | 로그아웃·전체 로그아웃·역할 변경 | 무효 | P11 Auth |
| `Guest Token` 유효 | 소셜 가입 완료 | User·SocialCredential 생성, Guest Token 폐기 | P11 Auth + P1 User |
| `Guest Token` 유효 | 만료 또는 목적 외 사용 | 거부 | P11 Auth |

## 5. 도메인 간 규칙과 예외 소유권

- P11은 P1의 내부 Entity·Repository를 참조하지 않고 공개 User 계약으로 인증된 `userId`, 역할, 활성 상태를 확인한다.
- P1은 User 생성·역할 변경·비활성화의 원본 예외를 소유한다. P11은 인증 API에서 필요한 경우에만 공개 인증 오류로 변환한다.
- P7은 P1의 역할 변경 공개 계약을 호출하고, P11의 세션 저장소를 직접 호출하지 않는다.
- P6는 이벤트 저장·전달·재시도를 소유한다. P11은 `UserRolesChangedEvent` 소비와 세션 무효화 결과를 소유한다.
- P3는 Cart 원본과 병합 규칙을 소유한다. P11은 로그인 성공 사실과 인증된 `userId`·게스트 Cart 식별자를 전달한다.
- OAuth Provider 오류는 Provider Adapter가 원인을 기록하고, P11 API는 `AUTH-016`으로 추상화한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P1 User | User 생성, 활성 상태·역할 조회, 역할 변경 사실 | [P1 Policy](../p1/p1-policy.md), [User API](../p1/p1-user.md) |
| P3 Cart | 로그인 성공 후 게스트 Cart 병합 | [P3 Policy](../p3/p3-policy.md), [Cart API](../p3/p3-cart.md) |
| P6 Infrastructure | Outbox 저장·재전달·멱등 eventId | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| P7 Admin | 관리자 역할 변경 요청 | [P7 Access](../p7/p7-access.md) |

## 6. API 문서와의 관계

- 인증수단·재인증 API는 [Credential API](p11-credential.md), 가입 흐름 API는 [Sign-up API](p11-signup.md), 로그인 세션 API는 [Session API](p11-session.md)에서 정의한다.
- 이 정책과 API 문서가 충돌하면 이 문서를 기준으로 API 문서를 수정한다.
- 공통 오류 응답 필드와 `AUTH-001`의 의미는 [공통 API 계약](../index.md#공통-api-계약)을 따른다. User 활성 상태 오류는 [P1 User의 `USER-004`](../p1/p1-user.md#user-disabled-error)를 따른다.
- P11 API는 토큰 원문·비밀번호·OTP를 성공 응답이나 예외의 `details`에 포함하지 않는다.
