# P1 User Policy

이 문서는 User와 Address의 API 독립적인 업무 정책을 정의한다. User API 계약은 [User API](p1-user.md), Address API 계약은 [Address API](p1-address.md)를 따른다.

## 1. 범위와 책임

### 범위

- 사용자의 이름·연락처·역할·활성 상태와 가입 시각을 관리한다.
- 인증 완료 후 정식 User 프로필을 생성하고 기본 역할과 초기 포인트 잔액을 설정한다.
- 사용자가 소유한 배송지의 등록·조회·수정·삭제·기본 배송지 지정을 관리한다.
- 역할 집합 변경이라는 도메인 사실을 `UserRolesChangedEvent`로 공개한다.

### 범위 밖

- 이메일·비밀번호·OAuth 식별자·Access/Refresh/Guest Token·로그인 세션은 P11 Auth가 소유한다.
- Seller 프로필·판매자 심사·판매자 상태는 P8 Seller가 소유한다.
- 관리자 역할 변경 요청과 관리자용 사용자 운영 API는 P7 Admin이 소유한다.
- 주문 배송지 선택과 결제 시점의 불변 주소 스냅샷은 P5 Order가 소유한다. P5는 P1 Address의 현재 값을 직접 변경하지 않는다.

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| User 프로필·역할·활성 상태 | P1 User | [User API](p1-user.md) |
| Address 원본·소유권·기본 배송지 | P1 User | [Address API](p1-address.md) |
| 인증 완료와 인증수단 연결 | P11 Auth | [P11 Policy](../p11/p11-policy.md), [Sign-up API](../p11/p11-signup.md), [Credential API](../p11/p11-credential.md) |
| 역할 변경의 관리자 진입점 | P7 Admin | [P7 Access](../p7/p7-access.md) |
| 주소 사용·배송지 스냅샷 | P5 Order | [P5 Policy](../p5/p5-policy.md) |
| 이벤트 전달·재시도 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `User` | 이름·연락처·역할·활성 상태를 가진 고객 프로필의 주체. 인증수단 자체는 소유하지 않는다. |
| `Address` | User가 주문에 사용할 목적으로 관리하는 배송지 원본. |
| `Role` | 접근 권한의 집합. `USER`, `PRODUCT_MANAGER`, `ADMIN`을 동시에 보유할 수 있다. |
| `USER` | 모든 정식 계정에 부여되는 기본 구매자 역할. 제거할 수 없다. |
| `PRODUCT_MANAGER` | `ACTIVE` Seller와 연결된 판매자 기능용 추가 역할. 판매자 상태와의 생명주기는 P8이 정의한다. |
| `ADMIN` | 플랫폼 운영 역할. 다른 사용자의 역할 변경은 P7의 관리자 API를 통해서만 수행한다. |
| `Address` 소유자 | 해당 User. 본인 주소만 변경·삭제·기본 지정할 수 있다. |
| `기본 배송지` | User당 최대 하나로 지정되는 Address. 주소 목록의 대표 배송지일 뿐 주문 스냅샷 자체가 아니다. |
| `계정 비활성화` | User를 삭제하지 않고 사용과 로그인을 막는 논리적 비활성화. 주문 이력과 참조 무결성을 보존한다. |

용어의 기준은 [도메인 용어집](../../domain-glossary.md)이다. 용어 의미를 변경할 때는 용어집과 관련 P5·P7·P8·P11 문서를 함께 검토한다.

## 3. 핵심 업무 규칙

### User 생성과 프로필

1. 이메일 인증 또는 OAuth 인증이 완료되기 전에는 정식 User를 생성하지 않는다. 만료 가능한 가입 세션은 P11 Auth가 소유한다.
2. P11 Auth는 인증 완료 후 이름과 선택적 연락처를 포함한 프로필 생성 요청을 User에 전달한다. P1은 이메일·비밀번호·OAuth 비밀값을 저장하거나 검증하지 않는다.
3. User 생성 시 도메인 역할 집합 `{USER}`와 `isEnabled=true`, 포인트 잔액 `0`을 적용한다. API·Auth DTO 응답에서는 `roles=["USER"]`로, JWT claim·이벤트 payload에서는 `roles="USER"`로 표현한다. User 생성과 초기 포인트 생성은 하나의 트랜잭션으로 처리한다.
4. 이름은 필수이며 API 입력은 공백만으로 구성될 수 없다. 연락처는 선택값이며 입력하면 유효한 전화번호 형식이어야 하고 User 사이에서 중복될 수 없다.
5. `loginEmail`은 P11의 `UserCredential`에 존재할 때만 공개할 수 있다. 소셜 전용 User에는 `loginEmail`을 반환하지 않거나 `null`로 반환한다.
6. 프로필 수정은 이름과 연락처만 변경한다. 이메일 변경·비밀번호 변경·인증수단 연결은 P11 Auth의 별도 계약을 사용한다.

