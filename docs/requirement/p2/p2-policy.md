# P2 Catalog Policy

이 문서는 P2 Catalog의 범위·책임과 API·ORM 구현에 독립적인 업무 정책을 정의한다. 데이터 모델과 HTTP 계약은 [Catalog API](p2-catalog.md), [Category API](p2-category.md), [CatalogProduct API](p2-catalog-product.md), [ProductVariant API](p2-product-variant.md)를 따른다.

## 1. 범위와 책임

### 범위

- 상품을 분류하는 Category 계층
- 상품군의 공통 정보인 CatalogProduct
- 고객이 선택하고 주문하는 ProductVariant
- CatalogProduct에 연결된 상품용 Media의 대상·대표·순서·보관 정책
- 다른 도메인이 기준 상품 정보를 검증·조회하는 공개 application interface

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Category·CatalogProduct·ProductVariant 원본과 상태 | P2 Catalog | [Catalog API](p2-catalog.md) |
| 관리자 HTTP 진입점과 승인 흐름 | P7 Admin | [P7 Catalog](../p7/p7-catalog.md) |
| Seller 등록 요청과 제안 | P8 Seller | [P8 Catalog Requests](../p8/p8-catalog-requests.md) |
| 가격·재고·판매 상태·Offer Media | P9 Offer | [P9 Offer](../p9/p9-offer.md) |
| 공통 업로드와 물리 파일 저장 | P12 Media·공통 인프라 | [P12 Media](../p12/p12-media.md) |

