# P4 Coupon (쿠폰 및 혜택)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 생성 (P7 진입점) |
| PATCH | `/api/v1/admin/coupons/{couponId}` | `ADMIN` | 쿠폰명·만료일 수정, 비활성화 (P7 진입점) |
| GET | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 관리 목록 (P7 진입점) |
| POST | `/api/v1/coupons/{couponId}/claim` | 로그인 | 쿠폰 발급 |
| GET | `/api/v1/me/coupons` | 로그인 | 내 쿠폰 조회 |

## 2. 요구사항

### 2-1. 쿠폰 생성

`POST /api/v1/admin/coupons`

요청:

```json
{
  "name": "신규 가입 10% 할인",
  "discountType": "PERCENTAGE",
  "discountRate": 10,
  "maxDiscountAmount": { "amount": 5000.00, "currency": "KRW" },
  "minimumOrderAmount": { "amount": 30000.00, "currency": "KRW" },
  "totalQuantity": 1000,
  "validFrom": "2026-08-09T00:00:00Z",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

- `PERCENTAGE`는 `discountRate`를 사용하며 0 초과 100 이하다. `maxDiscountAmount`가 필수다.
- `FIXED_AMOUNT`는 `discountAmount`를 사용한다. `discountRate`, `maxDiscountAmount`는 전달할 수 없다.
- 모든 금액은 `{ "amount", "currency" }` 형식이고, 한 Coupon의 금액 필드는 모두 같은 ISO 4217 `currency`를 사용한다.
- `FIXED_AMOUNT`의 할인 금액은 주문 상품 금액보다 클 수 없다.
- `validFrom < validUntil`이어야 한다.
- `totalQuantity = null`은 무제한이다.

`FIXED_AMOUNT` 요청은 다음 형태를 사용한다.

```json
{
  "name": "5,000원 할인",
  "discountType": "FIXED_AMOUNT",
  "discountAmount": { "amount": 5000.00, "currency": "KRW" },
  "minimumOrderAmount": { "amount": 30000.00, "currency": "KRW" },
  "totalQuantity": 1000,
  "validFrom": "2026-08-09T00:00:00Z",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

#### 심화 사항

- 사용자·카테고리·상품·첫 구매·기간·등급별 쿠폰을 지원한다.
- 쿠폰 중복 적용 우선순위와 제외 상품을 지원한다.

성공 응답 `201`:

```json
{
  "couponId": "uuid",
  "name": "신규 가입 10% 할인",
  "discountType": "PERCENTAGE",
  "discountRate": 10,
  "maxDiscountAmount": { "amount": 5000.00, "currency": "KRW" },
  "minimumOrderAmount": { "amount": 30000.00, "currency": "KRW" },
  "status": "ACTIVE",
  "issuedQuantity": 0,
  "totalQuantity": 1000,
  "validFrom": "2026-08-09T00:00:00Z",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

`PATCH /api/v1/admin/coupons/{couponId}`는 `name`, `validUntil`, `status`만 수정한다.

- 할인 유형·할인 값·최소 주문 금액·발급 한도는 생성 후 변경하지 않는다.
- `status`는 `ACTIVE → INACTIVE`만 허용하며 비활성 쿠폰은 신규 발급할 수 없다.
- `validUntil`은 `validFrom` 이후여야 한다.

`GET /api/v1/admin/coupons`는 `page`, `size`, `status`를 받아 Coupon의 발급·잔여 수량과 기간을 페이지 기반 목록으로 반환한다.

### 2-2. 쿠폰 발급·조회

`POST /api/v1/coupons/{couponId}/claim` 성공 응답:

```json
{
  "userCouponId": "uuid",
  "couponId": "uuid",
  "status": "AVAILABLE",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

`GET /api/v1/me/coupons`는 `page`, `size`, `status`를 받아 로그인 사용자의 User Coupon을 페이지 기반 목록으로 반환한다. 각 항목은 `userCouponId`, `couponId`, `name`, `status`, `validUntil`을 포함한다.

사용자 쿠폰 상태는 `AVAILABLE`, `USED`, `EXPIRED`다.

- 발급 수량을 원자적으로 증가한다.
- 중복 발급은 허용하지 않는다.
- 만료 스케줄러는 매 정시에 실행하고 1000건 단위로 처리한다.
- 주문 취소 시 유효기간이 남은 쿠폰만 `AVAILABLE`로 복원한다.

#### 심화 사항

- 자동 발급, 쿠폰 코드, 프로모션 그룹, 사용 이력과 감사 로그를 지원한다.

## 3. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 쿠폰 값·기간 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 발급·조회에 로그인 필요 |
| 403 | `ACCESS_DENIED` | 관리자 권한 부족 |
| 404 | `COUPON_NOT_FOUND` | 쿠폰 없음 |
| 409 | `COUPON_ALREADY_CLAIMED` | 중복 발급 |
| 409 | `COUPON_EXHAUSTED` | 발급 수량 소진 |
| 409 | `COUPON_NOT_AVAILABLE` | 비활성·미발급·만료 상태 |
| 409 | `COUPON_MINIMUM_ORDER_NOT_MET` | 최소 주문 금액 미달 |
