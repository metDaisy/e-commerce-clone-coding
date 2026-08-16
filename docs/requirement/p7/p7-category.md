# P7 Category Administration (Category 관리)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

P2 Catalog가 Category의 정식 데이터와 검증을 소유하며, P7은 관리자의 Category 생성·수정 진입점을 제공한다. 판매자는 별도의 Category 등록 요청 또는 CatalogProduct 등록 요청의 `categoryProposal`로 새 카테고리를 제안할 수 있지만 직접 생성할 수는 없다.

## 1. 카테고리 생성

`POST /api/v1/admin/categories`

요청:

```json
{
  "name": "노트북",
  "parentId": "uuid"
}
```

- `name`은 공백만으로 구성할 수 없다.
- `parentId`가 없으면 루트 카테고리로 생성하고 `depth = 1`로 저장한다.
- `parentId`가 있으면 부모 카테고리가 존재해야 하며 `depth = parent.depth + 1`로 저장한다.
- 부모 카테고리는 하나만 지정할 수 있으며, 서버는 부모 연결을 검증해 순환 참조가 생기지 않도록 한다.
- `depth`는 `1~3`만 허용한다.
- 생성 전 검증은 P2가 공개한 `CatalogCategoryValidationApi`를 호출한다. P7은 자체 검증 규칙을 만들지 않는다.

성공 응답 `201`:

```json
{
  "categoryId": "uuid",
  "name": "노트북",
  "parentId": "uuid",
  "depth": 3,
  "createdAt": "2026-08-09T12:00:00Z"
}
```

## 2. 카테고리 수정

`PATCH /api/v1/admin/categories/{categoryId}`

요청:

```json
{
  "name": "노트북·태블릿",
  "parentId": "uuid"
}
```

- 전달된 필드만 수정하고 전달되지 않은 필드는 유지한다.
- `parentId: null`은 루트 카테고리로 이동한다. `parentId`를 생략하면 기존 부모를 유지한다.
- `parentId` 변경 시 자기 자신이나 자신의 하위 카테고리를 부모로 지정할 수 없다.
- 서버는 변경 후 부모를 따라가며 순환 참조가 발생하지 않는지 검증한다.
- 수정 후 하위 카테고리 전체의 `depth`가 `1~3`을 벗어나면 요청을 거부한다.
- 카테고리와 하위 카테고리의 `parentId`, `depth` 변경은 하나의 트랜잭션으로 처리한다.

성공 응답 `200`:

```json
{
  "categoryId": "uuid",
  "name": "노트북·태블릿",
  "parentId": "uuid",
  "depth": 3,
  "updatedAt": "2026-08-09T12:05:00Z"
}
```
