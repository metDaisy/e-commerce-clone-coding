# P4 Index (Coupon)

P4는 상품 구매 시 적용할 수 있는 쿠폰과 고객의 쿠폰 보유·사용 상태를 정의한다. 쿠폰은 플랫폼 또는 판매자 주체가 발행할 수 있으며, 주문 생성 시 적용 가능 여부와 할인 금액을 검증한다.

## 1. 범위

- 쿠폰 기본 정책과 적용 대상
- 고객의 쿠폰 Clip·조회·주문 적용
- 판매자·관리자의 쿠폰 생성·수정·비활성화
- Coupon, CouponTarget, CouponClip, CouponRedemption 모델과 상태 전이
- 여러 Offer에 대한 쿠폰 적용 및 최대 5개 제한

## 2. 문서 구성

- [P4 Coupon](p4-coupon.md): P4 범위, 공통 API, 공통 예외
- [P4 Coupon Customer](p4-coupon-customer.md): 고객 보유 쿠폰과 주문 적용
- [P4 Coupon Model](p4-coupon-model.md): 쿠폰 데이터 모델과 상태
- [P4 Coupon Seller](p4-coupon-seller.md): 판매자·관리자 쿠폰 운영
- [P4 Coupon Advanced](p4-coupon-advanced.md): 정책 비교와 향후 심화사항

## 3. 도메인 경계

| 도메인 | P4와의 관계 |
|---|---|
| P5 Order | 주문 요청의 쿠폰 적용을 검증하고 할인 금액을 계산 |
| P7 Admin | 플랫폼 쿠폰과 운영 정책 관리 |
| P8 Seller | 판매자 쿠폰 발행 요청 및 소유권 |
| P9 Offer | 쿠폰 적용 대상인 Offer 확인 |

공통 응답 형식과 HTTP 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