### User 보호 API 재인증

1. 내 프로필 조회·수정·계정 비활성화는 User 개인정보와 계정 상태를 다루므로, 유효한 Access Token만으로 접근할 수 없다. 세 API 모두 공통 계정 관리 재인증을 요구한다.
2. 클라이언트는 최초 보호 API 호출 전에 P11의 재인증을 수행하고, P11은 `__Host-REAUTH` 쿠키를 발급한다. 쿠키는 재인증 후 30분 동안 세 보호 API에 재사용할 수 있다.
3. 로컬 `UserCredential`이 있는 User는 기존 비밀번호를 입력해 재인증한다. 비밀번호 불일치·잠금·로컬 인증수단 부재의 원본 예외는 P11이 소유한다.
4. OAuth 전용 User는 비밀번호가 없으므로 연결된 `SocialCredential`의 OAuth 공급자 재인증을 완료한다. 현재 Access Token이나 이미 로그인된 애플리케이션 세션만으로는 재인증을 충족하지 않는다.
5. 여러 OAuth 인증수단이 연결된 User는 연결된 공급자 중 하나를 선택할 수 있다. 공급자 재인증이 완료되지 않으면 보호 API를 수행하지 않는다.
6. OAuth 공급자가 새 인증을 완료할 수 없고 로컬 인증수단도 없다면 보호 API를 거절한다. User는 P11 `Login & security`에서 로컬 인증수단을 추가한 후 비밀번호 재인증을 사용할 수 있다.
7. P1은 비밀번호 원문·OAuth access token·공급자 응답을 받거나 저장하지 않는다. 재인증 검증·쿠키 발급·민감정보 마스킹은 P11이 담당한다.

### 역할과 권한

1. `USER`는 모든 계정이 기본으로 보유하며 삭제할 수 없다.
2. `PRODUCT_MANAGER`와 `ADMIN`은 서로 독립적인 추가 역할이다. 한 User가 여러 역할을 동시에 보유할 수 있으며, 도메인에서는 역할 집합으로 관리하고 API·Auth DTO 응답에서는 JSON 배열로 반환한다. JWT claim·이벤트 payload에서는 `USER,PRODUCT_MANAGER`처럼 쉼표로 구분한다.
3. 역할 집합 변경은 P7의 관리자 정책 또는 P8 Seller 생명주기에서 요청한다. P1은 유효성·불변식에 따라 저장하고 결과 사실을 이벤트로 공개한다.
4. 실제 역할 집합이 변경된 경우에만 `UserRolesChangedEvent`를 발행한다. 같은 역할을 다시 추가·삭제하는 요청은 상태를 변경하지 않는다.
5. 역할 집합 변경과 Outbox 기록은 같은 트랜잭션에 속한다. 이벤트 payload에는 비밀번호·토큰·세션 비밀값을 포함하지 않는다.
6. 비활성 User는 로그인과 인증이 필요한 기능을 사용할 수 없다. 기존 세션의 즉시 무효화는 P11 Auth가 담당한다.

### Account 메뉴

| 메뉴 | 담당 | P1의 책임 |
|---|---|---|
| `Your Orders` | P5 Order | 주문 화면으로 연결만 한다. |
| `Your Addresses` | P1 User | 주소 CRUD와 기본 배송지 지정을 제공한다. |
| `Your Payments` | P5 Payment Method | 결제수단 화면으로 연결만 한다. |
| `Login & security` | P11 Auth | 인증수단·비밀번호·세션 보안 화면으로 연결만 한다. |
| 그 외 Account 메뉴 | 비범위 | 표시되더라도 라우팅·API 호출·상태 변경을 발생시키지 않는다. |

### Address

