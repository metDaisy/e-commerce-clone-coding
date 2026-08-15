# P1 User (사용자)

공통 응답 봉투, 권한 오류, 페이지네이션은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 범위

P1 User는 사용자 프로필, 계정 활성 상태, 역할, 주소록과 `Your Account`의 주소 기능을 담당한다.

- `users`는 프로필과 역할을 소유하며 로컬 로그인 이메일과 비밀번호를 저장하지 않는다.
- 사용자 리소스는 기본적으로 본인만 조회·변경할 수 있다.
- `is_enabled=false`인 사용자는 비활성 계정이며 로그인할 수 없다.
- `ADMIN`은 전체 사용자 리소스와 역할을 관리할 수 있다.
- `PRODUCT_MANAGER`는 활성 `Seller`를 가진 사용자의 판매자 역할이다.
- 한 사용자는 구매자이면서 판매자일 수 있다. 판매자 인증 정보는 P9가 아니라 P8의 `Seller`가 소유한다.
- 회원가입 인증 흐름은 P11 Auth가 소유하고, 인증 완료 후 User에 프로필 생성을 요청한다. User는 이름·연락처·역할·활성 상태를 소유한다.
- 이메일·비밀번호는 Auth의 로컬 인증수단이다. 휴대폰 번호는 회원가입 필수값이 아니며 로그인 식별자로도 사용하지 않는다. 가입 후 프로필 연락처로 선택 등록할 수 있다.
- `Your Orders`와 `Your Payments`는 [P5 Order](../p5/p5-order.md)와 [P5 Payment](../p5/p5-payment.md)가 소유하고, P1은 Account 메뉴에서 해당 화면으로 연결만 한다.
- `Login & security`는 [P11 Auth](../p11/p11-auth.md)가 소유하고, P1은 Account 메뉴에서 해당 화면으로 연결만 한다.
- 이번 구현의 `Your Account` 범위는 `Your Orders`, `Your Addresses`, `Your Payments`, `Login & security` 네 메뉴다. 신분증 제출·KYC·본인 신원 인증은 요구하지 않는다.
- 그 외 Account 메뉴는 시각적 메뉴 항목만 둘 수 있으며, 클릭해도 라우팅·API 호출·상태 변경·알림을 발생시키지 않는다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/me` | 로그인 | 내 프로필 조회 |
| PATCH | `/api/v1/me` | 로그인 | 내 프로필 수정 |
| POST | `/api/v1/me/deactivate` | 로그인 | 계정 비활성화 |
| GET | `/api/v1/me/addresses` | 로그인 | 주소 목록 |
| POST | `/api/v1/me/addresses` | 로그인 | 주소 등록 |
| PATCH | `/api/v1/me/addresses/{addressId}` | 로그인 | 주소 수정 |
| DELETE | `/api/v1/me/addresses/{addressId}` | 로그인 | 주소 삭제 |
| POST | `/api/v1/me/addresses/{addressId}/default` | 로그인 | 기본 배송지 지정 |

## 3. 요구사항

### 3-1. Auth 회원가입 완료 연계

회원가입 세션, 비밀번호, 이메일 OTP의 발급·검증·재전송은 [P11 Auth](../p11/p11-auth.md)가 소유한다. P1 User는 이메일 인증이 완료된 요청을 받아 사용자 프로필을 생성한다.

- 이메일 인증이 끝나기 전에는 정식 `User`를 만들지 않는다. 중간 상태는 Auth가 소유하는 만료 가능한 가입 세션이다.
- Auth는 인증 완료 후 이름과 선택적 연락처를 포함한 프로필 생성 요청을 User에 전달한다. User는 요청자의 이메일·비밀번호를 저장하거나 검증하지 않는다.
- User 생성 시 `is_enabled=true`, `USER` 권한, 포인트 잔액 0을 기본값으로 적용한다.
- User 생성과 기본 포인트 생성은 하나의 트랜잭션으로 처리한다. 실패하면 정식 User를 남기지 않는다.
- P1은 회원가입 화면, 가입 세션, 이메일 OTP 정책, 가입 API를 정의하지 않는다. 해당 규칙은 P11의 단일 책임이다.

### 3-2. Your Account 메뉴 범위

| 메뉴 | 상태 | 책임·동작 |
|---|---|---|
| `Your Orders` | 구현 | [P5 Order](../p5/p5-order.md)의 내 주문 목록·상세·취소 화면으로 이동 |
| `Your Addresses` | 구현 | P1의 주소 목록·등록·수정·삭제·기본 주소 지정 |
| `Your Payments` | 구현 | [P5 Payment Method](../p5/p5-payment-method.md)의 결제수단 등록·조회·삭제 |
| `Login & security` | 구현 | [P11 Auth](../p11/p11-auth.md)의 로그인·비밀번호·이메일·인증수단·세션 보안 화면으로 이동 |
| 그 외 Account 메뉴 | 비범위 | 메뉴 항목을 표시하더라도 클릭 시 아무 동작도 하지 않음 |

- 위 구현 메뉴는 로그인 사용자만 접근할 수 있다. 비로그인 사용자는 로그인 화면으로 이동한다.
- 상단 타일과 하단 `Ordering and shopping preferences` 영역에 같은 메뉴가 반복되어도 동일한 기능·라우트로 연결한다.
- `Your Addresses`와 `Your Payments`는 신분증 인증을 선행 조건으로 요구하지 않는다.
- 결제수단 등록 과정에서 PG가 카드 토큰화·결제수단 유효성 확인을 수행할 수 있으나, P1/P5는 신분증 업로드나 KYC 절차를 구현하지 않는다.
- `Prime`, `Your business account`, `Gift cards`, `Your Lists`, `Customer Service`, `Your Messages` 및 하단의 기타 링크는 비범위다. 실제 링크나 활성 버튼으로 구현하지 않으며, 클릭에 대한 API·라우팅·토스트·에러 응답을 만들지 않는다.

### 3-3. 프로필·계정

- `GET /api/v1/me`는 이름, 연락처, 가입일과 로컬 로그인 사용자에게만 존재하는 nullable `loginEmail`을 반환한다. `users` 프로필 자체는 이메일을 소유하지 않는다.
- `PATCH /api/v1/me`는 이름과 연락처만 수정한다.
- 이메일 변경은 Auth의 별도 API로 분리하고 기존 비밀번호를 재확인한다.
- 탈퇴는 물리 삭제하지 않고 `is_enabled=false`로 처리한다.
- 비활성화 후 90일이 지나면 개인정보를 마스킹한다.

### 3-4. 권한

| 권한 | 허용 범위 |
|---|---|
| `USER` | 본인 장바구니·주문·주소·리뷰 |
| `PRODUCT_MANAGER` | P9 Offer·Inventory 및 P8 Seller 기능 |
| `ADMIN` | 전체 사용자·주문·상품·쿠폰 및 권한 관리 |

- `ADMIN`만 다른 사용자의 권한을 변경한다.
- 자기 자신의 `ADMIN` 권한은 해제할 수 없다.
- 본인 소유가 아닌 리소스 접근은 `403`이다.

### 3-5. 주소록

- 사용자당 주소는 최대 5개다.
- 필수 필드는 수령인 이름, 연락처, 우편번호, 기본 주소다.
- 기본 주소는 사용자당 정확히 0~1개다.
- 기본 주소 변경은 기존 주소 해제와 신규 주소 지정을 하나의 트랜잭션으로 처리한다.
- 기본 주소 삭제 시 가장 최근 주소를 기본 주소로 승격한다.
- 주문 화면에서 주소를 별도로 조회할 수 있도록 제공하며, 선택한 주소는 최종 결제 시 주문 스냅샷으로 복사된다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 필드 검증 실패 |
| 400 | `ADDRESS_LIMIT_EXCEEDED` | 주소 5개 초과 |
| 403 | `ACCESS_DENIED` | 권한 부족 또는 타인 리소스 접근 |
| 403 | `ACCOUNT_DEACTIVATED` | 비활성화 계정 사용 |
