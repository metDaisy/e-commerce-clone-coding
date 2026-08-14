# P2 ProductVariant (상품 Variant)

[P2 Catalog 개요](p2-catalog.md)의 ProductVariant 식별·속성 규칙에 대한 상세 요구사항이다.

## 1. 모델과 책임

ProductVariant는 고객이 선택하고 주문하는 실제 구매 단위다.

```text
CatalogProduct 1 : N ProductVariant
```

- 하나의 ProductVariant는 정확히 하나의 CatalogProduct에 속한다.
- ProductVariant의 유일한 식별자는 서버가 생성한 `variantId`다.
- 색상·사이즈·용량·무게·포장 크기 등 판매 단위 정보는 동적 `attributes`에 저장한다.
- 단일 상품도 `기본 옵션` ProductVariant 하나를 생성한다.
- Offer·Inventory는 P9가 소유하며 ProductVariant 생성 시 함께 만들지 않는다.

## 2. 등록

```http
POST /api/v1/admin/catalog-products/{catalogProductId}/variants
```

- `ADMIN`만 보관되지 않은 CatalogProduct에 등록할 수 있다.
- `variantId`는 요청 본문으로 받지 않는다. 서버가 UUID를 자동 생성한다.
- ProductVariant 생성 시 Offer·Inventory·Media는 생성하지 않는다. ProductVariant에는 Media를 연결하지 않는다.
- `displayName`과 `attributes`를 입력한다. `weight`, `dimensions` 같은 고정 필드는 사용하지 않는다.
- UUID 충돌은 DB UNIQUE 제약으로 최종 방지하며 재시도 후 저장소 오류는 공통 `500 INTERNAL_SERVER_ERROR`다.

## 3. 수정

```http
PATCH  /api/v1/admin/product-variants/{variantId}
```

- `ADMIN`만 호출할 수 있다.
- 수정 가능한 필드는 `displayName`, `attributes`이며 `variantId`는 수정할 수 없다.
- `attributes`는 [CatalogProduct의 부분 수정 규칙](p2-catalog-product.md#3-동적-attributes)에 따른다.
- 보관된 CatalogProduct 또는 ProductVariant는 수정할 수 없다.
- ProductVariant는 옵션·속성만 소유한다. 판매자 소개 이미지는 P9의 Offer Media가 소유하고, CatalogProduct 공통 이미지는 [CatalogProduct Media](p2-catalog-product.md#5-media)가 소유한다.

## 4. 보관과 조회

```http
POST /api/v1/admin/product-variants/{variantId}/archive
```

- 보관은 `publicationStatus = ARCHIVED`, `archivedAt = 현재 시각`으로 변경하며 물리 삭제하지 않는다.
- 연결된 Offer는 P9 규칙에 따라 비활성화한다.
- 보관된 Variant는 고객 검색·상세, Seller Catalog 조회, Offer 등록 대상에서 제외한다.
- Seller·구매자가 보관되거나 존재하지 않는 Variant를 내부 `variantId`로 조회하면 `404 VARIANT_NOT_FOUND`다.
- 관리자 조회는 존재하는 Variant라면 `ACTIVE`·`ARCHIVED` 모두 반환한다.
- 관리자 요청에서 실제로 존재하지 않는 Variant만 `404 VARIANT_NOT_FOUND`다.
- `CatalogVariantQueryApi.findActiveByVariantId(variantId)`는 P9가 Offer 등록에 사용하는 P2 공개 interface다.
- `CatalogVariantReference`는 Modulith 내부 호출용이며 고객·Seller HTTP 응답으로 노출하지 않는다.

## 5. 예외

| HTTP | 코드 | 클라이언트 메시지 | 서버 상세 원인 |
|---:|---|---|---|
| 400 | `VALIDATION_ERROR` | Variant 입력을 확인해 주세요. | `displayName` 또는 `attributes`의 필드별 검증 결과 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요합니다. | 인증 정보 없음·위조·만료 |
| 403 | `ACCESS_DENIED` | 이 작업을 수행할 권한이 없습니다. | ADMIN 권한이 아닌 Variant 변경 요청 |
| 404 | `CATALOG_PRODUCT_NOT_FOUND` | 상품을 찾을 수 없습니다. | 부모 CatalogProduct가 `NOT_FOUND` 또는 `ARCHIVED` |
| 404 | `VARIANT_NOT_FOUND` | 상품 옵션을 찾을 수 없습니다. | Variant가 `NOT_FOUND` 또는 `ARCHIVED` |
| 409 | `PRODUCT_VARIANT_ARCHIVED` | 보관된 상품 옵션은 변경할 수 없습니다. | `publicationStatus = ARCHIVED` 상태에서 변경 시도 |
| 500 | `INTERNAL_SERVER_ERROR` | 요청을 처리하지 못했습니다. | UUID 생성·저장소·예상하지 못한 서버 오류 |

구매자·Seller에게 보관 여부를 노출하지 않는 404는 `VARIANT_NOT_FOUND`와 추상적인 클라이언트 메시지를 사용한다. 서버는 `getDetailMessage()`에 `lookupResult = ARCHIVED` 또는 `NOT_FOUND`와 내부 식별자를 기록한다. `VALIDATION_ERROR`는 공통 예외 정책에 따라 `details.fields`에 수정 가능한 필드·원인·안내 메시지를 포함할 수 있지만, 내부 로그 상세값은 반환하지 않는다.
