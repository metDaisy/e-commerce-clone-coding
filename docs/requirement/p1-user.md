# P1 User (사용자)

공통 응답 봉투, 권한 오류, 페이지네이션은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P1 User는 사용자 프로필, 계정 활성 상태, 역할, 주소록, 포인트, 관심 상품을 담당한다.

- `users`는 프로필과 역할을 소유하며 로컬 로그인 이메일과 비밀번호를 저장하지 않는다.
- 사용자 리소스는 기본적으로 본인만 조회·변경할 수 있다.
- `is_enabled=false`인 사용자는 비활성 계정이며 로그인할 수 없다.
- `ADMIN`은 전체 사용자 리소스와 역할을 관리할 수 있다.
- `PRODUCT_MANAGER`는 활성 `Seller`를 가진 사용자의 판매자 역할이다.
- 한 사용자는 구매자이면서 판매자일 수 있다. 판매자 인증 정보는 P9가 아니라 P8의 `Seller`가 소유한다.
- Auth의 회원가입 흐름은 User에 프로필 생성을 요청하며, User는 이름·연락처·역할·활성 상태를 소유한다.

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
| GET | `/api/v1/me/points` | 로그인 | 포인트 잔액·원장 조회 |
| GET | `/api/v1/me/wishlists` | 로그인 | 관심 상품 조회 |
| PUT | `/api/v1/me/wishlists/{variantId}` | 로그인 | ProductVariant 관심 상품 추가 |
| DELETE | `/api/v1/me/wishlists/{variantId}` | 로그인 | ProductVariant 관심 상품 삭제 |

## 3. 요구사항

### 3-1. 프로필·계정

- `GET /api/v1/me`는 이름, 연락처, 가입일, 포인트 잔액과 로컬 로그인 사용자에게만 존재하는 nullable `loginEmail`을 반환한다. `users` 프로필 자체는 이메일을 소유하지 않는다.
- `PATCH /api/v1/me`는 이름과 연락처만 수정한다.
- 이메일 변경은 Auth의 별도 API로 분리하고 기존 비밀번호를 재확인한다.
- 탈퇴는 물리 삭제하지 않고 `is_enabled=false`로 처리한다.
- 비활성화 후 90일이 지나면 개인정보를 마스킹한다.

### 3-2. 권한

| 권한 | 허용 범위 |
|---|---|
| `USER` | 본인 장바구니·주문·주소·위시리스트·리뷰 |
| `PRODUCT_MANAGER` | P9 Offer·Inventory 및 P8 Seller 기능 |
| `ADMIN` | 전체 사용자·주문·상품·쿠폰 및 권한 관리 |

- `ADMIN`만 다른 사용자의 권한을 변경한다.
- 자기 자신의 `ADMIN` 권한은 해제할 수 없다.
- 본인 소유가 아닌 리소스 접근은 `403`이다.

### 3-3. 주소록

- 사용자당 주소는 최대 5개다.
- 필수 필드는 수령인 이름, 연락처, 우편번호, 기본 주소다.
- 기본 주소는 사용자당 정확히 0~1개다.
- 기본 주소 변경은 기존 주소 해제와 신규 주소 지정을 하나의 트랜잭션으로 처리한다.
- 기본 주소 삭제 시 가장 최근 주소를 기본 주소로 승격한다.
- 주문 생성 시 주소 전체를 주문 스냅샷으로 복사한다.

### 3-4. 포인트

- 배송 완료 시 최종 결제 금액의 1%를 적립한다.
- 텍스트 리뷰는 50P, 이미지 리뷰는 100P를 상품당 1회 적립한다.
- 100P 이상부터 사용할 수 있고 한 주문에서 결제 금액의 50%까지만 사용한다.
- 적립 포인트는 1년 후 만료된다.
- 원장은 INSERT만 허용한다. 취소는 반대 금액의 새 원장을 생성한다.
- 잔액은 음수가 될 수 없다.
- `GET /api/v1/me/points`의 원장 목록은 `createdAt DESC, pointHistoryId DESC` 순서의 커서 기반 조회다.
- 첫 조회는 `cursor` 없이 요청하고, 이후 응답의 `nextCursor`를 전달한다. `size` 기본값은 20, 최대값은 100이다.
- 응답은 `balance`, `data`, `nextCursor`, `hasNext`를 포함하며 전체 건수는 제공하지 않는다. `data`는 Point History 배열이다.

#### 심화 사항

- 포인트 만료 예정 알림을 지원한다.

### 3-5. 관심 상품

- 관심 상품은 실제 구매 단위인 `ProductVariant`를 저장한다.
- `PUT`은 관심 상품을 추가하고 이미 존재하면 성공으로 처리한다.
- `DELETE`는 존재하지 않아도 성공으로 처리한다.
- `(user_id, variant_id)`는 UNIQUE다.
- 조회 응답에는 `variantId`, `catalogProductId`, SKU, 상품명, 현재 가격, 썸네일, 구매 가능 상태를 포함한다.
- ProductVariant가 품절·보관되어도 관심 관계는 유지하고 조회 화면에서 상태를 구분한다.

#### 심화 사항

- 관심 상품 가격 하락 알림을 지원한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 필드 검증 실패 |
| 400 | `ADDRESS_LIMIT_EXCEEDED` | 주소 5개 초과 |
| 400 | `INSUFFICIENT_POINT` | 포인트 잔액 부족 |
| 403 | `ACCESS_DENIED` | 권한 부족 또는 타인 리소스 접근 |
| 403 | `ACCOUNT_DEACTIVATED` | 비활성화 계정 사용 |
| 404 | `WISHLIST_VARIANT_NOT_FOUND` | 관심 상품 대상 ProductVariant 없음 |