P2는 판매자별 판매 조건, 재고, 리뷰, 주문을 소유하지 않는다. 다른 도메인의 내부 모델·Repository·서비스 구현을 직접 사용하지 않는다.

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Category` | 상품을 탐색하기 위한 최대 깊이 3의 계층형 분류 |
| `CatalogProduct` | 여러 Variant가 공유하는 상품명·설명·브랜드·식별자·공통 attributes |
| `ProductVariant` | 색상·용량 등 선택 가능한 실제 구매 단위. 단일 상품도 하나를 가진다. |
| `ADMIN` | 정식 Category·CatalogProduct·ProductVariant·CatalogProduct Media를 생성·수정·보관한다. |
| `Seller` | P8을 통해 등록을 요청하고, P9에서 자신의 Offer를 관리한다. P2 원본을 직접 변경하지 않는다. |
| 구매자 | 공개 상태의 상품·Variant와 Category를 조회한다. 내부 ID와 보관 여부를 알 수 없다. |
| `ACTIVE` | 공개 조회 및 후속 업무의 기준 리소스로 사용할 수 있는 상태 |
| `ARCHIVED` | 물리 삭제하지 않고 공개·신규 연결에서 제외한 종단 상태 |

기존 용어와 의미가 충돌하면 [domain-glossary.md](../../domain-glossary.md)를 함께 갱신한다.

## 3. 핵심 업무 규칙

- 정식 Category·CatalogProduct·ProductVariant·CatalogProduct Media의 생성·수정·보관은 `ADMIN`만 수행한다.
- Seller의 Category·CatalogProduct·ProductVariant 제안은 P8이 받고, P7의 승인 후 P2 정식 리소스로 반영한다.
- CatalogProduct는 대표 Category 하나와 연결한다. Category 계층을 변경해도 상품의 연결 FK는 하나만 유지한다.
- CatalogProduct 생성은 ProductVariant·Offer·Inventory·Media를 함께 생성하지 않는다.
- ProductVariant는 서버가 생성한 `variantId`만 식별자로 사용하며 요청이 SKU를 지정하지 않는다.
- ProductVariant는 자체 Media를 소유하지 않는다. 공통 상품 이미지는 CatalogProduct Media, 판매자 소개 이미지는 P9 Offer Media가 소유한다.
- `attributes`는 동적 JSON object다. CatalogProduct는 상품군 공통 정보, ProductVariant는 판매 단위 정보를 담는다. Category별 필수 스키마는 현재 강제하지 않는다.
- 보관은 물리 삭제가 아니라 공개·신규 연결에서 제외하는 상태 변경이다.
- 구매자·Seller에게 보관 여부나 내부 ID를 노출하지 않는다. 서버 로그에는 운영에 필요한 판별 정보를 남긴다.
- SKU, ProductType, ItemType, SearchKeyword는 현재 계약에 포함하지 않는다.

## 4. 불변식과 상태 전이

### 불변식

- Category 루트는 `parentId = null`, `depth = 1`; 전체 계층의 최대 깊이는 3이다.
- 모든 Category의 `name`은 부모와 상관없이 전역적으로 유일하다. 자기 자신·하위 Category를 부모로 지정할 수 없고, 연결은 순환을 만들 수 없다.
- 하나의 CatalogProduct는 Category 하나에만 연결되고, ProductVariant는 CatalogProduct 하나에만 속한다.
- `isPrimary = true`인 CatalogProduct Media는 상품당 최대 하나이며, `ACTIVE` Media는 상품당 최대 20개다.
- Media `sortOrder`는 같은 CatalogProduct 안에서 유일하다.
- 외부 상품 식별자는 유형별 형식·체크디지트·CatalogProduct 간 유일성을 만족해야 한다. ISBN만 외부 API 검증을 추가한다.
- 한 번 보관한 Category·CatalogProduct·ProductVariant는 다시 `ACTIVE`로 전환하지 않는다. Category 삭제·보관은 현재 제공하지 않는다.

### 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| `ACTIVE` CatalogProduct | 관리자가 보관 요청 | `ARCHIVED` | ADMIN |
| `ACTIVE` ProductVariant | 관리자가 보관 요청 | `ARCHIVED` | ADMIN |
| `ACTIVE` CatalogProduct Media | 관리자가 삭제 요청 | `ARCHIVED` | ADMIN |
| `SCHEDULED`가 필요한 상태 | 현재 P2에는 사용하지 않음 | - | - |

CatalogProduct 보관은 하위 Variant를 공개·Offer 등록 대상에서 제외하고, 연결된 Offer 비활성화는 P9 정책에 따른다. ProductVariant 보관도 같은 방식으로 P9에 알린다.

## 5. 도메인 간 규칙과 예외 소유권

- P7·P8·P9는 P2의 공개 interface를 통해서만 Category·Variant 검증·조회 결과를 사용한다.
- P9가 상품 검색을 수행할 때 Category 테이블이나 P2 Repository를 직접 조회하지 않고 `findSelfAndDescendantIds` 결과를 사용한다.
- P9가 Offer 등록 전에 Variant를 확인할 때 `findActiveByVariantId`를 사용한다. 보관·미존재 Variant는 같은 등록 불가 결과로 처리한다.
- P12가 발급한 `READY` 상태 `uploadId`만 CatalogProduct Media 연결에 사용한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P7 Admin | 관리자 인증·HTTP 진입점·승인 | [P7 Admin](../p7/p7-admin.md) |
| P8 Seller | Seller 제안과 등록 요청 | [P8 Catalog Requests](../p8/p8-catalog-requests.md) |
| P9 Offer | Offer·Inventory·Offer Media 연결 | [P9 Offer API](../p9/p9-offer.md) |
| P10 Review | 구매 경험 리뷰와 Review Media | [P10 Review](../p10/p10-review.md) |
| P12 Media | uploadId·파일 검증·저장소 | [P12 Media](../p12/p12-media.md) |

외부 도메인의 인증·권한·예외 코드는 해당 도메인 문서의 원본을 사용한다. P2가 보관·미존재를 사용자용 `404`로 추상화하는 경우에만 P2 API 문서에 그 응답을 등록한다.

## 6. API 문서와의 관계

- 정책과 API 문서가 충돌하면 이 문서의 정책을 기준으로 API 문서를 수정한다.
- 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
- P2 고유 `exceptionCode`는 도메인별 prefix를 사용한다. Category는 `CATEGORY-*`, CatalogProduct·ProductVariant 등 Catalog 도메인은 `CATALOG-*`를 사용하며, 이미 사용한 번호를 재사용하지 않는다.
