# P7 Category API

이 문서는 P2 Category를 관리자 화면에서 생성·수정하는 API를 정의한다. Category 정책은 [P7 Catalog Administration Policy](p7-catalog.md), 공통 관리자 권한은 [P7 Admin API](p7-admin.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Category` | P2가 계층 구조와 공개 상태를 소유한다. | 관리자 생성·수정 |
| `CatalogRegistrationRequest` | P8이 판매자의 Category 제안 원본을 소유한다. | [P7 Catalog Request API](p7-catalog-requests.md) |

P7은 `Category` 원본을 소유하지 않고 P2의 공개 Category application interface를 호출한다.

## 2. 데이터 모델

### 2-1. Category

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `categoryId` | UUID | 예 | Category 식별자 |
| `name` | VARCHAR(255) | 예 | 공백이 아닌 Category 이름 |
| `parentId` | UUID | 아니오 | 부모 Category. 없으면 루트 |
| `depth` | INTEGER | 예 | 루트 1, 최대 3 |
| `createdAt` | TIMESTAMP | 예 | 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 수정 시각 |

### 2-2. 관계와 제약

- `parentId`가 있으면 부모가 존재해야 한다.
- 자기 자신 또는 하위 Category를 부모로 지정할 수 없다.
- 부모 변경으로 하위 Category의 깊이가 3을 초과하면 거부한다.
- Category와 하위 Category의 부모·깊이 변경은 하나의 트랜잭션으로 처리한다.
- P7은 P2의 Category validation interface를 사용하고 검증 규칙을 복제하지 않는다.

## 3. API 정의

### 3-1. Category 생성

`POST /api/v1/admin/categories`

권한: `ADMIN`

요청:

```json
{
  "name": "노트북",
  "parentId": "uuid"
}
```

#### 성공 응답: `201 Created`

```json
{
  "categoryId": "uuid",
  "name": "노트북",
  "parentId": "uuid",
  "depth": 3,
  "createdAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. Category 원본 예외는 [P2 Category](../p2/p2-category.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | [CATEGORY-001](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-002](../p2/p2-category.md) | — | — | — | — |
| 404 | [CATEGORY-003](../p2/p2-category.md) | — | — | — | — |
| 409 | [CATEGORY-004](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-005](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-006](../p2/p2-category.md) | — | — | — | — |

### 3-2. Category 수정

`PATCH /api/v1/admin/categories/{categoryId}`

권한: `ADMIN`

요청:

```json
{
  "name": "노트북·태블릿",
  "parentId": "uuid"
}
```

전달하지 않은 필드는 유지한다. `parentId: null`은 루트 이동을 의미한다.

#### 성공 응답: `200 OK`

```json
{
  "categoryId": "uuid",
  "name": "노트북·태블릿",
  "parentId": "uuid",
  "depth": 3,
  "updatedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다. Category 원본 예외는 [P2 Category](../p2/p2-category.md)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | [CATEGORY-003](../p2/p2-category.md) | — | — | — | — |
| 400 | `ADMIN-020` | 수정 필드가 없음 | 수정할 값을 입력해 주세요. | 없음 | 요청 본문 |
| 400 | [CATEGORY-001](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-005](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-006](../p2/p2-category.md) | — | — | — | — |
| 400 | [CATEGORY-002](../p2/p2-category.md) | — | — | — | — |
| 409 | [CATEGORY-004](../p2/p2-category.md) | — | — | — | — |
