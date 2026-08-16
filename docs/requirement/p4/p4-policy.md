# P4 Coupon Policy

이 문서는 쿠폰 API와 독립적으로 유지되는 P4의 업무 정책을 정의한다. 데이터 모델과 API 계약은 [Coupon API](p4-coupon.md), 행위자별 API는 [Coupon Seller API](p4-coupon-seller.md)와 [Coupon Customer API](p4-coupon-customer.md)를 따른다.

## 1. 범위와 책임

### 범위

- 플랫폼 또는 판매자가 생성하는 기간 한정 쿠폰
- 쿠폰 적용 대상 Offer 선택과 할인 정책
- 쿠폰 상태, 고객 Clip, 주문 적용 기록
- 판매자·관리자 쿠폰 운영과 고객 보유 쿠폰 조회
- 쿠폰 적용 가능 여부와 실제 할인 금액 검증

P4는 Offer의 상품 정보·가격·재고, User·Seller의 인증·원본 상태, Order의 결제·취소·환불 상태를 소유하지 않는다. 해당 정보는 각 도메인의 공개 계약으로 확인한다.

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Coupon·CouponTarget·CouponClip·CouponRedemption 원본·상태 | P4 Coupon | [Coupon API](p4-coupon.md) |
| 인증 User와 Seller 자격 | P1 User·P8 Seller·P11 Auth | [P1 User](../p1/p1-user.md), [P8 Seller](../p8/p8-index.md), [P11 Auth](../p11/p11-index.md) |
| Offer 존재·활성·판매자 소유권·가격·재고 | P9 Offer | [P9 Offer](../p9/p9-index.md) |
| 주문 생성·결제 확정·취소·환불 | P5 Order | [P5 Policy](../p5/p5-policy.md), [Order API](../p5/p5-order.md) |
| 관리자 권한과 플랫폼 운영 진입점 | P7 Admin | [P7 Policy](../p7/p7-policy.md), [P7 Admin API](../p7/p7-admin.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Coupon` | 하나 이상의 Offer에 연결된 할인 캠페인과 상태를 보유하는 기준 리소스 |
| `CouponTarget` | Coupon과 적용 대상 Offer의 연결 리소스 |
| `CouponClip` | 고객이 쿠폰을 저장한 사실과 고객별 사용 상태를 보유하는 리소스 |
| `CouponRedemption` | 주문 상품에 쿠폰 할인이 실제 적용된 사실을 보유하는 리소스 |
| `ADMIN` | 플랫폼 운영 범위의 Offer에 쿠폰을 생성·수정·비활성화할 수 있는 행위자 |
| `PRODUCT_MANAGER` | 본인이 소유한 Seller의 활성 Offer에 쿠폰을 생성·수정·비활성화할 수 있는 행위자 |
| 고객 | 활성 쿠폰을 Clip하고 주문에서 적용할 수 있는 인증 사용자 |

용어의 기준은 [domain-glossary.md](../../domain-glossary.md)를 따른다.

## 3. 핵심 업무 규칙

- `ADMIN`과 `PRODUCT_MANAGER`는 같은 Coupon 스키마와 `/api/v1/coupons` API를 사용한다.
- `ownerType`과 `ownerId`는 요청 본문으로 받지 않는다. `PLATFORM` 쿠폰의 `ownerId`는 관리자 User ID, `SELLER` 쿠폰의 `ownerId`는 Seller ID로 인증 주체에서 결정한다.
- 모든 할인 금액은 KRW 원 단위 정수로 저장한다. 별도 통화 필드는 두지 않는다.
- `PERCENTAGE`의 `discount`는 5 이상 50 이하의 정수 퍼센트다. `FIXED_AMOUNT`의 `discount`는 0보다 큰 원 단위 정수다.
- Coupon은 하나 이상의 Offer를 대상으로 하며 기본 과정에서 최대 200개까지 연결한다.
- 판매자는 본인 Seller가 소유한 활성 Offer만 선택할 수 있다. 관리자는 운영 권한 범위의 활성 Offer를 선택할 수 있다.
- `validFrom < validUntil`이어야 하며 기본 과정의 최대 운영 기간은 30일이다. `validFrom`은 현재 시각보다 과거일 수 없다.
- 기본 과정에서는 판매자 쿠폰의 플랫폼 검증·승인을 수행하지 않고, 요청 형식·권한·Offer 소유권을 확인한 뒤 자동 적용한다. 플랫폼 검증은 [Coupon Advanced](p4-coupon-advanced.md)에서 다룬다.
- 고객은 `ACTIVE` Coupon만 Clip할 수 있다. `(couponId, userId)` 조합은 유일하다.
- 고객 보유 쿠폰 조회는 고객 본인의 `CouponClip.status = CLIPPED`이고 `Coupon.status = ACTIVE`인 항목만 반환한다.
- 기본 과정의 고객별 쿠폰 사용 횟수는 1회다.
- 한 주문에는 서로 다른 Coupon을 최대 5개까지 적용할 수 있다. 하나의 Cart Item에는 Coupon을 최대 1개만 적용한다.
- 퍼센트 Coupon 하나는 한 주문의 최대 5개 대상 상품에, 정액 Coupon 하나는 대상 상품 1개에 적용한다.
- 할인 금액이 대상 상품의 결제 금액보다 크면 쿠폰 적용을 거절한다.

## 4. 불변식과 상태 전이

### 불변식

- Coupon의 `ownerType`과 `ownerId` 조합은 인증 주체와 일치해야 한다.
- 동일 Coupon에 동일 Offer를 중복 연결할 수 없다.
- `(couponId, userId)`는 유일하며 Clip은 고객별로 한 번만 생성한다.
- `CouponRedemption`은 `clipId → CouponClip → Coupon` 관계로 Coupon을 추적하고 `couponId`를 중복 저장하지 않는다.
- 하나의 `OrderItem`에는 확정된 CouponRedemption을 하나만 둔다.
- 동일 주문 요청의 재시도로 CouponRedemption이 중복 생성되지 않아야 한다.
- `INACTIVE`는 종단 상태이며 다시 활성화할 수 없다.
- `ACTIVE` 상태에서는 `validUntil`만 수정할 수 있다. 이름·할인 정책·대상 Offer 변경은 새 Coupon을 생성해야 한다.

### Coupon 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| 없음 | `validFrom > now`인 Coupon 생성 | `SCHEDULED` | Coupon API |
| 없음 | `validFrom <= now`인 Coupon 생성 | `ACTIVE` | Coupon API |
| `SCHEDULED` | `validFrom` 도달 | `ACTIVE` | P4 스케줄러 |
| `SCHEDULED` | 소유자 또는 관리자의 비활성화 | `INACTIVE` | Coupon API |
| `ACTIVE` | `now >= validUntil` | `INACTIVE` | P4 스케줄러 |
| `ACTIVE` | 소유자 또는 관리자의 비활성화 | `INACTIVE` | Coupon API |
| `INACTIVE` | 재활성화 요청 | 전이 없음 | Coupon API |

## 5. 도메인 간 규칙과 예외 소유권

- P9 Offer의 존재·활성·가격·재고·판매자 소유권은 P9의 공개 계약으로 확인한다.
- P1·P8·P11의 인증·User·Seller 자격 예외 원본은 해당 도메인이 소유한다. P4는 필요한 경우 Coupon API의 응답 코드로 변환한다.
- P5는 주문 생성·결제 확정·취소·환불을 소유한다. P4는 쿠폰 적용 조건과 CouponRedemption 기록을 제공한다.
- 주문 취소·환불 시 CouponRedemption 복원 기준은 P5가 정의한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P9 Offer | Offer 존재·활성·가격·재고·소유권 확인 | [P9 Offer](../p9/p9-index.md) |
| P1 User·P8 Seller·P11 Auth | 인증 주체와 User·Seller 자격 확인 | [P1 User](../p1/p1-user.md), [P8 Seller](../p8/p8-index.md), [P11 Auth](../p11/p11-index.md) |
| P5 Order | 주문 상품·결제 확정·취소·환불 처리 | [P5 Policy](../p5/p5-policy.md), [Order API](../p5/p5-order.md) |
| P7 Admin | 관리자 운영 권한과 진입점 | [P7 Policy](../p7/p7-policy.md), [P7 Admin API](../p7/p7-admin.md) |

## 6. API 문서와의 관계

- 공통 모델과 Coupon 단위 API는 [Coupon API](p4-coupon.md)에서 정의한다.
- 판매자·관리자 행위자별 API는 [Coupon Seller API](p4-coupon-seller.md)에서 정의한다.
- 고객 Clip·보유 쿠폰·주문 적용 API는 [Coupon Customer API](p4-coupon-customer.md)에서 정의한다.
- 플랫폼 검증·예산·대상 고객군·중복 적용·통계는 [Coupon Advanced](p4-coupon-advanced.md)에서 심화사항으로 관리한다.
- 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따르고, Coupon별 `exceptionCode`는 [Coupon API](p4-coupon.md)에 등록한다.
