# P4 Coupon Seller (판매자·관리자 쿠폰 운영)

[P4 쿠폰 개요](p4-coupon.md)와 [쿠폰 데이터 모델](p4-coupon-model.md)을 따른다.

## 1. 생성

`POST /api/v1/coupons`

관리자와 판매자는 같은 요청 스키마를 사용한다.

```json
{
  "name": "신규 상품 10% 할인",
  "targetOfferIds": ["uuid-offer-1", "uuid-offer-2"],
  "discountType": "PERCENTAGE",
  "discount": 10,
  "validFrom": "2026-08-20T00:00:00Z",
  "validUntil": "2026-09-19T00:00:00Z"
}
```

`FIXED_AMOUNT`는 `discount`에 원 단위 정수 금액을 넣는다. `PERCENTAGE`는 5 이상 50 이하의 정수 퍼센트다.

생성 규칙:

- `targetOfferIds`는 1개 이상 200개 이하여야 한다.
- 모든 대상 Offer는 존재하고 활성 상태여야 한다.
- 판매자는 본인 Seller의 Offer만 선택할 수 있다.
- 관리자는 운영 권한 범위의 Offer를 선택할 수 있다.
- `validFrom < validUntil`이어야 한다.
- 기본 과정의 최대 운영 기간은 30일이다.
- `validFrom`은 현재 시각보다 과거일 수 없다.
- 현재 판매자 쿠폰은 플랫폼 검증·승인 없이 요청 형식·권한·대상 소유권만 확인하고 자동 적용한다.
- 미래 `validFrom`은 `SCHEDULED`, 현재 시각과 같은 `validFrom`은 `ACTIVE`로 생성한다.

## 2. 수정

`PATCH /api/v1/coupons/{couponId}`

### SCHEDULED

- 이름, 대상 Offer, 할인 정책, 기간을 수정할 수 있다.

### ACTIVE

```json
{
  "validUntil": "2026-09-25T00:00:00Z"
}
```

- `validUntil`만 수정할 수 있다.
- `validUntil`은 현재 시각 이후여야 한다.
- 이름, 할인 정책, 대상 Offer는 변경할 수 없다. 새 쿠폰을 생성해야 한다.
- `status`는 PATCH 요청으로 받지 않는다.

## 3. 비활성화

`DELETE /api/v1/coupons/{id}/inactive`

- 쿠폰 소유자 또는 `ADMIN`만 요청할 수 있다.
- `SCHEDULED`, `ACTIVE` 쿠폰만 `INACTIVE`로 전환할 수 있다.
- 이미 `INACTIVE`인 쿠폰은 멱등적으로 `204 No Content`를 반환한다.
- 성공 시 `204 No Content`를 반환한다.
- 비활성화한 쿠폰은 다시 활성화할 수 없다.

## 4. 판매자 쿠폰 목록

`GET /api/v1/coupons?page=0&size=20&sort=createdAt,DESC`

- `ADMIN`은 전체 쿠폰을 조회한다.
- `PRODUCT_MANAGER`는 본인이 생성한 Seller 쿠폰을 상태와 관계없이 모두 조회한다.
- 판매자 소유 범위는 인증된 Seller에서 결정하며 `ownerId`를 요청으로 받지 않는다.
- 페이지 기반 조회를 사용하며 기본 `page=0`, `size=20`, 최대 `size=100`이다.
- 기본 정렬은 `createdAt DESC, couponId DESC`다.
- 기본 조회에는 상태 제한을 적용하지 않는다. 선택적으로 `status`를 전달하면 `SCHEDULED`, `ACTIVE`, `INACTIVE` 중 해당 상태로 필터링한다.

지원 필터:

- `keyword`: 쿠폰명 부분 일치
- `status`: 쿠폰 상태
- `discountType`: `PERCENTAGE` 또는 `FIXED_AMOUNT`
- `targetOfferId`: 특정 판매 상품에 적용된 쿠폰
- `validUntilFrom`, `validUntilTo`: 종료 시각 범위
- `sort`: `createdAt,DESC`, `validUntil,ASC`, `name,ASC`

요청 예시:

`GET /api/v1/coupons?page=0&size=20&keyword=여름&status=ACTIVE&discountType=PERCENTAGE&targetOfferId=uuid-offer-1&validUntilFrom=2026-08-01T00:00:00Z&validUntilTo=2026-08-31T23:59:59Z&sort=validUntil,ASC`

```json
{
  "page": 0,
  "size": 20,
  "keyword": "여름",
  "status": "ACTIVE",
  "discountType": "PERCENTAGE",
  "targetOfferId": "uuid-offer-1",
  "validUntilFrom": "2026-08-01T00:00:00Z",
  "validUntilTo": "2026-08-31T23:59:59Z",
  "sort": "validUntil,ASC"
}
```

목록 항목은 `couponId`, `name`, `status`, `discountType`, `discount`, `validFrom`, `validUntil`을 포함한다.
응답은 공통 페이지 형식의 `data`, `page`, `size`, `totalElements`, `totalPages`를 사용한다. 다음 페이지 존재 여부는 `page + 1 < totalPages`로 판단한다.

응답 예시 `200 OK`:

```json
{
  "data": [
    {
      "couponId": "uuid-coupon-1",
      "name": "여름 시즌 10% 할인",
      "status": "ACTIVE",
      "discountType": "PERCENTAGE",
      "discount": 10,
      "validFrom": "2026-08-01T00:00:00Z",
      "validUntil": "2026-08-31T23:59:59Z"
    },
    {
      "couponId": "uuid-coupon-2",
      "name": "여름 특가 5000원 할인",
      "status": "ACTIVE",
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

## 5. 관리자 규칙

- 관리자는 전체 쿠폰을 조회할 수 있다.
- 관리자는 모든 쿠폰을 운영상 비활성화할 수 있다.
- 관리자 쿠폰도 판매자 쿠폰과 같은 생성·수정 스키마를 사용한다.
