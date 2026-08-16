# P10 Review API

이 문서는 고객이 사용하는 Review·ReviewMediaAttachment 데이터 모델을 정의하고, 해당 모델을 조회·등록·수정하는 API를 서술한다. 리뷰 정책은 [P10 Policy](p10-policy.md), 숨김 요청·관리자 심사 API는 [P10 Review Moderation API](p10-review-moderation.md)에 정의한다.

성공 응답은 각 API의 Response DTO를 직접 반환한다. 예외 응답은 [공통 API 계약](../index.md#공통-api-계약)의 `exceptionCode`, `message`, 선택적 `details` 형식을 사용한다. 요청 추적 ID는 `X-Request-Id` 응답 헤더로 제공한다.

## 1. 대상 데이터 모델과 API 관계

| 데이터 모델 | 이 문서에서 다루는 내용 | 관련 API |
|---|---|---|
| `Review` | Offer에 대한 평점·본문·공개 상태·작성 시각 | 목록 조회, 등록, 수정 |
| `ReviewMediaAttachment` | Review에 연결된 Media URL과 노출 순서 | 목록 조회, 등록, 수정 |

- 목록 조회 API는 `Review`와 연결된 `ReviewMediaAttachment`를 고객용 표현으로 반환한다.
- 등록 API는 `Review`를 생성하고 요청받은 `READY` Media를 `ReviewMediaAttachment`로 연결한다.
- 수정 API는 `Review`의 평점·본문과 첨부 연결을 변경한다.
- 숨김 요청·관리자 심사 API는 [P10 Review Moderation API](p10-review-moderation.md)에서 정의한다.
- 리뷰 작성·공개·숨김의 업무 정책은 [P10 Policy](p10-policy.md)를 따른다.

## 2. 데이터 모델

### 2-1. Review

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `reviewId` | UUID | 예 | Review 식별자 |
| `userId` | UUID | 예 | 작성자 식별자 |
| `offerId` | UUID | 예 | 평가 대상 Offer 식별자 |
| `orderItemId` | UUID | 예 | 작성 자격을 확인한 구매 항목 식별자 |
| `rating` | INTEGER | 예 | 1~5 정수 평점 |
| `content` | VARCHAR(2000) | 예 | 판매자·상품 평가 본문, 최대 2,000자 |
| `visibilityStatus` | ENUM | 예 | `VISIBLE`, `HIDDEN` |
| `createdAt` | TIMESTAMP | 예 | 작성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 본문·평점·첨부 변경 시각 |

### 2-2. ReviewMediaAttachment

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `mediaId` | UUID | 예 | P12 Media 식별자 |
| `reviewId` | UUID | 예 | 연결된 Review 식별자 |
| `url` | VARCHAR(2048) | 예 | 고객에게 제공할 CDN URL |
| `sortOrder` | INTEGER | 예 | 노출 순서, 0 이상 |
| `createdAt` | TIMESTAMP | 예 | 연결 시각 |

첨부는 Review당 최대 5개이며, 업로드·저장은 [P12 Media](../p12/p12-media.md)의 `READY` Media만 연결한다. 첨부에는 `updatedAt`을 두지 않는다.

### 2-3. 관계와 제약

- `(userId, offerId)`는 유일해야 한다.
- `orderItemId`의 구매자와 Offer가 각각 Review의 `userId`, `offerId`와 일치해야 한다.
- Review 생성 시 `DELIVERED` 자격과 중복 여부를 같은 트랜잭션에서 확인한다.
- 평균 평점과 리뷰 수는 `VISIBLE` Review를 기준으로 P10이 계산해 제공한다. `Offer`에 `averageRating`, `reviewCount` 필드를 저장하지 않는다.

## 3. Review 목록 조회

### 요청

`GET /api/v1/offers/{offerId}/reviews?page=0&size=20`

쿼리 파라미터 `page`는 0부터 시작하고 `size`는 1~20이다.

### 성공 응답: `200 OK`

```json
{
  "summary": {
    "averageRating": 4.5,
    "reviewCount": 120
  },
  "data": [
    {
      "reviewId": "uuid",
      "userName": "구매자*",
      "rating": 5,
      "content": "상품과 판매자 모두 만족스럽습니다.",
      "images": [
        {
          "mediaId": "uuid",
          "url": "https://cdn.example.com/review.jpg",
          "sortOrder": 0
        }
      ],
      "createdAt": "2026-08-09T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}
```

`summary`는 동일한 Offer의 공개 Review만 집계한다. 여러 Offer의 요약이 필요한 상품 상세 화면은 P9가 P10의 Offer별 조회 결과를 조합한다.

### 예외

| HTTP | exceptionCode | client message | system message | details |
|---:|---|---|---|---|
| 400 | `REVIEW-001` | 페이지 번호 또는 페이지 크기(1 이상 20 이하)가 올바르지 않습니다. | Review 목록 페이지 파라미터 검증 실패: `page={page}`, `size={size}` | `{"page":"0 이상 정수","size":"1~20 정수"}` |
| 404 | [OFFER-001](../p9/p9-offer.md) | — | — | — |
| 500 | [SYSTEM-001](../index.md#예외-응답) | — | — | — |
| 503 | `REVIEW-019` | 리뷰 서비스를 일시적으로 사용할 수 없습니다. | Review 목록 조회 의존 서비스 이용 불가: `offerId={offerId}`, `dependency={dependency}` | 없음 |

`client message`와 `details`는 클라이언트 응답에 포함한다. `system message`는 서버 로그용이며 클라이언트에 반환하지 않는다. `system message`에는 요청으로 전달된 실제 값과 내부 원인을 기록할 수 있다.

## 4. Review 등록

### 요청

`POST /api/v1/order-items/{orderItemId}/reviews`

```json
{
  "rating": 5,
  "content": "상품과 판매자 모두 만족스럽습니다.",
  "mediaUploadIds": ["uuid"]
}
```

### 성공 응답: `201 Created`

```json
{
  "reviewId": "uuid",
  "rating": 5,
  "content": "상품과 판매자 모두 만족스럽습니다.",
  "images": [
    {
      "mediaId": "uuid",
      "url": "https://cdn.example.com/review.jpg",
      "sortOrder": 0
    }
  ],
  "visibilityStatus": "VISIBLE",
  "createdAt": "2026-08-09T12:00:00Z",
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

서버는 `orderItemId`로 작성자, Offer, 배송 상태를 확인하고 `offerId`를 결정한다. 요청 본문으로 `offerId`, `catalogProductId`, `variantId`를 받지 않는다.

### 예외

이 표는 Review 등록 API에서 클라이언트가 받을 수 있는 모든 예외를 정의한다. `ORDER-001`은 P5 Order가 소유한 예외이므로 [P5 Order](../p5/p5-order.md)의 정의를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `REVIEW-014` | 평점·본문·첨부 정보 검증 실패 | 평점, 본문 또는 첨부 정보가 올바르지 않습니다. | 실패한 필드와 허용 조건 | Review 등록 입력값 검증 실패: `orderItemId={orderItemId}` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `REVIEW-003` | 배송 완료된 구매 항목이 아니거나 구매자가 아님 | 배송 완료된 구매 항목만 리뷰를 작성할 수 있습니다. | 없음 | Review 작성 자격 없음: `userId={userId}, orderItemId={orderItemId}, deliveryStatus={deliveryStatus}` |
| 404 | [ORDER-001](../p5/p5-order.md) | — | — | — | — |
| 409 | `REVIEW-004` | 동일 User·Offer Review가 이미 존재함 | 해당 상품에는 이미 리뷰를 작성했습니다. | 없음 | Review 중복: `userId={userId}, offerId={offerId}` |
| 503 | [MEDIA-009](../p12/p12-media.md) | — | — | — | — |

## 5. Review 수정

### 요청

`PATCH /api/v1/reviews/{reviewId}`

```json
{
  "rating": 4,
  "content": "수정한 리뷰 내용입니다.",
  "mediaUploadIds": []
}
```

### 성공 응답: `200 OK`

```json
{
  "reviewId": "uuid",
  "rating": 4,
  "content": "수정한 리뷰 내용입니다.",
  "images": [],
  "visibilityStatus": "VISIBLE",
  "createdAt": "2026-08-09T12:00:00Z",
  "updatedAt": "2026-08-11T09:30:00Z"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `REVIEW-015` | 평점·본문·첨부 정보 검증 실패 | 평점, 본문 또는 첨부 정보가 올바르지 않습니다. | 실패한 필드와 허용 조건 | Review 수정 입력값 검증 실패: `reviewId={reviewId}` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `REVIEW-005` | 요청자가 Review 작성자가 아님 | 본인이 작성한 리뷰만 수정할 수 있습니다. | 없음 | Review 수정 권한 없음: `userId={userId}, reviewId={reviewId}` |
| 404 | `REVIEW-006` | Review가 존재하지 않음 | 리뷰를 찾을 수 없습니다. | 없음 | Review 조회 실패: `reviewId={reviewId}` |
| 409 | `REVIEW-007` | Review가 `HIDDEN` 상태임 | 숨김 처리된 리뷰는 수정할 수 없습니다. | 없음 | 숨김 Review 수정 시도: `reviewId={reviewId}, visibilityStatus={visibilityStatus}` |
| 503 | [MEDIA-009](../p12/p12-media.md) | — | — | — | — |
