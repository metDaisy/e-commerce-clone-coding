# P7 Catalog Registration Review (Catalog 등록 요청 심사)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

P8의 `CatalogRegistrationRequest`에는 Category·CatalogProduct·ProductVariant 등록 요청이 저장된다. P7은 이 요청의 관리자 목록·승인·거절을 담당하고, 정식 Catalog 데이터 생성은 P2의 공개 application interface를 호출해 처리한다.

## 1. 요청 목록

`GET /api/v1/admin/catalog-registration-requests`

- `status`, `requestType`, `sellerId` 필터와 페이지네이션을 지원한다.
- `requestType`은 `CATEGORY`, `CATALOG_PRODUCT`, `PRODUCT_VARIANT`를 지원한다.
- 기본 정렬은 `createdAt DESC, requestId DESC`다.
- 등록 요청 레코드의 `sellerId`와 `requestedByUserId`는 클라이언트 요청 본문에서 받지 않는다. P8이 인증된 `userId`와 그 User에 연결된 활성 Seller를 기준으로 서버에서 설정한다.
- 관리자 목록과 상세에는 `sellerId`·판매자 표시명·사업자명 및 `requestedByUserId`·요청자 이름을 함께 표시한다. `sellerId`는 판매 주체, `requestedByUserId`는 실제 제출 행위자다.
- 요청 상세 원본과 저장 필드는 [P8 CatalogRegistrationRequest 모델](../p8/p8-seller.md#catalogregistrationrequest)을 따른다.

## 2. 요청 승인

`POST /api/v1/admin/catalog-registration-requests/{requestId}/approve`

- 요청 본문은 없다. 승인 자체에 별도 사유를 요구하지 않는다.
- `PENDING` 요청만 승인할 수 있다.
- `requestType=CATEGORY`이면 P2의 Category 생성 application interface를 호출한다.
- `requestType=CATALOG_PRODUCT`이면 P2의 CatalogProduct 생성 application interface를 호출한다.
- `requestType=PRODUCT_VARIANT`이면 `targetCatalogProductId`에 연결된 ProductVariant 생성 application interface를 호출한다.
- ProductVariant 승인 시 기존 CatalogProduct를 수정하거나 새 CatalogProduct를 생성하지 않는다.
- 승인 시 생성 결과 ID를 요청 레코드에 기록한다: `createdCategoryId`, `createdCatalogProductId`, `createdVariantId`.
- CatalogProduct·ProductVariant 승인만으로 판매자 Offer·Inventory를 생성하지 않는다.
- 이미 승인된 동일 요청을 다시 승인할 수 없다.

## 3. 요청 거절

`POST /api/v1/admin/catalog-registration-requests/{requestId}/reject`

```json
{
  "reasonCode": "DUPLICATE_CATALOG_ENTRY",
  "message": "동일한 CatalogProduct 또는 ProductVariant가 이미 존재합니다."
}
```

- `PENDING` 요청만 거절할 수 있다.
- `reasonCode`와 판매자에게 공개할 `message`는 필수다.

| `reasonCode` | 설명 |
|---|---|
| `CATALOG_DATA_INVALID` | 상품·Variant 정보, 식별자 또는 속성 값이 유효하지 않음 |
| `DUPLICATE_CATALOG_ENTRY` | 동일한 Category·CatalogProduct·ProductVariant가 이미 존재함 |
| `CATEGORY_NOT_APPROVED` | Category가 존재하지 않거나 제안된 Category가 승인되지 않음 |

- 거절 시 요청 상태를 `REJECTED`로 변경하고 처리 관리자와 처리 시각을 기록한다.
- 승인·거절은 한 번만 처리하며, 처리 후 요청 내용을 수정하지 않는다.
- 거절된 요청은 수정하지 않고 새 요청으로 다시 제출한다.

