# P4 Coupon API

이 문서는 `Coupon`과 하위 리소스의 데이터 모델 및 API를 정의한다. 업무 정책은 [P4 Coupon Policy](p4-policy.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Coupon` | 쿠폰명·할인 정책·소유자·기간·상태 | 생성·상세 조회·수정·비활성화 |
| `CouponTarget` | Coupon과 적용 Offer의 연결 | 생성·수정 시 함께 관리 |
| `CouponClip` | 고객의 쿠폰 저장과 사용 상태 | Clip·고객 보유 쿠폰 조회 |
| `CouponRedemption` | 주문 상품에 실제 적용된 할인 기록 | 주문 결제 확정·취소·환불 연계 |

- P4는 네 모델의 원본과 관계를 소유한다.
- `offerId`, `userId`, `orderId`, `orderItemId`는 외부 도메인의 식별자만 저장하며 외부 필드를 복제하지 않는다.
- 정책 불변식의 원본은 [P4 Coupon Policy](p4-policy.md)이며 API는 해당 불변식을 검증한다.

## 2. 데이터 모델

### 2-1. `Coupon`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `couponId` | UUID | 예 | 쿠폰 식별자 |
| `ownerType` | Enum | 예 | `PLATFORM` 또는 `SELLER`. 요청 본문으로 받지 않는다. |
| `ownerId` | UUID | 예 | `PLATFORM`이면 관리자 User ID, `SELLER`이면 Seller ID |
| `name` | String | 예 | 고객에게 표시할 쿠폰명 |
| `discountType` | Enum | 예 | `PERCENTAGE` 또는 `FIXED_AMOUNT` |
| `discount` | Integer | 예 | 퍼센트 정수 또는 KRW 원 단위 정수 |
| `status` | Enum | 예 | `SCHEDULED`, `ACTIVE`, `INACTIVE` |
| `validFrom` | Instant | 예 | 적용 시작 시각 |
| `validUntil` | Instant | 예 | 적용 종료 시각 |
| `createdAt` | Instant | 예 | 생성 시각 |
| `updatedAt` | Instant | 예 | 수정 시각 |

### 2-2. `CouponTarget`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `couponTargetId` | UUID | 예 | 연결 식별자 |
| `couponId` | UUID | 예 | Coupon 식별자 |
| `offerId` | UUID | 예 | 적용 대상 P9 Offer 식별자 |
| `createdAt` | Instant | 예 | 연결 시각 |

### 2-3. `CouponClip`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `clipId` | UUID | 예 | Clip 식별자 |
| `couponId` | UUID | 예 | Coupon 식별자 |
| `userId` | UUID | 예 | 고객 User 식별자 |
| `status` | Enum | 예 | `CLIPPED`, `REDEEMED`, `EXPIRED`, `REVOKED` |
| `createdAt` | Instant | 예 | 고객이 쿠폰을 저장한 시각 |
| `updatedAt` | Instant | 예 | 상태 변경 시각 |

### 2-4. `CouponRedemption`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `redemptionId` | UUID | 예 | 적용 기록 식별자 |
| `clipId` | UUID | 예 | 고객 Clip 식별자 |
| `orderId` | UUID | 예 | P5 Order 식별자 |
| `orderItemId` | UUID | 예 | P5 OrderItem 식별자 |
| `appliedDiscount` | Integer | 예 | 실제 적용 할인 금액(KRW) |
| `status` | Enum | 예 | `CONFIRMED`, `REVERSED` |
| `redeemedAt` | Instant | 예 | 쿠폰이 주문에 실제 적용된 시각 |
| `reversedAt` | Instant | 조건부 | 적용 취소 시각 |

## 3. 관계와 제약

- Coupon은 하나 이상의 CouponTarget을 가지며 기본 과정에서 최대 200개 Offer를 연결한다.
- `(couponId, offerId)`와 `(couponId, userId)`는 각각 UNIQUE다.
- `CouponRedemption`은 `clipId → CouponClip → Coupon` 관계로 Coupon을 추적하므로 `couponId`를 저장하지 않는다.
- `redeemedAt`은 Clip 생성 시각이나 주문 생성 시각이 아니라 할인 적용 시각이다.
- 하나의 OrderItem에는 `CONFIRMED` 상태의 CouponRedemption을 하나만 둔다.
- 동일 주문 요청의 재시도에도 CouponRedemption이 중복 생성되지 않아야 한다.
- 주문 취소·환불에 따른 `REVERSED` 전환과 복원 시점은 P5 정책을 따른다.

## 4. API 정의

성공 응답은 공통 성공 봉투 없이 도메인 Response DTO를 직접 반환한다. 목록 API는 공통 페이지 필드 `data`, `page`, `size`, `totalElements`, `totalPages`를 사용하며 `hasNext`를 사용하지 않는다.

### 4-1. Coupon 상세 조회

`GET /api/v1/coupons/{couponId}`

권한: Coupon 소유자 또는 `ADMIN`

#### 성공 응답: `200 OK`

```json
{
  "couponId": "uuid-coupon-1",
  "ownerType": "SELLER",
  "ownerId": "uuid-seller-1",
  "name": "여름 시즌 10% 할인",
  "discountType": "PERCENTAGE",
  "discount": 10,
  "status": "ACTIVE",
  "validFrom": "2026-08-01T00:00:00Z",
  "validUntil": "2026-08-31T23:59:59Z",
  "targetOfferIds": ["uuid-offer-1", "uuid-offer-2"],
  "createdAt": "2026-07-25T12:00:00Z",
  "updatedAt": "2026-07-25T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `COUPON-001` | 소유자 또는 관리자가 아님 | 쿠폰에 접근할 수 없습니다. | 없음 | 소유권 검증 원인과 Coupon 식별자 |
| 404 | `COUPON-002` | Coupon이 없음 | 쿠폰을 찾을 수 없습니다. | 없음 | Coupon 식별자 |

### 4-2. Coupon 생성

`POST /api/v1/coupons`

권한: `ADMIN` 또는 활성 Seller를 가진 `PRODUCT_MANAGER`

요청:

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

`ownerType`, `ownerId`, `sellerId`, `status`는 요청 본문으로 받지 않는다.

#### 성공 응답: `201 Created`

```json
{
  "couponId": "uuid-coupon-1",
  "ownerType": "SELLER",
  "ownerId": "uuid-seller-1",
  "name": "신규 상품 10% 할인",
  "targetOfferIds": ["uuid-offer-1", "uuid-offer-2"],
  "discountType": "PERCENTAGE",
  "discount": 10,
  "status": "SCHEDULED",
  "validFrom": "2026-08-20T00:00:00Z",
  "validUntil": "2026-09-19T00:00:00Z",
  "createdAt": "2026-08-15T12:00:00Z",
  "updatedAt": "2026-08-15T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `COUPON-003` | 필수 필드·UUID·기간 형식 오류 | 요청 값을 확인해 주세요. | 실패 필드와 수정 방법 | 입력값과 검증 원인 |
| 400 | `COUPON-004` | 할인 타입과 `discount` 조합 또는 범위 오류 | 할인 정책을 확인해 주세요. | `discountType`, 허용 범위 | 할인 검증 원인 |
| 400 | `COUPON-005` | 기간이 역전되거나 최대 30일 초과 | 쿠폰 기간을 확인해 주세요. | `validFrom`, `validUntil`, 허용 기간 | 기간 검증 원인 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | `COUPON-011` | 관리 범위 밖 Offer 선택 | 쿠폰 적용 권한이 없습니다. | 거부된 Offer ID | 소유권·운영 범위 검증 |
| 404 | `COUPON-006` | 대상 Offer가 없음 | 적용 상품을 찾을 수 없습니다. | 대상 Offer ID | Offer 조회 원인 |
| 409 | `COUPON-007` | Offer 비활성·보관·소유권 불일치 | 적용할 수 없는 상품이 포함되어 있습니다. | 대상 Offer와 사유 | Offer 상태·소유권 |

### 4-3. Coupon 수정

`PATCH /api/v1/coupons/{couponId}`

권한: Coupon 소유자 또는 `ADMIN`

`SCHEDULED`는 이름·대상 Offer·할인 정책·기간을 수정할 수 있다. `ACTIVE`는 `validUntil`만 수정할 수 있다. `status`는 요청으로 받지 않는다.

요청 예시:

```json
{
  "validUntil": "2026-09-25T00:00:00Z"
}
```

#### 성공 응답: `200 OK`

```json
{
  "couponId": "uuid-coupon-1",
  "ownerType": "SELLER",
  "ownerId": "uuid-seller-1",
  "name": "여름 시즌 10% 할인",
  "discountType": "PERCENTAGE",
  "discount": 10,
  "status": "ACTIVE",
  "validFrom": "2026-08-01T00:00:00Z",
  "validUntil": "2026-09-25T00:00:00Z",
  "updatedAt": "2026-08-15T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `COUPON-003` | 수정 필드 형식 오류 | 요청 값을 확인해 주세요. | 실패 필드와 수정 방법 | 입력값과 검증 원인 |
| 400 | `COUPON-005` | `validUntil`이 현재 시각 이하이거나 기간 초과 | 쿠폰 기간을 확인해 주세요. | 현재 시각과 요청 기간 | 기간 검증 원인 |
| 403 | `COUPON-001` | 소유자 또는 관리자가 아님 | 쿠폰에 접근할 수 없습니다. | 없음 | 소유권 검증 원인과 Coupon 식별자 |
| 404 | `COUPON-002` | Coupon이 없음 | 쿠폰을 찾을 수 없습니다. | 없음 | Coupon 식별자 |
| 409 | `COUPON-008` | ACTIVE에서 `validUntil` 이외 필드 수정 | 활성 쿠폰은 해당 항목을 수정할 수 없습니다. | 수정 불가 필드 | 상태별 수정 검증 |

### 4-4. Coupon 비활성화

`DELETE /api/v1/coupons/{id}/inactive`

권한: Coupon 소유자 또는 `ADMIN`

#### 성공 응답: `204 No Content`

`SCHEDULED` 또는 `ACTIVE`를 `INACTIVE`로 전환한다. 이미 `INACTIVE`인 Coupon도 같은 응답을 반환하는 멱등 API다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `COUPON-001` | 소유자 또는 관리자가 아님 | 쿠폰에 접근할 수 없습니다. | 없음 | 소유권 검증 원인과 Coupon 식별자 |
| 404 | `COUPON-002` | Coupon이 없음 | 쿠폰을 찾을 수 없습니다. | 없음 | Coupon 식별자 |

### 4-5. 판매자·관리자 쿠폰 목록

`GET /api/v1/coupons`

권한: `ADMIN` 또는 활성 Seller를 가진 `PRODUCT_MANAGER`

판매자 목록의 필터·요청 JSON·성공 응답은 [Coupon Seller API](p4-coupon-seller.md)의 `4. 판매자 쿠폰 목록`을 따른다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `COUPON-003` | 페이지·필터·정렬 형식 오류 | 조회 조건을 확인해 주세요. | 실패 파라미터와 허용 값 | 목록 파라미터 검증 원인 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](../p8/p8-seller-profile.md) | — | — | — | — |

### 4-6. 고객 보유 쿠폰 목록

`GET /api/v1/me/coupons`

권한: 로그인 고객

고객 목록의 정렬·요청 JSON·성공 응답은 [Coupon Customer API](p4-coupon-customer.md)의 `2. 고객 보유 쿠폰 조회`를 따른다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `COUPON-003` | 페이지·정렬 형식 오류 | 조회 조건을 확인해 주세요. | 실패 파라미터와 허용 값 | 목록 파라미터 검증 원인 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |

### 4-7. Coupon Clip

`POST /api/v1/coupons/{couponId}/clip`

권한: 로그인 고객

요청 본문은 없다. 성공 응답과 Clip 규칙은 [Coupon Customer API](p4-coupon-customer.md)의 `1. 쿠폰 Clip`을 따른다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 404 | `COUPON-002` | Coupon이 없음 | 쿠폰을 찾을 수 없습니다. | 없음 | Coupon 식별자 |
| 409 | `COUPON-009` | 동일 고객이 이미 Clip함 | 이미 저장한 쿠폰입니다. | `couponId` | 중복 Clip 검증 |
| 409 | `COUPON-010` | Coupon이 `ACTIVE`가 아님 | 현재 저장할 수 없는 쿠폰입니다. | Coupon 상태 | 상태 검증 원인 |

### 4-8. Coupon 통계 (심화)

`GET /api/v1/coupons/{couponId}/statistics`

권한: Coupon 소유자 또는 `ADMIN`

통계 지표와 성공 응답은 [Coupon Advanced](p4-coupon-advanced.md)에 정의한다. 기본 과정에서는 통계 데이터를 제공하지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `COUPON-001` | 소유자 또는 관리자가 아님 | 쿠폰 통계에 접근할 수 없습니다. | 없음 | 소유권 검증 원인과 Coupon 식별자 |
| 404 | `COUPON-002` | Coupon이 없음 | 쿠폰을 찾을 수 없습니다. | 없음 | Coupon 식별자 |
