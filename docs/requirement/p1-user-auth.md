# P1 User & Auth (사용자 및 인증)

공통 응답 봉투, 인증 오류, 페이지네이션은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P1은 사용자, 인증 수단, 권한, 주소록, 포인트, 관심 상품을 담당한다.

- 비밀번호·토큰·OAuth 비밀값은 로그와 이벤트 payload에 평문으로 남기지 않는다.
- 사용자의 리소스는 기본적으로 본인만 조회·변경할 수 있다.
- `ADMIN`은 전체 리소스를 관리할 수 있고, `PRODUCT_MANAGER`는 판매자로서 자신의 CatalogProduct·Offer·Inventory를 관리한다.
- `USER`는 구매자 역할이다. 판매 기능은 P8의 `SellerProfile`을 통해 별도로 활성화한다.
- 한 사용자는 구매자이면서 판매자일 수 있으며, 판매자 인증 정보는 `SellerProfile`로 관리한다.
- `PRODUCT_MANAGER`는 판매자 역할이며, 플랫폼 전체 운영은 `ADMIN`이 담당한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 공개 | 회원가입 |
| POST | `/api/v1/auth/login` | 공개 | 이메일·비밀번호 로그인 |
| GET | `/api/v1/auth/oauth/{provider}/authorize` | 공개 | OAuth 인증 시작 |
| GET | `/api/v1/auth/oauth/{provider}/callback` | 공개 | OAuth callback 처리 |
| POST | `/api/v1/auth/refresh` | Refresh Token | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | 로그인 | 현재 기기 로그아웃 |
| POST | `/api/v1/auth/logout-all` | 로그인 | 전체 기기 로그아웃 |
| GET | `/api/v1/me` | 로그인 | 내 프로필 조회 |
| PATCH | `/api/v1/me` | 로그인 | 내 프로필 수정 |
| POST | `/api/v1/me/deactivate` | 로그인 | 계정 비활성화 |
| GET | `/api/v1/me/addresses` | 로그인 | 주소 목록 |
| POST | `/api/v1/me/addresses` | 로그인 | 주소 등록 |
| PATCH | `/api/v1/me/addresses/{addressId}` | 로그인 | 주소 수정 |
| DELETE | `/api/v1/me/addresses/{addressId}` | 로그인 | 주소 삭제 |
| POST | `/api/v1/me/addresses/{addressId}/default` | 로그인 | 기본 배송지 지정 |
| GET | `/api/v1/me/points` | 로그인 | 포인트 잔액·원장 조회 |
| GET | `/api/v1/me/wishlists` | 로그인 | 관심 상품 조회 |
| PUT | `/api/v1/me/wishlists/{catalogProductId}` | 로그인 | 관심 상품 추가 |
| DELETE | `/api/v1/me/wishlists/{catalogProductId}` | 로그인 | 관심 상품 삭제 |

## 3. 요구사항

### 3-1. 회원가입

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
- 이메일은 RFC 5322 형식이며 대소문자를 구분하지 않는다.
- 이메일은 정규화 후 UNIQUE다.
- 비밀번호는 8자 이상이며 영문 대소문자·숫자·특수문자 중 2종 이상을 포함한다.
- 비밀번호는 Bcrypt로 저장한다.
- 가입 성공 시 `USER` 권한과 포인트 잔액 0을 생성한다.
- 응답에는 `userId`, `name`, `email`, `role`, `createdAt`만 포함한다. 비밀번호는 포함하지 않는다.

성공 응답 `201`:

```json
{
  "userId": "uuid",
  "name": "홍길동",
  "email": "user@example.com",
  "role": "USER",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 로그인·토큰

`POST /api/v1/auth/login`

- 성공 시 HttpOnly Secure 쿠키에 Access Token(30분)과 Refresh Token(7일)을 설정한다.
- JWT에는 `sub=userId`, `role`, `deviceId`, `iat`, `exp`를 포함한다.
- 비밀번호 오류 5회 누적 시 30분간 계정을 잠근다.
- 로그아웃 시 Access Token을 블랙리스트에 등록하고 Refresh Token 쿠키를 삭제한다.
- 전체 로그아웃은 사용자 기준으로 발급 시각 이전의 모든 토큰을 무효화한다.

#### 심화 사항

- 로그인 성공 시 게스트 장바구니를 회원 장바구니로 병합한다.
- 이메일 인증, 비밀번호 재설정, 기기별 세션 관리, MFA를 지원한다.

### 3-3. 소셜 인증 식별자

- `provider`는 Amazon이 정한 값이 아니라 이 프로젝트가 여러 OAuth 공급자를 통일하기 위해 정의한 애플리케이션 enum이다.
- 요구사항에서 허용하는 값은 `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB`다.
- `providerId`는 OAuth 공급자가 반환하는 외부 사용자 식별자이며 이메일 주소를 대신 사용하지 않는다.
- `provider + providerId` 조합은 UNIQUE다.
- 소셜 로그인에 의한 회원가입은 `users`와 `social_credentials`를 생성하지만 `user_credentials`를 생성하지 않는다. `user_credentials`는 이메일·비밀번호 방식으로 가입하거나 이후 로컬 인증수단을 연결할 때만 생성한다.
- 신규 소셜 사용자는 OAuth callback에서 즉시 정식 회원으로 생성하지 않고 회원가입 화면(`/signup`)으로 이동한다.
- 신규 소셜 사용자는 회원가입 화면에서 서비스가 요구하는 추가 프로필 정보를 입력한 뒤 가입을 완료한다. 이때 OAuth 인증 결과와 가입 화면 입력값을 결합하여 `users`와 `social_credentials`를 생성한다.
- 이미 동일한 `(provider, providerId)`가 존재하면 회원가입 화면으로 이동하지 않고 기존 계정으로 로그인한다.
- 신규 소셜 사용자의 가입 완료 전에는 `Guest Token`을 사용하며, 이 토큰은 회원가입 완료 용도로만 사용할 수 있고 일반 회원 API에는 사용할 수 없다.
- 공급자 access token, client secret, 원본 OAuth 응답은 DB·로그·이벤트에 저장하지 않는다.
- 소셜 인증 성공 후 기존 계정이 있으면 로그인하고, 없으면 이 프로젝트의 회원가입 보완 절차를 거쳐 로컬 `users` 계정을 생성한다.
- 이 절차에서 `provider`는 외부 서비스의 실제 회원 유형을 의미하지 않고, 우리 서비스의 `social_credentials` 연결 정보만 의미한다.

#### 심화 사항

- Google·Naver·Kakao·GitHub OAuth2 로그인과 게스트 토큰을 지원한다.
- Login with Amazon을 별도 OAuth 공급자 `AMAZON`으로 연동할 수 있다. 이때 Amazon의 `user_id`는 `providerId`로 저장하고, Amazon의 고객 계정 자체를 우리 서비스의 사용자 테이블로 간주하지 않는다.
- 다중 소셜 인증 수단 연결·해제를 지원한다.

성공 응답 `200`:

```json
{
  "user": {
    "userId": "uuid",
    "name": "홍길동",
    "email": "user@example.com",
    "role": "USER"
  },
  "expiresIn": 1800
}
```

### 3-4. 프로필·계정

- `GET /api/v1/me`는 이름, 이메일, 연락처, 가입일, 포인트 잔액을 반환한다.
- `PATCH /api/v1/me`는 이름과 연락처만 수정한다.
- 이메일 변경은 별도 API로 분리하고 기존 비밀번호를 재확인한다.
- 탈퇴는 물리 삭제하지 않고 `is_active=false`로 처리한다.
- 비활성화된 계정은 로그인할 수 없다.
- 비활성화 후 90일이 지나면 개인정보를 마스킹한다.

### 3-5. 권한

| 권한 | 허용 범위 |
|---|---|
| `USER` | 본인 장바구니·주문·주소·위시리스트·리뷰 |
| `PRODUCT_MANAGER` | 상품 CRUD·재고 관리·기간 할인 |
| `ADMIN` | 전체 사용자·주문·상품·쿠폰 및 권한 관리 |

- `ADMIN`만 다른 사용자의 권한을 변경한다.
- 자기 자신의 `ADMIN` 권한은 해제할 수 없다.
- 본인 소유가 아닌 리소스 접근은 `403`이다.

### 3-6. 주소록

- 사용자당 주소는 최대 5개다.
- 필수 필드는 수령인 이름, 연락처, 우편번호, 기본 주소다.
- 기본 주소는 사용자당 정확히 0~1개다.
- 기본 주소 변경은 기존 주소 해제와 신규 주소 지정을 하나의 트랜잭션으로 처리한다.
- 기본 주소 삭제 시 가장 최근 주소를 기본 주소로 승격한다.
- 주문 생성 시 주소 전체를 주문 스냅샷으로 복사한다.

### 3-7. 포인트

- 배송 완료 시 최종 결제 금액의 1%를 적립한다.
- 텍스트 리뷰는 50P, 이미지 리뷰는 100P를 상품당 1회 적립한다.
- 100P 이상부터 사용할 수 있고 한 주문에서 결제 금액의 50%까지만 사용한다.
- 적립 포인트는 1년 후 만료된다.
- 원장은 INSERT만 허용한다. 취소는 반대 금액의 새 원장을 생성한다.
- 잔액은 음수가 될 수 없다.
- `GET /api/v1/me/points`의 원장 목록은 `createdAt DESC, pointHistoryId DESC` 순서의 커서 기반 조회다.
- 첫 조회는 `cursor` 없이 요청하고, 이후 응답의 `nextCursor`를 전달한다. `size` 기본값은 20, 최대값은 100이다.
- 응답은 `balance`, `items`, `nextCursor`, `hasNext`를 포함하며 전체 건수는 제공하지 않는다.

#### 심화 사항

- 포인트 만료 예정 알림을 지원한다.

### 3-8. 관심 상품

- `PUT`은 관심 상품을 추가하고 이미 존재하면 성공으로 처리한다.
- `DELETE`는 존재하지 않아도 성공으로 처리한다.
- `(user_id, catalog_product_id)`는 UNIQUE다.
- 조회 응답에는 상품명, 현재 가격, 썸네일, 구매 가능 상태를 포함한다.

#### 심화 사항

- 관심 상품 가격 하락 알림을 지원한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 필드 검증 실패 |
| 400 | `INVALID_PASSWORD_FORMAT` | 비밀번호 정책 위반 |
| 400 | `ADDRESS_LIMIT_EXCEEDED` | 주소 5개 초과 |
| 400 | `INSUFFICIENT_POINT` | 포인트 잔액 부족 |
| 401 | `BAD_CREDENTIALS` | 이메일·비밀번호 불일치 |
| 401 | `INVALID_TOKEN` | 토큰 서명·만료 오류 |
| 401 | `BLACKLISTED_TOKEN` | 로그아웃된 토큰 사용 |
| 400 | `UNSUPPORTED_OAUTH_PROVIDER` | 허용하지 않은 provider 값 |
| 400 | `INVALID_OAUTH_CALLBACK` | state·code 검증 실패 |
| 403 | `ACCESS_DENIED` | 권한 부족 또는 타인 리소스 접근 |
| 403 | `ACCOUNT_DEACTIVATED` | 비활성화 계정 사용 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이메일 중복 |
| 409 | `SOCIAL_CREDENTIAL_ALREADY_LINKED` | 소셜 계정 중복 연결 |
| 409 | `LAST_AUTH_METHOD` | 마지막 인증 수단 해제 |
| 423 | `ACCOUNT_LOCKED` | 로그인 실패 누적 잠금 |
