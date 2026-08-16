# P10 Review 문서 안내

P10은 배송이 완료된 구매자가 Offer에 리뷰를 작성하고, 고객이 공개 리뷰와 평점 요약을 조회하는 기능을 정의한다. 리뷰 본문에는 판매자와 상품에 대한 평가를 함께 작성할 수 있다. CatalogProduct와 ProductVariant는 별도의 리뷰 대상이 아니다.

## 문서 목록

| 문서 | 역할 | 주요 내용 |
|---|---|---|
| [P10 Policy](p10-policy.md) | 리뷰 정책 | 범위·책임, 작성 자격, 중복·수정, 주문 취소·환불·반품 이후 처리, 공개·숨김, 평균 집계, 판매자 요청과 관리자 심사 정책 |
| [P10 Review](p10-review.md) | Review 데이터 모델·고객 API | Review·ReviewMediaAttachment 모델, 고객용 목록·등록·수정 API, 요청·응답 JSON, 성공 상태와 예외 |
| [P10 Review Moderation](p10-review-moderation.md) | Review 숨김·심사 API | ReviewModeration 모델, 판매자 숨김 요청, 관리자 승인·거절 API, 요청·응답 JSON, 성공 상태와 예외 |

## 책임과 경계

- P10은 Review 원본, 첨부 연결, 공개 여부, Offer별 공개 리뷰 요약의 기준을 소유한다.
- P5 Order·Delivery는 구매자·Offer 일치 여부와 `DELIVERED` 도달 여부를 제공한다.
- P9 Offer는 Offer 식별자와 판매자 소유 관계를 제공한다. 평균 평점·리뷰 수를 Offer 테이블에 저장하지 않는다.
- P7 Admin은 정책 문서에 정의된 관리자 심사 API의 진입점이다.
- P12 Media는 업로드와 저장을 담당하고, P10은 리뷰 첨부의 연결·순서·노출만 관리한다.

공통 URI, 인증, 페이지네이션, HTTP 상태와 오류 응답 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. 성공 응답은 각 API의 Response DTO를 직접 반환하고, 예외 응답만 공통 오류 DTO를 사용한다.
