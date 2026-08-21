# P2 Category API

이 문서는 Category 데이터 모델과 Category를 조회·관리하는 API를 정의한다. 업무 정책은 [P2 Policy](p2-policy.md), P7의 관리자 HTTP 진입점은 [P7 Category](../p7/p7-category.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Category` | 상품 분류 이름과 부모·자식 계층 | 전체 조회·생성·수정 |
| `CatalogProduct.categoryId` | 상품의 대표 Category 단일 참조 | CatalogProduct 생성·조회 |

P2가 Category 원본과 검증을 소유하고, P7이 관리자용 HTTP 진입점을 제공한다. Seller의 새 Category 제안은 [P8 Catalog Requests](../p8/p8-catalog-requests.md)에서 처리한다.

## 2. 데이터 모델

### 2-1. `Category`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | UUID | 예 | Category 식별자. 서버 생성 |
| `name` | String | 예 | 공백이 아닌 표시 이름 |
| `parentId` | UUID | 아니오 | 부모 Category. 루트는 `null` |
| `depth` | Integer | 예 | 루트 1, 부모 깊이 + 1 |
| `createdAt` | Instant | 예 | 생성 시각 |
| `updatedAt` | Instant | 예 | 수정 시각 |

### 2-2. 관계와 제약

- 루트는 `parentId = null`, `depth = 1`이다. 최대 깊이는 3이다.
- 모든 Category의 `name`은 `parentId`와 상관없이 전역적으로 유일하다.
- `parentId`가 지정되면 부모가 존재해야 하고 `depth = parent.depth + 1`이어야 한다.
- 자기 자신이나 자신의 하위 Category를 부모로 지정할 수 없다.
- 하위 Category를 함께 이동한 결과 깊이가 3을 넘으면 전체 변경을 거부한다.
- Category 연결은 순환을 만들 수 없다.
- 기본 범위에서 Category 삭제·보관은 제공하지 않는다.
- `CatalogProduct`는 대표 Category 하나만 `categoryId`로 참조한다. Category 검색은 선택한 Category와 모든 하위 Category를 포함한다.

## 3. API 정의

P7이 다음 HTTP 진입점을 제공한다. 성공 응답은 Category Response DTO를 직접 반환하며 공통 성공 봉투를 사용하지 않는다.

### 3-1. Category 전체 조회

`GET /api/v1/categories`

권한: 구매자·Seller·ADMIN

#### 성공 응답: `200 OK`

```json
{
  "categories": [
    {
      "id": "uuid-electronics",
      "name": "전자기기",
      "parentId": null,
      "depth": 1,
      "children": []
    }
  ]
}
```

정렬은 `depth ASC`, 같은 부모 안에서는 `name ASC`, `id ASC`를 사용한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 500 | [SYSTEM-001](../index.md#예외-응답) | Category 트리 조회 실패 | 요청을 처리하지 못했습니다. | 없음 | 저장소 원인과 requestId |

### 3-2. Category 생성

`POST /api/v1/admin/categories`

권한: ADMIN

요청:

```json
{
  "name": "그래픽카드",
  "parentId": "uuid-computer"
}
```

`parentId`가 `null`이면 루트 Category를 생성한다.

#### 성공 응답: `201 Created`

```json
{
  "id": "uuid-graphics-card",
  "name": "그래픽카드",
  "parentId": "uuid-computer",
  "depth": 3
}
```

Category와 부모 연결은 하나의 트랜잭션으로 생성한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATEGORY-001` | 이름이 비어 있거나 공백만 포함 | 카테고리 이름을 확인해 주세요. | `field`, `reason` | 입력값을 제외한 검증 원인 |
| 400 | `CATEGORY-002` | 부모 지정으로 최대 깊이 3 초과 | 카테고리 깊이는 3단계까지 가능합니다. | `parentId`, `depth` | 계산된 깊이 |
| 404 | `CATEGORY-003` | 지정한 부모가 없음 | 상위 카테고리를 찾을 수 없습니다. | 없음 | `parentId`, requestId |
| 409 | `CATEGORY-004` | 다른 Category와 이름 중복 | 같은 이름의 카테고리가 이미 있습니다. | `name` | 충돌 식별자 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATEGORY-011` | 저장소 오류 | 요청을 처리하지 못했습니다. | 없음 | 저장소 원인과 requestId |

### 3-3. Category 수정

`PATCH /api/v1/admin/categories/{categoryId}`

권한: ADMIN

요청:

```json
{
  "name": "컴퓨터 부품",
  "parentId": "uuid-electronics"
}
```

`name`과 `parentId`는 전달된 값만 변경한다. 부모를 변경하면 해당 Category와 모든 하위 Category의 `depth`를 함께 계산한다.

#### 성공 응답: `200 OK`

```json
{
  "id": "uuid-computer",
  "name": "컴퓨터 부품",
  "parentId": "uuid-electronics",
  "depth": 2
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATEGORY-001` | 이름이 비어 있거나 공백만 포함 | 카테고리 이름을 확인해 주세요. | `field`, `reason` | 입력값을 제외한 검증 원인 |
| 400 | `CATEGORY-005` | 자기 자신 또는 하위 Category를 부모로 지정 | 카테고리 계층을 확인해 주세요. | 없음 | 순환 후보 ID |
| 400 | `CATEGORY-006` | 부모 연결이 순환을 만듦 | 카테고리 계층을 확인해 주세요. | 없음 | 순환 경로 |
| 400 | `CATEGORY-002` | 하위 Category 포함 깊이 3 초과 | 카테고리 깊이는 3단계까지 가능합니다. | `categoryId`, `depth` | 계산된 최대 깊이 |
| 404 | `CATEGORY-003` | Category 또는 부모가 없음 | 카테고리를 찾을 수 없습니다. | 없음 | 조회 대상 ID |
| 409 | `CATEGORY-004` | 다른 Category와 이름 중복 | 같은 이름의 카테고리가 이미 있습니다. | `name` | 충돌 식별자 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATEGORY-012` | 계층 갱신 저장 실패 | 요청을 처리하지 못했습니다. | 없음 | 저장소 원인과 requestId |

## 4. 공개 application interface

P7·P8·P9는 다음 공개 interface를 사용하며 P2 Repository를 직접 호출하지 않는다.

```text
CatalogCategoryValidationApi.validate(CategoryProposal)
  → CategoryValidationResult(valid, depth, errors)

CatalogCategoryQueryApi.findSelfAndDescendantIds(categoryId)
  → Set<CategoryId>
```

- `validate`는 이름·부모·깊이·순환·전역 이름 중복을 검증한다.
- 검증 결과는 Category를 생성하거나 이름을 예약하지 않는다.
- Category가 존재하지 않으면 `CATEGORY-003`의 원본 의미를 호출 API의 응답 계약에 맞춰 참조한다.
