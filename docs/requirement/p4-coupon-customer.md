# P4 Coupon Customer (고객 쿠폰 사용)

[P4 쿠폰 개요](p4-coupon.md)와 [쿠폰 데이터 모델](p4-coupon-model.md)을 따른다.

## 1. 쿠폰 Clip

`POST /api/v1/coupons/{couponId}/clip`

- 로그인한 고객만 Clip할 수 있다.
- `ACTIVE` 쿠폰만 Clip할 수 있다.
- `(couponId, userId)` 중복 Clip은 허용하지 않는다.
- Clip 레코드의 `createdAt`은 고객이 쿠폰을 저장한 시각이다.
- 중복 Clip은 `409 COUPON_ALREADY_CLIPPED`를 반환한다.

## 2. 고객 보유 쿠폰 조회

`GET /api/v1/me/coupons?page=0&size=20&sort=DESC`

- 고객 본인이 Clip한 쿠폰 중 `Coupon.status = ACTIVE`이고 `CouponClip.status = CLIPPED`인 쿠폰만 반환한다.
- 사용 완료·만료·비활성 쿠폰은 목록에서 제외한다.
- 페이지 기반 조회를 사용하며 기본 `page=0`, `size=20`, 최대 `size=100`이다.
- 정렬 기준은 `createdAt`으로 고정하며, 정렬 방향만 `ASC` 또는 `DESC`로 선택한다.
- 기본 정렬 방향은 `DESC`이며, 동일한 `createdAt`에서는 `clipId DESC`를 보조 정렬로 사용한다.
- `status` 필터는 받지 않는다. 결과의 Clip 상태는 항상 `CLIPPED`다.
- 항목은 `clipId`, `couponId`, `name`, `ownerType`, `status`, `discountType`, `discount`, `validFrom`, `validUntil`을 포함한다.
- 응답은 공통 페이지 형식의 `data`, `page`, `size`, `totalElements`, `totalPages`를 사용한다. 다음 페이지 존재 여부는 `page + 1 < totalPages`로 판단한다.

요청 예시:

`GET /api/v1/me/coupons?page=0&size=20&sort=DESC`

```json
{
  "page": 0,
  "size": 20,
  "sort": "DESC"
}
```

`status`는 요청으로 전달하지 않는다. 서버는 고객 본인의 Clip 중 쿠폰이 `ACTIVE`이고 Clip이 `CLIPPED`인 데이터만 반환한다.

응답 예시 `200 OK`:

```json
{
  "data": [
    {
      "clipId": "uuid-clip-1",
      "couponId": "uuid-coupon-1",
      "name": "여름 시즌 10% 할인",
      "ownerType": "SELLER",
      "status": "CLIPPED",
      "discountType": "PERCENTAGE",
      "discount": 10,
      "validFrom": "2026-08-01T00:00:00Z",
      "validUntil": "2026-08-31T23:59:59Z"
    },
    {
      "clipId": "uuid-clip-2",
      "couponId": "uuid-coupon-2",
      "name": "플랫폼 5000원 할인",
      "ownerType": "PLATFORM",
      "status": "CLIPPED",
      "discountType": "FIXED_AMOUNT",
      "discount": 5000,
      "validFrom": "2026-08-05T00:00:00Z",
      "validUntil": "2026-08-25T23:59:59Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

## 3. 주문 적용

쿠폰 적용과 사용 기록 생성은 주문 처리의 원자적 범위에 포함한다.

- 주문의 Offer가 `CouponTarget`에 포함되어야 한다.
- `validFrom <= now < validUntil`이어야 한다.
- 고객의 Clip이 존재하고 `CLIPPED` 상태여야 한다.
- 기본 과정의 고객별 최대 사용 횟수는 1회다.
- 퍼센트 쿠폰은 한 주문에서 최대 5개의 대상 상품에 적용한다.
- 정액 쿠폰은 한 주문에서 대상 상품 1개에 한 번만 적용한다.
- 할인 금액이 대상 상품의 결제 금액보다 크면 주문을 거절한다.
- 결제 확정 시 `CouponRedemption`을 `CONFIRMED`로 생성한다. `redeemedAt`은 쿠폰이 주문에 실제 적용된 시각이다.
- 결제 실패·주문 생성 실패 시 쿠폰 사용 기록을 생성하지 않고 Clip 상태를 유지한다.
- 주문 취소·환불에 따른 사용 복원은 P5에서 정의한다.
