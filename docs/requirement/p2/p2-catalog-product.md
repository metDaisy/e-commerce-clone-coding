# P2 CatalogProduct API

이 문서는 CatalogProduct 데이터 모델과 상품 공통 정보·식별자·CatalogProduct Media를 관리하는 API를 정의한다. 업무 정책은 [P2 Policy](p2-policy.md), Category 규칙은 [Category API](p2-category.md), 공통 파일 계약은 [P12 Media](../p12/p12-media.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `CatalogProduct` | 상품명·설명·브랜드·외부 식별자·공통 attributes·상태 | 조회·생성·수정·보관 |
| `CatalogProductIdentifier` | CatalogProduct와 외부 상품 코드의 유형별 연결 | 생성·수정 |
| `CatalogProductMedia` | 상품 이미지의 대상·정렬·대표·보관 상태 | 연결·수정·보관 |
| `Category` | 상품의 대표 분류 | [Category API](p2-category.md) |
| `ProductVariant` | 상품군에 속한 구매 단위 | [ProductVariant API](p2-product-variant.md) |

P2는 Category·ProductVariant의 내부 모델을 응답에 복제하지 않는다. 외부 도메인의 Offer·Inventory·Review는 식별자와 공개 계약만 참조한다.

## 2. 데이터 모델

### 2-1. `CatalogProduct`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `catalogProductId` | UUID | 예 | 서버 생성 내부 식별자 |
| `categoryId` | UUID | 예 | 대표 Category 단일 참조 |
| `name` | String | 예 | 공백이 아닌 상품명 |
| `description` | String | 예 | 공백이 아닌 상품 설명 |
| `brand` | String | 예 | 상품 브랜드 |
| `attributes` | JSON object | 아니오 | 카테고리별 구조가 달라 별도 포맷을 강제하지 않으며, `null`·생략 시 `{}` |
| `identifiers` | Object | 예 | `asin`, `gtin`, `upc`, `ean`, `isbn` 중 하나 이상 |
| `publicationStatus` | Enum | 예 | `ACTIVE` 또는 `ARCHIVED` |
| `archivedAt` | Instant | 아니오 | 보관 시각 |
| `createdAt` | Instant | 예 | 생성 시각 |
| `updatedAt` | Instant | 예 | 수정 시각 |

### 2-2. `CatalogProductMedia`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `mediaId` | UUID | 예 | Media 연결 식별자 |
| `catalogProductId` | UUID | 예 | 소유 CatalogProduct |
| `uploadId` | UUID | 예 | P12에서 `READY`로 발급된 업로드 식별자 |
| `sortOrder` | Integer | 예 | 상품 안에서 유일한 정렬 순서 |
| `isPrimary` | Boolean | 예 | 대표 이미지 여부. 상품당 하나 |
| `status` | Enum | 예 | `ACTIVE` 또는 `ARCHIVED` |
| `createdAt` | Instant | 예 | 연결 시각 |
| `updatedAt` | Instant | 예 | 변경 시각 |

### 2-3. 관계와 제약

- 생성 시 `categoryId`가 존재해야 하며 CatalogProduct·식별자·Category 연결은 하나의 트랜잭션으로 처리한다.
- ProductVariant·Offer·Inventory·Media는 CatalogProduct 생성 시 함께 생성하지 않는다.
- `asin`, `gtin`, `upc`, `ean`, `isbn` 중 하나 이상을 입력한다. 각 유형은 형식·체크디지트·CatalogProduct 간 유일성을 검증한다.
- `isbn`만 외부 도서 API로 추가 검증한다.
- `attributes`는 카테고리마다 구조가 다르므로 별도 포맷이나 Category별 스키마를 강제하지 않는다. `null`·생략은 `{}`로 처리한다.
- `PATCH`의 `attributes`는 JSON Merge Patch다. 일반 값은 추가·수정, `null`은 키 삭제, 생략은 유지, `{}`는 변경 없음이다. `null` 자체를 값으로 저장하지 않는다.
- 일반 수정은 `name`, `description`, `brand`, `attributes`만 받는다. Category·Variant·Offer·Inventory·`publicationStatus`는 받지 않는다.
- 외부 식별자 수정은 전달된 값만 바꾸며 식별자 삭제는 지원하지 않는다.
- `ACTIVE` Media는 상품당 최대 20개, 대표 Media는 최대 하나, `sortOrder`는 상품 내 유일값이다.
- Media DELETE와 CatalogProduct 보관은 물리 삭제가 아닌 `ARCHIVED` 전환이다.

## 3. API 정의

성공 응답은 고객용 Product API에서 내부 `catalogProductId`, `variantId`를 제외한다. 관리자·Product Manager용 Catalog 조회 API는 등록 대상 선택과 운영을 위해 내부 ID와 상태를 반환한다.

### 3-1. CatalogProduct 생성

`POST /api/v1/admin/catalog-products`

권한: ADMIN

요청:

```json
{
  "categoryId": "uuid-graphics-card",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "attributes": { "connectionType": "BLUETOOTH" },
  "identifiers": { "gtin": "8801234567890" }
}
```

#### 성공 응답: `201 Created`

```json
{
  "catalogProductId": "uuid-product",
  "categoryId": "uuid-graphics-card",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "attributes": { "connectionType": "BLUETOOTH" },
  "identifiers": { "gtin": "8801234567890" },
  "publicationStatus": "ACTIVE"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-013` | 이름·설명·브랜드·attributes 검증 실패 | 상품 정보를 확인해 주세요. | 실패 필드와 수정 가능한 reason | 내부 검증 원인 |
| 400 | `CATALOG-014` | 식별자 형식·체크디지트 실패 또는 식별자 없음 | 상품 식별자 입력을 확인해 주세요. | `details.fields`에 필드·reason·안내 메시지 | 실제 입력값은 로그에만 기록 |
| 400 | `CATALOG-015` | ISBN 외부 검증 실패 | 상품 식별자 입력을 확인해 주세요. | `field=isbn`, `reason=external_verification_failed` | 외부 응답 원문은 로그에만 기록 |
| 404 | [CATEGORY-003](../p2/p2-category.md) | Category가 없음 | 카테고리를 찾을 수 없습니다. | 없음 | `categoryId`, requestId |
| 409 | `CATALOG-017` | 식별자가 다른 CatalogProduct와 중복 | 이미 등록된 상품 식별자입니다. | 식별자 유형만 | 충돌 식별자 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-018` | 저장소 또는 예상하지 못한 오류 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

식별자 오류가 여러 개면 `details.fields`에 모든 실패 필드를 반환한다. 전체 식별자 값·SQL·외부 API 원문은 반환하지 않는다.

### 3-2. CatalogProduct 조회

`GET /api/v1/catalog-products/{catalogProductId}`

권한: `ADMIN` 또는 `PRODUCT_MANAGER` 권한과 `ACTIVE Seller` 상태를 가진 사용자.

#### 성공 응답: `200 OK`

```json
{
  "catalogProductId": "uuid-product",
  "name": "무선 헤드폰",
  "description": "카탈로그 상품 설명",
  "brand": "Example Brand",
  "attributes": { "connectionType": "BLUETOOTH" },
  "media": [],
  "publicationStatus": "ACTIVE"
}
```

관리자·Product Manager 응답에는 `categoryId`, 내부 ID, `publicationStatus`, 연결된 ProductVariant를 포함한다. Product Manager는 활성 CatalogProduct만 조회할 수 있으며, 고객용 공개 응답은 P9 Product API에서 별도로 정의한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-019` | 미존재 또는 비관리자의 보관 상품 조회 | 상품을 찾을 수 없습니다. | 없음 | `lookupResult=NOT_FOUND` 또는 `ARCHIVED` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 500 | `CATALOG-020` | 조회 실패 | 상품을 조회하지 못했습니다. | 없음 | 저장소 원인과 requestId |

### 3-3. CatalogProduct 메타데이터 수정

`PATCH /api/v1/admin/catalog-products/{catalogProductId}`

권한: ADMIN

요청:

```json
{
  "name": "무선 헤드폰 Pro",
  "description": "수정된 설명",
  "brand": "Example Brand",
  "attributes": { "connectionType": "BLUETOOTH", "noiseCanceling": true }
}
```

#### 성공 응답: `200 OK`

생성 응답과 같은 CatalogProduct Response DTO를 반환한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-013` | 수정 필드 또는 Merge Patch 검증 실패 | 상품 정보를 확인해 주세요. | 실패 필드와 reason | 내부 검증 원인 |
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 409 | `CATALOG-021` | `ARCHIVED` 상품 수정 | 보관된 상품은 변경할 수 없습니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-022` | 저장 실패 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

### 3-4. 외부 식별자 수정

`PATCH /api/v1/admin/catalog-products/{catalogProductId}/identifiers`

권한: ADMIN

요청:

```json
{
  "identifiers": {
    "gtin": "8801234567890",
    "isbn": "9781234567890"
  }
}
```

#### 성공 응답: `200 OK`

```json
{
  "catalogProductId": "uuid-product",
  "identifiers": {
    "gtin": "8801234567890",
    "isbn": "9781234567890"
  }
}
```

전달하지 않은 식별자는 유지하며, 식별자를 `null`로 삭제할 수 없다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-014` | 형식·체크디지트 실패 또는 모든 식별자 삭제 | 상품 식별자 입력을 확인해 주세요. | `details.fields` | 내부 검증 원인 |
| 400 | `CATALOG-015` | ISBN 외부 검증 실패 | 상품 식별자 입력을 확인해 주세요. | 실패 필드와 reason | 외부 응답 원문은 로그에만 기록 |
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 409 | `CATALOG-017` | 식별자 중복 | 이미 등록된 상품 식별자입니다. | 식별자 유형만 | 충돌 식별자 |
| 409 | `CATALOG-021` | `ARCHIVED` 상품 수정 | 보관된 상품은 변경할 수 없습니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |

### 3-5. CatalogProduct 보관

`POST /api/v1/admin/catalog-products/{catalogProductId}/archive`

권한: ADMIN

#### 성공 응답: `200 OK`

```json
{
  "catalogProductId": "uuid-product",
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-16T12:31:33Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 409 | `CATALOG-023` | 이미 보관된 상품 재보관 | 이미 보관된 상품입니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-024` | 보관 상태 저장 실패 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

보관 시 하위 Variant·CatalogProduct Media를 물리 삭제하지 않고 공개·Seller 조회와 Offer 등록 대상에서 제외한다. 연결 Offer 비활성화는 P9가 수행한다.

## 4. CatalogProduct Media API

CatalogProduct Media의 업로드 준비·파일 검증·저장소 계약은 P12가 소유한다. P2는 `READY` 상태 `uploadId`의 대상 연결, 정렬, 대표 지정, 보관을 소유한다.

### 4-1. Media 연결

`POST /api/v1/admin/catalog-products/{catalogProductId}/media`

권한: ADMIN

요청:

```json
{
  "uploadId": "uuid-upload",
  "sortOrder": 1,
  "isPrimary": true
}
```

#### 성공 응답: `201 Created`

```json
{
  "mediaId": "uuid-media",
  "uploadId": "uuid-upload",
  "sortOrder": 1,
  "isPrimary": true,
  "status": "ACTIVE"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 404 | `CATALOG-025` | uploadId 미존재 또는 소유자 불일치 | 이미지를 업로드할 수 없습니다. | 없음 | uploadId와 P12 조회 결과 |
| 409 | `CATALOG-026` | uploadId가 `READY`가 아님 | 이미지 업로드를 완료해 주세요. | 현재 업로드 상태 | P12 상태 |
| 409 | `CATALOG-027` | ACTIVE Media 20개 초과 | 상품 이미지 설정을 확인해 주세요. | `field=media` | 내부 제약 원인 |
| 409 | `CATALOG-037` | 대표 Media 중복 | 상품 이미지 설정을 확인해 주세요. | `field=isPrimary` | 내부 제약 원인 |
| 409 | `CATALOG-038` | sortOrder 중복 | 상품 이미지 설정을 확인해 주세요. | `field=sortOrder` | 내부 제약 원인 |
| 409 | `CATALOG-021` | `ARCHIVED` 상품에 연결 | 보관된 상품은 변경할 수 없습니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |

### 4-2. Media 수정

`PATCH /api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}`

권한: ADMIN

요청:

```json
{
  "sortOrder": 2,
  "isPrimary": false
}
```

#### 성공 응답: `200 OK`

Media 연결 성공 응답과 같은 Media Response DTO를 반환한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 상품 조회 원인 |
| 404 | `CATALOG-028` | Media 미존재 또는 보관됨 | 이미지를 찾을 수 없습니다. | 없음 | Media 조회 원인 |
| 409 | `CATALOG-037` | 대표 Media 중복 | 상품 이미지 설정을 확인해 주세요. | `field=isPrimary` | 내부 제약 원인 |
| 409 | `CATALOG-038` | sortOrder 중복 | 상품 이미지 설정을 확인해 주세요. | `field=sortOrder` | 내부 제약 원인 |
| 409 | `CATALOG-021` | 보관된 상품의 Media 수정 | 보관된 상품은 변경할 수 없습니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |

### 4-3. Media 보관

`DELETE /api/v1/admin/catalog-products/{catalogProductId}/media/{mediaId}`

권한: ADMIN

#### 성공 응답: `200 OK`

```json
{
  "mediaId": "uuid-media",
  "status": "ARCHIVED"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-019` | 상품 미존재 | 상품을 찾을 수 없습니다. | 없음 | 상품 조회 원인 |
| 404 | `CATALOG-028` | Media 미존재 또는 이미 보관됨 | 이미지를 찾을 수 없습니다. | 없음 | Media 조회 원인 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |

보관된 Media는 공개 조회에서 제외하며 물리 파일 삭제는 P12 저장소 정책에 위임한다.
