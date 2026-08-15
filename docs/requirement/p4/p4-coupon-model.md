# P4 Coupon Model (쿠폰 데이터 모델·상태)

[P4 쿠폰 개요](p4-coupon.md)의 공통 규칙을 따른다.

## 1. Coupon

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `couponId` | UUID | O | 쿠폰 식별자 |
| `ownerType` | Enum | O | `PLATFORM` 또는 `SELLER` |
| `ownerId` | UUID | O | 관리자 User 또는 Seller 식별자 |
| `name` | String | O | 고객 표시 쿠폰명 |
| `discountType` | Enum | O | `PERCENTAGE` 또는 `FIXED_AMOUNT` |
| `discount` | Integer | O | 할인율 또는 원화 할인 금액 |
| `status` | Enum | O | `SCHEDULED`, `ACTIVE`, `INACTIVE` |
| `validFrom` | Instant | O | 시작 시각 |
| `validUntil` | Instant | O | 종료 시각 |
| `createdAt` | Instant | O | 생성 시각 |
| `updatedAt` | Instant | O | 수정 시각 |

- `PERCENTAGE`의 `discount`는 5 이상 50 이하의 정수 퍼센트다.
- `FIXED_AMOUNT`의 `discount`는 0보다 큰 원 단위 정수다.
- 플랫폼·판매자 쿠폰의 스키마를 분리하지 않는다.
- 적용 상품 연결은 `CouponTarget`으로 분리한다.

## 2. CouponTarget

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `couponTargetId` | UUID | O | 연결 식별자 |
| `couponId` | UUID | O | 쿠폰 식별자 |
| `offerId` | UUID | O | 적용 대상 Offer |
| `createdAt` | Instant | O | 연결 시각 |

- 쿠폰 하나는 하나 이상의 Offer를 대상으로 한다.
- 기본 과정에서는 쿠폰 하나에 최대 200개 Offer를 연결한다.
- 판매자 쿠폰은 해당 Seller 소유의 활성 Offer만 대상으로 한다.
- 관리자 쿠폰은 운영 권한 범위의 활성 Offer를 대상으로 한다.
- 동일 쿠폰에 동일 Offer를 중복 연결할 수 없다.

## 3. CouponClip

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `clipId` | UUID | O | Clip 식별자 |
| `couponId` | UUID | O | 쿠폰 식별자 |
| `userId` | UUID | O | 고객 식별자 |
| `status` | Enum | O | `CLIPPED`, `REDEEMED`, `EXPIRED`, `REVOKED` |
| `createdAt` | Instant | O | Clip 레코드 생성 시각 |
| `updatedAt` | Instant | O | 수정 시각 |

- `(couponId, userId)`는 유일해야 한다.
- `createdAt`은 고객이 쿠폰을 저장한 시각이다.
- 기본 과정의 고객별 최대 사용 횟수는 1회다.

## 4. CouponRedemption

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `redemptionId` | UUID | O | 적용 기록 식별자 |
| `clipId` | UUID | O | 고객 Clip 식별자 |
| `orderId` | UUID | O | 주문 식별자 |
| `orderItemId` | UUID | O | 주문 상품 식별자 |
| `appliedDiscount` | Integer | O | 실제 적용 할인 금액(KRW) |
| `status` | Enum | O | `CONFIRMED`, `REVERSED` |
| `redeemedAt` | Instant | O | 쿠폰이 주문에 실제 적용된 시각 |
| `reversedAt` | Instant | 조건부 | 적용 취소 시각 |

- `clipId → CouponClip → Coupon` 관계로 쿠폰을 추적하므로 `couponId`를 중복 저장하지 않는다.
- `redeemedAt`은 Clip 시각이나 주문 생성 시각이 아니라 쿠폰 적용 시각이다.
- 주문 재시도로 동일 할인 기록을 중복 생성하지 않는다.
- 하나의 `OrderItem`에는 `CONFIRMED` 상태의 CouponRedemption을 하나만 유지한다. 동일 상품에 여러 쿠폰을 중첩하지 않는다.
- 하나의 Order에는 서로 다른 `UserCoupon`을 최대 5개까지 적용할 수 있다. 하나의 쿠폰이 여러 OrderItem에 적용되면 OrderItem별 CouponRedemption을 생성한다.
- 주문 취소·환불에 따른 적용 취소 기준은 P5에서 정의한다.

## 5. 상태 전이

| 상태 | 의미 |
|---|---|
| `SCHEDULED` | `validFrom` 도달 전 |
| `ACTIVE` | 고객 Clip 및 주문 적용 가능 |
| `INACTIVE` | 비활성화 또는 기간 종료 |

```text
SCHEDULED → ACTIVE → INACTIVE
SCHEDULED → INACTIVE
```

- 미래 `validFrom`은 `SCHEDULED`, 현재 시각과 같은 `validFrom`은 `ACTIVE`로 저장한다.
- 시스템은 `validFrom`에 `SCHEDULED`를 `ACTIVE`로 전환한다.
- 시스템은 `now >= validUntil`인 `ACTIVE` 쿠폰을 `INACTIVE`로 전환한다.
- `INACTIVE`는 종단 상태이며 다시 활성화할 수 없다.
