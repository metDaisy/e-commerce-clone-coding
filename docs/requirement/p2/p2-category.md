# P2 Category (카테고리)

[P2 Catalog 개요](p2-catalog.md)에서 정의한 CatalogProduct 분류 메타데이터의 상세 요구사항이다.

## 1. 범위와 모델

Category는 상품을 분류하는 공용 메타데이터다. 카테고리는 계층을 가지지만 상품 연결은 다대다다.

```text
Category 1 : N CatalogProduct
Category 1 : N Category (parent · childCategories)
```

- 하나의 CatalogProduct는 대표 분류 Category 하나에 연결된다. 이 Category는 일반적으로 최하위(leaf) Category다.
- 하나의 Category는 여러 CatalogProduct에 연결될 수 있다.
- 연결은 `CatalogProduct.categoryId` 단일 FK로 저장한다. 별도의 `catalog_product_categories` 관계 테이블은 사용하지 않는다.
- Category는 `parentId`와 `depth`로 계층을 표현한다. 루트는 `parentId = null`, `depth = 1`이며 최대 깊이는 3이다.
- CatalogProduct에 연결된 Category의 `parentId`를 반복 조회하면 전체 경로를 얻는다. 예를 들어 `그래픽카드` → `컴퓨터` → `전자기기`를 `전자기기 > 컴퓨터 > 그래픽카드`로 표시할 수 있다.
- 기본 범위에서는 Category 삭제·아카이빙을 제공하지 않는다.

## 2. 생성과 수정

Category의 HTTP 관리자 진입점은 P7이 소유한다.

```http
POST  /api/v1/admin/categories
PATCH /api/v1/admin/categories/{categoryId}
GET   /api/v1/categories
```

- `ADMIN`만 생성·수정할 수 있다.
- `name`은 공백만으로 구성할 수 없고, 동일 부모 아래에서 중복될 수 없다.
- `parentId`가 있으면 부모가 존재해야 하며 `depth = parent.depth + 1`이다.
- 자기 자신이나 자신의 하위 Category를 부모로 지정할 수 없다.
- 수정으로 인해 하위 Category의 깊이가 3을 초과하면 거부한다.
- P7은 P2의 검증 application interface를 호출하고 자체 검증 규칙을 만들지 않는다.

## 3. CatalogProduct 연결

CatalogProduct 등록·수정 요청은 단일 `categoryId`를 사용한다.

```json
{
  "categoryId": "uuid-graphics-card"
}
```

- 등록 시 `categoryId`는 반드시 하나여야 한다.
- `categoryId`가 가리키는 Category가 존재해야 하며, 존재하지 않으면 `404 CATEGORY_NOT_FOUND`다.
- 상품 조회에서 Category를 선택하면 P2는 해당 Category와 모든 하위 Category를 계산해 연결된 CatalogProduct를 검색한다. 따라서 상위 Category인 `컴퓨터`로 검색하면 `그래픽카드`에 연결된 상품도 포함한다.
- Category 연결 변경은 기본 범위에서 지원하지 않는다. 변경이 필요하면 관리자용 별도 연결 명령으로 처리한다.
- Seller가 기존 Category를 지정할 수 있고, 없는 분류는 단일 `categoryProposal`로 제안할 수 있다. 제안은 Category를 즉시 생성하지 않는다.
- 상품 검색에서 `categoryId`를 지정하면 해당 Category와 모든 하위 Category에 연결된 CatalogProduct를 검색한다.

## 4. 공개 application interface

P7·P8·P9는 P2의 domain·infra 내부 패키지를 직접 참조하지 않는다.

```text
CatalogCategoryValidationApi.validate(CategoryProposal)
  → CategoryValidationResult(valid, depth, errors)

CatalogCategoryQueryApi.findSelfAndDescendantIds(categoryId)
  → Set<CategoryId>
```

- 검증 interface는 Category를 생성하거나 이름을 예약하지 않는다.
- P8의 Seller 제안 제출 시와 P7의 관리자 승인·생성 시 동일한 검증을 다시 수행한다.
- P9는 상품 검색 시 하위 Category ID를 받아 P2의 Category 테이블·Repository를 직접 조회하지 않는다.
- HTTP 검증 API를 별도로 공개하지 않는다.

## 5. 공개 조회와 예외

`GET /api/v1/categories`는 전체 Category 트리를 반환한다. 구매자·Seller는 Category를 조회할 수 있지만 생성·수정할 수 없다.

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `CATEGORY_NAME_INVALID` | 이름이 비어 있거나 형식이 잘못됨 |
| 400 | `INVALID_CATEGORY_PARENT` | 부모가 없거나 자기 자신·하위 Category를 부모로 지정함 |
| 400 | `CATEGORY_CYCLE_DETECTED` | 부모 연결에서 순환이 발생함 |
| 400 | `CATEGORY_DEPTH_EXCEEDED` | Category 깊이가 3을 초과함 |
| 404 | `CATEGORY_NOT_FOUND` | Category가 존재하지 않음 |
| 409 | `CATEGORY_NAME_DUPLICATE` | 동일 부모 아래 이름이 중복됨 |
