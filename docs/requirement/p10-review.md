# P10 Review (리뷰)

공통 응답 봉투와 HTTP 상태 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 책임

Review는 구매·배송 완료를 확인한 고객이 ProductVariant에 남기는 평가다. Review 본문과 리뷰 Media는 Review가 소유하며, 상품 메타데이터와 상품용 Media는 [P2 Catalog](p2-catalog.md)가 소유한다.

- 배송 완료(`DELIVERED`)된 구매자만 리뷰를 작성할 수 있다.
- 사용자당 하나의 ProductVariant에 하나의 Review만 작성할 수 있다.
- 작성자는 자신의 Review를 수정·삭제할 수 있다.
- `ADMIN`은 리뷰를 관리 삭제할 수 있다.
- 고객용 상품 상세·검색은 Review 요약을 읽지만 Review 내부 정보 전체를 직접 노출하지 않는다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/product/{catalogProductId}/reviews` | 공개 | CatalogProduct 하위 Review 목록 |
| POST | `/api/v1/product-variants/{variantId}/reviews` | 로그인 구매자 | Review 작성 |
| PATCH | `/api/v1/reviews/{reviewId}` | 작성자 | 본인 Review 수정 |
| DELETE | `/api/v1/reviews/{reviewId}` | 작성자 | 본인 Review 보관 |
| DELETE | `/api/v1/admin/reviews/{reviewId}` | `ADMIN` | Review 관리 삭제 |

## 3. Review 목록

`GET /api/v1/product/{catalogProductId}/reviews`

Review 목록은 `createdAt DESC, reviewId DESC` 순서의 커서 기반 조회이며, 해당 CatalogProduct의 모든 ProductVariant Review를 통합한다.

응답 `200`:

```json
{
  "summary": { "averageRating": 4.5, "reviewCount": 120 },
  "data": [
    {
      "reviewId": "uuid",
      "variantId": "uuid",
      "userName": "홍**",
      "rating": 5,
      "content": "좋은 상품입니다.",
      "images": [
        { "mediaId": "uuid", "url": "https://cdn.example.com/review.jpg", "sortOrder": 0 }
      ],
      "createdAt": "2026-08-09T12:00:00Z"
    }
  ],
  "nextCursor": "opaque-cursor",
  "hasNext": true
}
```

`cursor`와 `page`를 함께 보내면 `400 PAGINATION_PARAMETER_CONFLICT`를 반환한다. 고객에게는 작성자의 표시 이름만 마스킹해 반환하며 내부 User ID는 노출하지 않는다.

## 4. Review 작성·수정·보관

`POST /api/v1/product-variants/{variantId}/reviews` 요청:

```json
{
  "rating": 5,
  "content": "좋은 상품입니다.",
  "imageUrls": ["https://cdn.example.com/review.jpg"]
}
```

성공 응답 `201`:

```json
{
  "reviewId": "uuid",
  "variantId": "uuid",
  "rating": 5,
  "content": "좋은 상품입니다.",
  "images": [
    { "mediaId": "uuid", "url": "https://cdn.example.com/review.jpg", "sortOrder": 0 }
  ],
  "createdAt": "2026-08-09T12:00:00Z"
}
```

- 평점은 1~5 정수, 본문은 최대 2000자, 이미지는 최대 5장이다.
- 구매·배송 완료 자격과 중복 Review를 하나의 트랜잭션에서 검증한다.
- Review 삭제는 물리 삭제가 아닌 보관 처리이며 고객 목록에서 제외한다.

`PATCH /api/v1/reviews/{reviewId}` 요청:

```json
{
  "rating": 4,
  "content": "수정된 리뷰입니다.",
  "imageUrls": []
}
```

성공 응답은 `reviewId`, `variantId`, `rating`, `content`, `images`, `updatedAt`을 반환한다. 작성자가 아닌 사용자는 `403 REVIEW_ACCESS_DENIED`다.

`DELETE /api/v1/admin/reviews/{reviewId}`는 `ADMIN`만 호출할 수 있으며, 성공 응답은 다음과 같다.

```json
{
  "reviewId": "uuid",
  "status": "ARCHIVED",
  "archivedAt": "2026-08-10T12:00:00Z"
}
```

## 5. Review Media

Review Media의 URL·정렬 순서·보관 상태는 Review가 관리한다. 실제 파일 업로드·CDN 저장은 공통 Media Storage port와 infra adapter가 담당하며, CatalogProduct·ProductVariant Media의 권한과 수명주기는 P2에서 관리한다.

## 6. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 평점·본문·이미지 검증 실패 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `REVIEW_NOT_ELIGIBLE` | 구매·배송 완료 조건 미충족 |
| 403 | `REVIEW_ACCESS_DENIED` | 다른 사용자의 Review 수정·삭제 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | CatalogProduct 없음 |
| 404 | `VARIANT_NOT_FOUND` | ProductVariant 없음 |
| 404 | `REVIEW_NOT_FOUND` | Review 없음 |
| 409 | `REVIEW_ALREADY_EXISTS` | 동일 User의 ProductVariant Review 중복 |