1. Address는 하나의 User에 속하며 User 간에 공유되지 않는다.
2. User당 Address는 최대 5개다.
3. 수령인 이름·수령인 연락처·우편번호·기본 주소는 필수다.
4. 기본 배송지는 User당 정확히 0개 또는 1개다. 새 기본 배송지를 지정할 때 기존 기본값 해제와 신규 지정을 하나의 트랜잭션으로 처리한다.
5. 기본 배송지를 삭제하면 남아 있는 주소 중 가장 최근 주소를 기본 배송지로 승격한다. 주소가 남지 않으면 기본 배송지는 0개가 된다.
6. Address 수정·삭제·기본 지정은 소유 User의 요청만 허용한다.
7. 주문은 최종 결제 시점에 Address 값을 스냅샷으로 복사한다. 이후 Address 수정·삭제가 이미 생성된 주문의 배송지 스냅샷을 바꾸지 않는다.

## 4. 불변식과 상태 전이

### 불변식

- `User.id`와 `Address.id`는 각각 전역적으로 유일하다.
- `User.roles`는 도메인에서 중복 없는 역할 집합이며 항상 `USER`를 포함한다. API·Auth DTO 응답은 JSON 배열이며, JWT claim·이벤트 payload는 쉼표로 구분한 문자열이다. 역할 코드는 중복되거나 공백을 포함할 수 없다.
- 비활성화는 물리 삭제가 아니며 `isEnabled=false`로 표현한다.
- 연락처가 있으면 User 간 유일해야 한다.
- `Address.userId`는 하나의 User만 가리키며, User당 Address 수는 5개 이하이다.
- 한 User의 `isPrimary=true` Address는 최대 하나다.
- 기본 배송지 변경·삭제에서 일시적인 중복 기본값이 외부에 관찰되지 않도록 하나의 트랜잭션으로 처리한다.
- 역할 집합 저장과 `UserRolesChangedEvent`의 Outbox 기록은 원자적으로 처리한다.

### 상태 전이

| 리소스 | 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|---|
| User | `ENABLED` | 본인 계정 비활성화 | `DISABLED` | User |
| User | `DISABLED` | 로그인·인증 요청 | `DISABLED` 유지, 요청 거절 | P11 Auth |
| User | `ENABLED` 또는 `DISABLED` | P7/P8이 유효한 역할 변경 | 동일 활성 상태, 역할 집합 변경 | P1 User + P7/P8 |
| Address | 기본 아님 | 기본 배송지 지정 | 기본 배송지 | User |
| Address | 기본 배송지 | 다른 주소를 기본으로 지정 | 기본 아님 | User |
| Address | 기본 배송지 | 주소 삭제 | 삭제 후 최근 주소가 있으면 그 주소가 기본 | User |

P1에는 자기 계정 재활성화 API가 없다. 재활성화가 필요하면 P7 Admin의 계정 운영 정책을 별도로 정의해야 한다.

## 5. 도메인 간 규칙과 예외 소유권

- P11은 인증 완료 사실과 인증된 `userId`를 제공한다. P1은 P11의 Credential·Token 내부 구조를 조회하지 않는다.
- P7은 관리자 권한으로 역할 변경을 요청한다. P1은 `USER` 제거, 존재하지 않는 역할, 자기 자신의 `ADMIN` 해제 같은 불변식을 거절한다.
- P8은 Seller 승인·정지에 따라 `PRODUCT_MANAGER` 역할 변경을 요청한다. Seller의 상태와 판매자 API 권한은 P8이 소유한다.
- P5는 P1의 Address API로 소유권과 현재 값을 확인하고 결제 시 스냅샷을 저장한다. 주문의 배송지 상태는 P5가 소유한다.
- P6은 P1이 기록한 Outbox를 전달·재시도하며 `eventId` 기준 멱등 처리를 보장한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P11 Auth | 인증 완료, 로그인 차단, 세션 무효화 | [P11 Policy](../p11/p11-policy.md), [Session API](../p11/p11-session.md) |
| P5 Order | 주소 소유권 확인과 주문 스냅샷 | [P5 Policy](../p5/p5-policy.md) |
| P7 Admin | 관리자 역할·계정 운영 | [P7 Access](../p7/p7-access.md) |
| P8 Seller | Seller 상태와 `PRODUCT_MANAGER` 역할 | [P8 Seller Policy](../p8/p8-policy.md) |
| P6 Infrastructure | Outbox 전달·재시도 | [P6 Infrastructure](../p6/p6-infrastructure.md) |

## 6. API 문서와의 관계

- User의 필드, 요청, 성공 응답, User 예외는 [User API](p1-user.md)에서 정의한다.
- Address의 필드, 요청, 성공 응답, 주소 예외는 [Address API](p1-address.md)에서 정의한다.
- 공통 오류 응답 필드·인증 오류·페이지네이션은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
- 이 정책과 API 문서가 충돌하면 이 문서를 기준으로 API 문서를 수정한다.
