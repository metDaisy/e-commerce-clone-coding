# P7 CatalogProduct Administration (CatalogProduct 관리 목록)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

P2가 소유한 CatalogProduct·ProductVariant를 관리자 화면에서 조회하기 위한 P7 관리자 전용 진입점이다. 일반 사용자의 상품 탐색에는 사용하지 않으며, 상품 검색은 P9의 `GET /api/v1/product/search`에서 제공한다.

## 1. CatalogProduct 관리 목록

`GET /api/v1/admin/catalog-products`

지원 Query:

```text
page=0
size=20
keyword=headphone (상품명·설명·브랜드·식별자·Variant 표시명)
categoryId=uuid
catalogPublicationStatus=ACTIVE|ARCHIVED
variantPublicationStatus=ACTIVE|ARCHIVED
sort=LATEST|NAME_ASC|NAME_DESC
```

- `page`는 0부터 시작하고 기본 `size`는 20, 최대 `size`는 100이다.
- 두 상태 필터를 생략하면 `ACTIVE` CatalogProduct와 `ACTIVE` ProductVariant만 조회한다.
- `catalogPublicationStatus=ARCHIVED`를 지정하면 보관된 CatalogProduct를 조회할 수 있다. 이때 하위 ProductVariant는 자체 상태와 관계없이 관리 화면의 관계 확인을 위해 함께 반환하고 각 상태를 표시한다.
- `variantPublicationStatus=ARCHIVED`를 지정하면 보관된 ProductVariant를 조회할 수 있다. 부모 CatalogProduct가 ACTIVE인지 ARCHIVED인지와 관계없이 관리자 결과에 포함한다.
- `ADMIN`은 모든 CatalogProduct와 ProductVariant를 조회할 수 있으며, 보관된 항목도 상태 필터로 선택한다.
- 기본 정렬은 `LATEST`이며 `createdAt DESC, catalogProductId DESC`를 사용한다.
- `categoryId`는 하위 카테고리를 포함하며, CatalogProduct가 해당 Category와 연결된 경우 결과에 포함한다.
- 이 목록은 판매자용 목록과 달리 보관 상태·관리 메타데이터를 포함한다. `managerId`는 저장하거나 반환하지 않는다.

응답 `200`:

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
        { "categoryId": "uuid", "name": "컴퓨터" },
        { "categoryId": "uuid", "name": "그래픽카드" }
      ],
      "publicationStatus": "ACTIVE",
      "archivedAt": null,
      "createdAt": "2026-08-09T12:00:00Z",
      "updatedAt": "2026-08-09T12:05:00Z",
      "variants": [
        {
          "variantId": "uuid",
          "displayName": "블랙",
          "attributes": {
            "color": "Black",
            "weight": { "value": 250, "unit": "g" },
            "packageSize": { "width": 18, "height": 20, "depth": 8, "unit": "cm" }
          },
          "publicationStatus": "ACTIVE",
          "archivedAt": null,
          "createdAt": "2026-08-09T12:01:00Z",
          "updatedAt": "2026-08-09T12:01:00Z"
        }
      ]
    }
  ]
}
```

관리자 목록은 CatalogProduct·ProductVariant의 관리 상태를 확인하는 조회이며 Offer 가격·재고의 상세는 [P7 Offer 운영](p7-offer.md)과 P9의 Offer·Inventory 관리 API에서 정의한다.

