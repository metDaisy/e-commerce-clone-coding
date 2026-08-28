# P7 CatalogProduct API

이 문서는 P2가 소유한 CatalogProduct·ProductVariant의 관리자 목록 API를 정의한다. Catalog 정책은 [P7 Catalog Administration Policy](p7-catalog.md), 공통 권한은 [P7 Admin API](p7-admin.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `CatalogProduct` | P2가 CatalogProduct 원본·공개 상태를 소유한다. | 관리자 목록 조회 |
| `ProductVariant` | P2가 CatalogProduct 하위 Variant 원본·공개 상태를 소유한다. | 관리자 목록 조회 |
| `Category` | P2가 Category 경로와 분류를 소유한다. | 목록 필터·응답 |

- 이 문서는 생성·수정 API를 제공하지 않는다. 판매자의 생성 요청은 [P7 Catalog Request API](p7-catalog-requests.md)에서 심사한다.
- Offer 가격·재고는 P9가 소유하며 이 목록에 상세 필드로 포함하지 않는다.

## 2. 데이터 모델

### 2-1. 관리자 목록 표현

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `catalogProductId` | UUID | 예 | CatalogProduct 식별자 |
| `name` | VARCHAR(255) | 예 | 상품명 |
| `description` | VARCHAR(4000) | 예 | 상품 설명 |
| `brand` | VARCHAR(255) | 아니오 | 브랜드 |
| `categoryPath` | 배열 | 예 | Category ID·이름 경로 |
| `publicationStatus` | ENUM | 예 | `ACTIVE`, `ARCHIVED` |
| `archivedAt` | TIMESTAMP | 아니오 | 보관 시각 |
| `createdAt` | TIMESTAMP | 예 | 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 수정 시각 |
| `variants` | 배열 | 예 | 하위 ProductVariant 관리자 표현 |

ProductVariant 표현은 `variantId`, `displayName`, `attributes`, `publicationStatus`, `archivedAt`, `createdAt`, `updatedAt`을 포함한다. `managerId`는 저장하거나 반환하지 않는다.

### 2-2. 관계와 제약

- `categoryId` 필터는 선택한 Category와 하위 Category를 포함한다.
- 기본 조회는 `ACTIVE` CatalogProduct와 `ACTIVE` ProductVariant만 반환한다.
- 보관 상태 필터를 지정하면 부모·자식의 현재 상태를 각각 표시한다.
- 목록은 관리자 운영 조회와 Product Manager의 Offer 등록 대상 검색을 함께 담당하며 고객용 상품 검색이나 상세 API를 대체하지 않는다.

## 3. API 정의

### 3-1. CatalogProduct·ProductVariant 관리자 목록

`GET /api/v1/catalog-products`

권한: `ADMIN` 또는 `PRODUCT_MANAGER` 권한과 `ACTIVE Seller` 상태를 가진 사용자.

Query:

```text
page=0
size=20
keyword=headphone
categoryId=uuid
catalogPublicationStatus=ACTIVE|ARCHIVED
variantPublicationStatus=ACTIVE|ARCHIVED
tag=office
sort=LATEST|NAME_ASC|NAME_DESC
```

`page`는 0부터 시작하고 `size`는 기본 20, 최대 100이다. `keyword`는 이름·설명·브랜드·상품 식별자·Variant 표시명을 검색한다.

#### 성공 응답: `200 OK`

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "data": [
    {
      "catalogProductId": "uuid",
      "name": "무선 헤드폰",
      "description": "카탈로그 상품 설명",
      "brand": "Example Brand",
      "categoryPath": [
        { "categoryId": "uuid", "name": "전자기기" },
        { "categoryId": "uuid", "name": "음향기기" }
      ],
      "publicationStatus": "ACTIVE",
      "archivedAt": null,
      "createdAt": "2026-08-16T12:00:00Z",
      "updatedAt": "2026-08-16T12:05:00Z",
      "variants": [
        {
          "variantId": "uuid",
          "displayName": "블랙",
          "attributes": { "color": "Black" },
          "publicationStatus": "ACTIVE",
          "archivedAt": null,
          "createdAt": "2026-08-16T12:01:00Z",
          "updatedAt": "2026-08-16T12:01:00Z"
        }
      ]
    }
  ]
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. CatalogProduct·ProductVariant 원본 예외는 [P2 Catalog](../p2/p2-catalog.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `ADMIN-004` | page·size·sort 값이 유효하지 않음 | 목록 조회 조건을 확인해 주세요. | 실패 query | 입력값과 검증 원인 |
| 404 | [CATEGORY-003](../p2/p2-category.md) | — | — | — | — |
