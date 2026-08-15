# P2 CatalogProduct (카탈로그 상품)

[P2 Catalog 개요](p2-catalog.md)의 CatalogProduct 메타데이터와 전시 정보에 대한 상세 요구사항이다.

## 1. 모델과 책임

CatalogProduct는 여러 ProductVariant가 공유하는 상품군의 공통 메타데이터다.

- `name`, `description`, `brand`, 외부 식별자, 동적 `attributes`, Media를 소유한다.
- Category 하나와 연결한다. Category 계층 경로와 하위 Category 검색은 [Category](p2-category.md) 규칙을 따른다.
- 정식 생성·수정·보관은 `ADMIN`만 수행한다. 관리자 소유자나 `managerId`를 저장하지 않는다.
- ProductVariant·Offer·Inventory는 생성 시 함께 만들지 않는다.
- Offer·Inventory 규칙은 P9, Seller 등록 요청은 P8, 관리자 진입점은 P7이 소유한다.

## 2. 등록

```http
POST /api/v1/admin/catalog-products
```

요청 예시:

```json
{
  "categoryId": "uuid-graphics-card",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "attributes": { "connectionType": "BLUETOOTH" },
  "gtin": "8801234567890"
}
```

- `ADMIN`만 호출할 수 있다.
- `categoryId`는 하나여야 하며 해당 Category가 존재해야 한다.
- `name`, `description`은 공백만으로 구성할 수 없다.
- `asin`, `gtin`, `upc`, `ean`, `isbn` 중 최소 하나를 입력해야 한다.
- CatalogProduct 생성 시 ProductVariant·Offer·Inventory·Media는 생성하지 않는다.
- CatalogProduct, 외부 식별자, Category 연결은 하나의 트랜잭션으로 생성한다.

입력된 외부 식별자에는 다음 규칙을 적용한다.

- `gtin`, `upc`, `ean`: 형식·체크디지트·CatalogProduct 간 유일성 검사
- `asin`: 10자리 영문 대문자·숫자 형식·CatalogProduct 간 유일성 검사
- `isbn`: ISBN-10/ISBN-13 형식·체크디지트·유일성 및 외부 도서 API 검사
- 외부 API는 ISBN 검증에만 사용한다.

식별자 검증에 실패하면 `message`는 안전한 공통 안내로 유지하되, 사용자가 수정할 수 있도록 응답 `details.fields`에 실패한 필드별 정보를 포함한다. 여러 식별자가 동시에 잘못된 경우 모든 실패 필드를 한 번에 반환한다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "상품 식별자 입력을 확인해 주세요.",
    "details": {
      "fields": [
        {
          "field": "gtin",
          "reason": "invalid_check_digit",
          "message": "GTIN 체크디지트가 올바르지 않습니다."
        },
        {
          "field": "isbn",
          "reason": "external_verification_failed",
          "message": "ISBN 정보를 확인할 수 없습니다."
        }
      ]
    }
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

`details.fields`에는 필드명, 수정 가능한 원인 코드, 사용자 안내 메시지만 포함한다. 입력한 전체 식별자 값, 내부 검증 로직, 외부 API 응답 원문은 포함하지 않으며 서버 로그의 `logDetails`에만 기록한다.

## 3. 동적 attributes

`CatalogProduct.attributes`는 카테고리별 고정 스키마가 없는 동적 JSON object다.

- 최상위 값은 객체여야 하며 공통 크기·깊이 제한만 적용한다.
- 키의 의미·자료형·필수 여부·카테고리 적합성은 P2가 제한하지 않는다.
- 여러 Variant가 공유하는 상품군 정보에 사용한다.
- `weight`, `dimensions` 같은 Variant별 정보는 ProductVariant.attributes에 저장한다.
- 새 Category를 제안할 때 attributes 스키마를 함께 제출하지 않는다.

`PATCH`의 `attributes`는 JSON Merge Patch 방식으로 처리한다.

| 요청 | 처리 |
|---|---|
| 일반 값 | 기존 값 수정 또는 새 키 추가 |
| `null` | 해당 키 삭제 |
| 키 생략 | 기존 값 유지 |
| `{}` | 변경 없음 |

중첩 객체에도 같은 규칙을 재귀적으로 적용한다. `null`을 실제 값으로 저장하는 것은 지원하지 않는다.

## 4. 수정·식별자 수정

```http
PATCH /api/v1/admin/catalog-products/{catalogProductId}
PATCH /api/v1/admin/catalog-products/{catalogProductId}/identifiers
```

