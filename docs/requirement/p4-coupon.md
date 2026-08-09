# P4 Coupon (쿠폰 및 혜택)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/coupons/{couponId}/claim` | 로그인 | 쿠폰 발급 |
| GET | `/api/v1/me/coupons` | 로그인 | 내 쿠폰 조회 |

## 2. 요구사항

### 2-1. 쿠폰 생성

관리자 쿠폰 생성 요청:

요청:

```json
{
  "name": "신규 가입 10% 할인",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxDiscountAmount": 5000,
  "minimumOrderAmount": 30000,
  "applicableCategoryId": null,
  "totalQuantity": 1000,
  "validFrom": "2026-08-09T00:00:00Z",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

- `PERCENTAGE`는 0 초과 100 이하이고 최대 할인 금액이 필수다.
- `FIXED_AMOUNT`는 할인 금액이 주문 상품 금액보다 클 수 없다.
- `validFrom < validUntil`이어야 한다.
- `totalQuantity = null`은 무제한이다.

#### 심화 사항

- 사용자·카테고리·상품·첫 구매·기간·등급별 쿠폰을 지원한다.
- 쿠폰 중복 적용 우선순위와 제외 상품을 지원한다.

성공 응답 `201`:

```json
{
  "couponId": "uuid",
  "name": "신규 가입 10% 할인",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "status": "ACTIVE",
  "issuedQuantity": 0,
  "totalQuantity": 1000,
  "validFrom": "2026-08-09T00:00:00Z",
  "validUntil": "2026-09-09T00:00:00Z"
}
```

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
