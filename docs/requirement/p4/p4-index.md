# P4 Coupon 문서 안내

P4는 상품에 연결된 쿠폰의 정책, 데이터 모델, 생성·조회·Clip·주문 적용 API를 정의한다. 정책은 [P4 Coupon Policy](p4-policy.md), 데이터 모델과 공통 API는 [Coupon API](p4-coupon.md)를 기준으로 한다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P4 Coupon Policy](p4-policy.md) | 정책 | 범위·책임, 행위자, 업무 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [Coupon API](p4-coupon.md) | 리소스 API | Coupon·CouponTarget·CouponClip·CouponRedemption 모델, 관계·제약, 공통 API |
| [Coupon Seller API](p4-coupon-seller.md) | 행위자별 API | 판매자·관리자 생성, 수정, 비활성화, 쿠폰 목록 조회 |
| [Coupon Customer API](p4-coupon-customer.md) | 행위자별 API | 고객 Clip, 보유 쿠폰 조회, 주문 적용 |
| [Coupon Advanced](p4-coupon-advanced.md) | 심화 정책 | Amazon 비교, 플랫폼 검증, 예산, 고객군, 중복 적용, 통계 |
행위자와 상태에 따라 요청·성공 응답·예외가 달라지는 API는 판매자·고객 보조 문서에서 정의한다. 해당 문서는 [Coupon API](p4-coupon.md)의 모델과 [P4 Coupon Policy](p4-policy.md)를 중복 정의하지 않는다.

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| Coupon·CouponTarget·CouponClip·CouponRedemption 원본·상태 | P4 Coupon | [Coupon API](p4-coupon.md), [P4 Coupon Policy](p4-policy.md) |
| 인증 주체와 User·Seller 식별자 | P1 User·P8 Seller·P11 Auth | [P1 User](../p1/p1-user.md), [P8 Seller](../p8/p8-index.md), [P11 Auth](../p11/p11-index.md) |
| 대상 Offer 상태·소유권·가격·재고 | P9 Offer | [P9 Offer](../p9/p9-index.md) |
| 주문 금액·결제·취소·환불 상태 | P5 Order | [P5 Policy](../p5/p5-policy.md), [Order API](../p5/p5-order.md) |
| 관리자 운영 진입점 | P7 Admin | [P7 Policy](../p7/p7-policy.md), [P7 Admin API](../p7/p7-admin.md) |

- P4는 외부 도메인의 내부 모델·Repository·서비스 구현을 소유하지 않는다.
- 외부 도메인의 식별자와 공개 계약만 참조하며, Offer·User·Seller 필드를 P4 모델에 복제하지 않는다.
- 공통 URI, 성공 응답 원칙, 예외 응답 필드, 페이지네이션, 인증은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
- 다른 도메인의 예외는 P4 API 응답에 포함할 수 있지만, 원본 코드·메시지의 소유권은 해당 도메인을 참조한다.

## 3. 문서 작성 순서

1. [P4 Coupon Policy](p4-policy.md)에서 범위·책임과 확정 업무 규칙을 정한다.
2. [Coupon API](p4-coupon.md)에서 정책을 만족하는 데이터 모델과 공통 API를 정의한다.
3. [Coupon Seller API](p4-coupon-seller.md)와 [Coupon Customer API](p4-coupon-customer.md)에서 행위자별 API 계약을 완성한다.
4. [Coupon Advanced](p4-coupon-advanced.md)에서 기본 과정에 포함하지 않는 정책과 Amazon 비교를 관리한다.
5. 정책과 API가 충돌하면 Policy를 기준으로 API 문서를 수정한다.

## 4. 작성 원칙

- 이 문서는 안내와 책임 경계만 작성하고, 정책·필드·API 계약을 중복해서 작성하지 않는다.
- 정책은 [P4 Coupon Policy](p4-policy.md)에만 작성하고 API 문서에는 필요한 참조만 남긴다.
- 데이터 모델과 공통 API는 [Coupon API](p4-coupon.md)가 소유한다.
- 구현 결과가 달라질 수 있는 선택 표현 대신 하나의 기본 동작을 확정한다.