- `ADMIN`만 호출할 수 있다.
- 수정 가능한 메타데이터는 `name`, `description`, `brand`, `attributes`다.
- 기본 범위에서는 Category 연결 변경을 일반 수정에 포함하지 않는다.
- 외부 식별자 수정은 전달된 값만 변경하며 식별자 삭제는 지원하지 않는다.
- 보관된 CatalogProduct는 일반 수정·식별자 수정 대상이 아니다.
- Category·Variant·Offer·Inventory·publicationStatus를 일반 CatalogProduct 수정 본문으로 받지 않는다.

## 5. Media

상품 Media attachment는 P2가 대상 검증, 정렬, 대표 이미지, 보관 규칙을 소유한다. 실제 파일 저장·CDN·스토리지 삭제는 공통 `MediaStoragePort`에 위임한다.

```http
POST   /api/v1/admin/catalog-products/{catalogProductId}/media
PATCH  /api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}
DELETE /api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}
```

- `ADMIN`만 호출할 수 있다.
- `isPrimary = true`인 Media는 상품당 최대 하나다.
- `sortOrder`는 상품 내에서 유일해야 한다.
- DELETE는 물리 삭제가 아니라 Media 보관이며 보관된 Media는 공개 조회에서 제외한다.
- Review Media는 P10이 소유한다.

## 6. 보관과 조회

```http
POST /api/v1/admin/catalog-products/{catalogProductId}/archive
GET  /api/v1/catalog-products/{catalogProductId}
GET  /api/v1/admin/catalog-products/{catalogProductId}
```

- 보관은 `publicationStatus = ARCHIVED`, `archivedAt = 현재 시각`으로 변경하며 물리 삭제하지 않는다.
- 보관된 CatalogProduct의 하위 ProductVariant·Offer·Inventory와 CatalogProduct Media도 물리 삭제하지 않는다.
- 하위 ProductVariant는 공개·Seller 조회와 Offer 등록 대상에서 제외한다.
- 하위 Offer는 P9 규칙에 따라 비활성화한다.
- Seller·구매자 조회에서 보관되거나 존재하지 않는 CatalogProduct는 `404 CATALOG_PRODUCT_NOT_FOUND`다.
- 관리자 조회는 존재하는 CatalogProduct라면 `ACTIVE`·`ARCHIVED` 모두 `200`으로 반환한다.
- 관리자 조회에서 실제로 존재하지 않는 CatalogProduct만 `404 CATALOG_PRODUCT_NOT_FOUND`다.
- Seller·구매자 응답에는 내부 `catalogProductId`, `variantId`를 반환하지 않고 상품 표시 메타데이터만 반환한다. 관리자 응답에는 운영에 필요한 ID와 상태를 포함한다.

## 7. 예외

| HTTP | 코드 | 클라이언트 메시지 | 서버 상세 원인 |
|---:|---|---|---|
| 400 | `VALIDATION_ERROR` | 상품 정보를 확인해 주세요. | `name`, `description`, `brand`, `attributes`의 필드별 검증 결과 |
| 400 | `PRODUCT_CODE_ERROR` | 상품 식별자 입력을 확인해 주세요. | `asin`, `gtin`, `upc`, `ean`, `isbn`별 형식·체크디지트·중복·외부 검증 결과 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요합니다. | 인증 정보 없음·위조·만료 |
| 403 | `ACCESS_DENIED` | 이 작업을 수행할 권한이 없습니다. | ADMIN 권한이 아닌 CatalogProduct 변경 요청 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리를 찾을 수 없습니다. | 요청한 `categoryId`가 `NOT_FOUND` |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | 상품을 찾을 수 없습니다. | CatalogProduct가 `NOT_FOUND` 또는 `ARCHIVED` |
| 404 | `MEDIA_NOT_FOUND` | 이미지를 찾을 수 없습니다. | CatalogProduct Media가 `NOT_FOUND` 또는 `ARCHIVED` |
| 409 | `CATALOG_PRODUCT_ARCHIVED` | 보관된 상품은 변경할 수 없습니다. | `publicationStatus = ARCHIVED` 상태에서 수정·식별자 수정 시도 |
| 500 | `INTERNAL_SERVER_ERROR` | 요청을 처리하지 못했습니다. | 저장소·외부 ISBN 검증·예상하지 못한 서버 오류 |

식별자 검증 실패는 `message`를 추상적으로 유지하되, 응답 `details.fields`에 문제가 있는 식별자 필드와 `invalid_format`, `invalid_check_digit`, `duplicate`, `external_verification_failed` 등의 수정 가능한 원인을 포함한다. 서버는 `getDetailMessage()`에 실제 검증 결과와 내부 식별자를 기록하며, 외부 API 응답 원문·SQL·내부 상태는 클라이언트에 반환하지 않는다.
