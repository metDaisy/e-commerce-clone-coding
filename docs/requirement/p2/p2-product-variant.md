# P2 ProductVariant API

이 문서는 ProductVariant 데이터 모델과 상품의 실제 구매 단위를 관리하는 API를 정의한다. 업무 정책은 [P2 Policy](p2-policy.md), CatalogProduct 공통 정보는 [CatalogProduct API](p2-catalog-product.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `ProductVariant` | 고객이 선택하고 주문하는 실제 구매 단위 | 조회·생성·수정·보관 |
| `CatalogProduct` | Variant가 속한 상품군과 공통 정보 | 부모 조회 |
| `Offer` | 판매자별 가격·판매 상태·판매 Media | P9가 소유 |
| `Inventory` | 판매 가능한 재고 | P9가 소유 |

```text
CatalogProduct 1 : N ProductVariant
ProductVariant 1 : N Offer (P9)
```

ProductVariant는 옵션·속성만 소유한다. Media는 연결하지 않으며 CatalogProduct 공통 이미지는 [CatalogProduct Media](p2-catalog-product.md#4-catalogproduct-media-api), 판매자 소개 이미지는 P9 Offer Media를 사용한다.

## 2. 데이터 모델

### 2-1. `ProductVariant`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `variantId` | UUID | 예 | 서버 생성 내부 식별자. 수정 불가 |
| `catalogProductId` | UUID | 예 | 소속 CatalogProduct |
| `displayName` | String | 예 | 고객이 선택할 때 보는 옵션 표시명 |
| `attributes` | JSON object | 예 | 색상·사이즈·용량·무게·포장 크기 등 동적 속성 |
| `publicationStatus` | Enum | 예 | `ACTIVE` 또는 `ARCHIVED` |
| `archivedAt` | Instant | 아니오 | 보관 시각 |
| `createdAt` | Instant | 예 | 생성 시각 |
| `updatedAt` | Instant | 예 | 수정 시각 |

### 2-2. 관계와 제약

- 하나의 Variant는 CatalogProduct 하나에만 속한다.
- 단일 상품도 기본 옵션을 표현하는 ProductVariant 하나를 생성한다.
- `variantId`는 서버가 UUID로 생성하며 요청 본문으로 받지 않는다.
- SKU를 입력·자동 생성·검증하거나 SKU 기반 조회·응답을 제공하지 않는다.
- `displayName`은 공백만으로 구성할 수 없다.
- `attributes` 최상위 값은 object여야 한다. JSON Merge Patch 규칙은 [CatalogProduct API](p2-catalog-product.md#2-데이터-모델)의 attributes 규칙과 같다.
- `weight`, `dimensions` 같은 고정 필드를 별도로 만들지 않는다.
- 보관된 CatalogProduct 또는 ProductVariant는 수정·Offer 등록 대상이 아니다.
- 보관은 물리 삭제가 아니며 연결된 Offer 비활성화는 P9 규칙에 따른다.

## 3. API 정의

관리자 응답에는 운영에 필요한 내부 ID와 상태를 포함한다. 구매자·Seller 응답에는 `variantId`, `catalogProductId`, 보관 상태를 반환하지 않는다.

### 3-1. ProductVariant 생성

`POST /api/v1/admin/catalog-products/{catalogProductId}/variants`

권한: ADMIN

요청:

```json
{
  "displayName": "블랙 / 256GB",
  "attributes": {
    "color": "BLACK",
    "storage": "256GB"
  }
}
```

#### 성공 응답: `201 Created`

```json
{
  "variantId": "uuid-variant",
  "catalogProductId": "uuid-product",
  "displayName": "블랙 / 256GB",
  "attributes": {
    "color": "BLACK",
    "storage": "256GB"
  },
  "publicationStatus": "ACTIVE"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-029` | displayName·attributes 검증 실패 | 상품 옵션 입력을 확인해 주세요. | 실패 필드와 reason | 내부 검증 원인 |
| 404 | `CATALOG-019` | 부모 CatalogProduct 미존재 또는 보관 | 상품을 찾을 수 없습니다. | 없음 | 부모 조회 결과 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-030` | UUID 생성·저장 실패 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

생성 시 Offer·Inventory·Media를 만들지 않는다.

### 3-2. ProductVariant 조회

구매자·Seller 조회:

`GET /api/v1/product-variants/{variantId}`

권한: 구매자·Seller

관리자 조회:

`GET /api/v1/admin/product-variants/{variantId}`

권한: ADMIN

#### 성공 응답: `200 OK`

구매자·Seller:

```json
{
  "displayName": "블랙 / 256GB",
  "attributes": {
    "color": "BLACK",
    "storage": "256GB"
  }
}
```

ADMIN:

```json
{
  "variantId": "uuid-variant",
  "catalogProductId": "uuid-product",
  "displayName": "블랙 / 256GB",
  "attributes": {
    "color": "BLACK",
    "storage": "256GB"
  },
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-16T12:31:33Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-031` | Variant 미존재 또는 비관리자의 보관 Variant 조회 | 상품 옵션을 찾을 수 없습니다. | 없음 | `lookupResult=NOT_FOUND` 또는 `ARCHIVED` |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 500 | `CATALOG-032` | 조회 실패 | 상품 옵션을 조회하지 못했습니다. | 없음 | 저장소 원인과 requestId |

### 3-3. ProductVariant 수정

`PATCH /api/v1/admin/product-variants/{variantId}`

권한: ADMIN

요청:

```json
{
  "displayName": "블랙 / 512GB",
  "attributes": {
    "color": "BLACK",
    "storage": "512GB"
  }
}
```

#### 성공 응답: `200 OK`

생성 응답과 같은 ProductVariant Response DTO를 반환한다.

`variantId`, `catalogProductId`, `publicationStatus`는 요청으로 수정할 수 없다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CATALOG-029` | displayName·attributes 검증 실패 | 상품 옵션 입력을 확인해 주세요. | 실패 필드와 reason | 내부 검증 원인 |
| 404 | `CATALOG-031` | Variant 미존재 | 상품 옵션을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 409 | `CATALOG-033` | 보관된 Variant 수정 | 보관된 상품 옵션은 변경할 수 없습니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-034` | 저장소 오류 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

### 3-4. ProductVariant 보관

`POST /api/v1/admin/product-variants/{variantId}/archive`

권한: ADMIN

#### 성공 응답: `200 OK`

```json
{
  "variantId": "uuid-variant",
  "publicationStatus": "ARCHIVED",
  "archivedAt": "2026-08-16T12:31:33Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `CATALOG-031` | Variant 미존재 | 상품 옵션을 찾을 수 없습니다. | 없음 | 조회 원인과 ID |
| 409 | `CATALOG-035` | 이미 보관된 Variant 재보관 | 이미 보관된 상품 옵션입니다. | 없음 | 현재 상태 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 500 | `CATALOG-036` | 보관 상태 저장 실패 | 요청을 처리하지 못했습니다. | 없음 | 내부 원인과 requestId |

보관 시 Variant를 물리 삭제하지 않고 고객·Seller 조회와 Offer 등록 대상에서 제외한다. 연결 Offer 비활성화는 P9가 수행한다.

## 4. 공개 application interface

P9는 Offer 등록 전에 다음 interface를 호출한다.

```text
CatalogVariantQueryApi.findActiveByVariantId(variantId)
  → CatalogVariantReference
```

- `CatalogVariantReference`는 Modulith 내부 호출용이다. 고객·Seller HTTP 응답으로 반환하지 않는다.
- ID가 미존재하거나 Variant가 `ARCHIVED`면 활성 Variant 조회 결과가 없으며 P9는 Offer 등록을 거부한다.
- P9는 ProductVariant의 내부 Entity·Repository를 직접 참조하지 않는다.
