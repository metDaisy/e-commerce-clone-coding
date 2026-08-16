# P10 Review 정책

이 문서는 Review의 작성·수정·공개 정책과 판매자 숨김 요청 및 관리자 심사 정책을 정의한다. P10 문서의 전체 구성은 [P10 Index](p10-index.md)를 참고한다.

## 1. 범위와 책임

### 범위

- 구매·배송 완료 여부에 따른 Review 작성 자격
- Review의 작성·수정·중복·삭제 정책
- Review와 첨부의 공개·숨김 정책
- Offer별 공개 Review 평점 요약 기준
- 판매자 숨김 요청과 관리자 승인·거절 정책

Review 데이터 모델과 고객용 목록·등록·수정 API의 상세 계약은 [P10 Review API](p10-review.md)를 따른다. 공통 URI, 인증, 페이지네이션, HTTP 상태와 오류 응답 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

### 책임

| 책임 | 담당 | 참조 문서 |
|---|---|---|
| 구매자·Offer·배송 완료 여부 확인 | P5 Order·Delivery | [P5 Index](../p5/p5-index.md) |
| Offer와 판매자 소유 관계 확인 | P9 Offer | [P9 Index](../p9/p9-index.md), [P9 Offer](../p9/p9-offer.md) |
| Review 원본·첨부 연결·공개 여부·평점 요약 | P10 Review | [P10 Review API](p10-review.md) |
| ReviewModeration 원본과 숨김·심사 상태 | P10 Review Moderation | [P10 Review Moderation API](p10-review-moderation.md) |
| 판매자 숨김 요청 접수와 관리자 심사 진입점 | P10 정책·P7 Admin | [P7 Admin](../p7/p7-admin.md) |
| Media 업로드·저장·CDN URL 제공 | P12 Media | [P12 Index](../p12/p12-index.md), [P12 Media](../p12/p12-media.md) |

P10은 P5·P9·P12의 내부 구현을 직접 소유하지 않는다. 각 도메인이 제공하는 공개 계약을 통해 자격·소유 관계·Media 상태를 확인한다.

## 2. 리뷰 대상과 작성 자격

- Review의 평가 대상은 Offer다.
- 하나의 Review 본문에는 판매자와 상품에 대한 평가를 함께 작성할 수 있다.
- CatalogProduct와 ProductVariant를 별도의 리뷰 대상으로 만들지 않는다.
- `DELIVERED`에 도달한 `OrderItem`의 구매자만 해당 Offer에 Review를 작성할 수 있다.
- 배송 완료 전에 주문이 취소된 `OrderItem`은 Review 작성 자격이 없다.
- Review 작성 후 주문이 취소·환불·반품되어도 이미 작성된 Review를 자동으로 숨기지 않는다.

## 3. 작성·수정·삭제 정책

- 동일한 사용자는 동일한 Offer에 Review를 하나만 작성할 수 있다. 숨김 상태도 중복 기준에 포함한다.
- 작성자는 자신의 Review를 수정할 수 있다.
- 숨김 처리된 Review는 수정할 수 없다.
- 고객용 Review 삭제 API와 숨김 해제 API는 제공하지 않는다.
- Review와 첨부 원본은 숨김 처리만으로 삭제하지 않는다.

## 4. 공개와 평점 요약

- Review 생성 시 공개 상태는 `VISIBLE`이다.
- 고객 목록에는 `VISIBLE` Review만 노출한다.
- `HIDDEN` Review는 고객 목록과 평점 요약에서 제외한다.
- P10은 Offer별 `averageRating`과 `reviewCount`의 기준을 소유한다.
- 두 값은 `VISIBLE` Review의 `rating`을 기준으로 계산하며, 현재 `Offer`에 집계 필드를 저장하지 않는다.
- 상품 상세에서 여러 Offer의 리뷰를 통합해야 하는 경우 P9가 P10의 Offer별 요약을 조합한다.
- Offer가 비활성·판매 종료되어도 Review의 `visibilityStatus`는 자동 변경하지 않는다. Offer 노출 여부와 Review 공개 여부는 별도로 판단한다.

## 5. 판매자 요청과 관리자 심사 정책

- 판매자는 자신의 Offer에 연결된 Review에 대해서만 숨김을 요청할 수 있다.
- 숨김 요청 사유는 필수이며 최대 2,000자다.
- 판매자 요청만으로 Review를 숨기지 않고, 관리자 승인 전까지 `VISIBLE` 상태를 유지한다.
- 하나의 Review에는 `PENDING` 심사 요청을 하나만 둘 수 있다.
- `REJECTED` 요청은 다시 요청할 수 있다.
- 이미 `HIDDEN`인 Review에는 새 숨김 요청을 만들 수 없다.
- 승인 시 `ReviewModeration.status = APPROVED`와 `Review.visibilityStatus = HIDDEN`을 같은 트랜잭션에서 변경한다.
- 승인된 `ReviewModeration`의 심사자·심사 시각·요청 사유가 숨김 처리 정보의 원본이다.
- 거절 시 Review는 `VISIBLE` 상태를 유지한다.
- 처리된 요청은 다시 승인·거절할 수 없다.

ReviewModeration 데이터 모델과 숨김 요청·관리자 승인·거절 API의 상세 계약은 [P10 Review Moderation API](p10-review-moderation.md)를 따른다.

## 6. Review API 요구사항

- Review 목록은 페이지 기반이며 `createdAt DESC, reviewId DESC`로 정렬한다.
- `page`는 0부터 시작하고 `size` 기본값은 20, 최대값은 20이다.
- 고객 목록에는 `ACTIVE` Offer의 `VISIBLE` Review만 포함한다.
