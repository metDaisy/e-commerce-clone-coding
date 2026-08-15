# P4 Coupon (쿠폰)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P4 쿠폰은 상품에 연결된 기간 한정 할인 캠페인이다. `ADMIN`과 `PRODUCT_MANAGER`는 같은 쿠폰 스키마와 API를 사용한다.

- `ADMIN`: 플랫폼 운영 범위의 상품에 쿠폰을 적용한다.
- `PRODUCT_MANAGER`: 본인이 판매하는 Offer에만 쿠폰을 적용한다.
- 모든 할인 금액은 원화(KRW) 원 단위 정수다.
- `ownerType`, `ownerId`, `sellerId`는 요청 본문으로 받지 않고 인증 주체에서 결정한다.

기본 과정은 상품 선택, 할인 정책, 기간, 고객 Clip, 주문 적용이다. 플랫폼 검증·승인, 예산, 대상 고객군, 중복 적용, 통계는 심화 과정이다.

## 2. 문서 구성

- [쿠폰 데이터 모델·상태](p4-coupon-model.md)
- [판매자·관리자 쿠폰 운영](p4-coupon-seller.md)
- [고객 쿠폰 사용](p4-coupon-customer.md)
- [쿠폰 심화 과정](p4-coupon-advanced.md)

## 3. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/coupons` | `ADMIN` 또는 `PRODUCT_MANAGER` + `ACTIVE` | 쿠폰 생성 |
| PATCH | `/api/v1/coupons/{couponId}` | 소유자 또는 `ADMIN` | 쿠폰 정보·기간 수정 |
| DELETE | `/api/v1/coupons/{id}/inactive` | 소유자 또는 `ADMIN` | 쿠폰 비활성화 |
| GET | `/api/v1/coupons` | `ADMIN` 또는 `PRODUCT_MANAGER` + `ACTIVE` | 권한 범위 쿠폰 목록 |
| GET | `/api/v1/coupons/{couponId}` | 소유자 또는 `ADMIN` | 쿠폰 상세 조회 |
| POST | `/api/v1/coupons/{couponId}/clip` | 로그인 | 고객 쿠폰 저장 |
| GET | `/api/v1/me/coupons` | 로그인 | 고객 보유 쿠폰 조회 |
| GET | `/api/v1/coupons/{couponId}/statistics` | 소유자 또는 `ADMIN` | 통계 조회 (심화) |

## 4. 공통 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 공통 필드 형식 오류 |
| 400 | `COUPON_DISCOUNT_INVALID` | 할인 값 오류 |
| 400 | `COUPON_PERIOD_INVALID` | 기간 오류 또는 최대 기간 초과 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 활성 Seller 없는 판매자 API 호출 |
| 403 | `COUPON_ACCESS_DENIED` | 소유하지 않은 쿠폰·Offer 접근 |
| 404 | `COUPON_NOT_FOUND` | 쿠폰 없음 |
| 404 | `COUPON_TARGET_NOT_FOUND` | 대상 Offer 없음 |
| 409 | `COUPON_TARGET_NOT_ELIGIBLE` | 대상 Offer 비활성·보관·소유권 불일치 |
| 409 | `COUPON_ALREADY_CLIPPED` | 같은 쿠폰을 이미 Clip함 |
| 409 | `COUPON_NOT_CLIPPABLE` | 예약·비활성 쿠폰 Clip 시도 |
| 409 | `COUPON_NOT_APPLICABLE` | 주문 Offer·기간·사용 조건 불일치 |
| 409 | `COUPON_ACTIVE_IMMUTABLE` | ACTIVE 쿠폰의 `validUntil` 이외 필드 수정 |
