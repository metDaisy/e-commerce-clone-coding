# ADR-0005: 상품 소유권·수명주기·역할별 조회 모델

- Status: Superseded
- Date: 2026-08-13
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: ADR-0006

## Context

CatalogProduct와 ProductVariant는 여러 판매자가 공유하는 카탈로그와 구매 단위다. 판매자가 이를 직접 생성·수정하면 동일 상품의 중복 등록과 SKU·상품 정보 불일치가 발생하고, `managerId`를 소유자로 저장하면 다른 판매자가 같은 상품에 Offer를 등록하는 모델과 충돌한다. 또한 관리자, 판매자, 고객은 같은 상품을 조회하더라도 필요한 정보와 노출해도 되는 정보가 다르다.

상품과 판매 제안의 삭제 의미도 분리해야 한다. 상품 이력을 보존하면서 공개 노출과 구매를 막아야 하고, 상위 CatalogProduct가 보관될 때 하위 Offer가 계속 활성 상태로 남아서는 안 된다.

## Decision Drivers

- CatalogProduct·ProductVariant의 공통 정보와 SKU 일관성
- 판매자 간 데이터 격리와 최소 권한
- 주문·리뷰·감사 이력의 보존
- 고객 응답의 개인정보·정확한 재고 수량·관리 정보 노출 방지
- 페이지 기반 관리자·판매자 조회와 커서 기반 고객 검색의 목적 분리
- 향후 스키마 마이그레이션과 모듈 경계의 명확성

## Considered Options

### Option A: 판매자가 CatalogProduct·ProductVariant를 소유

판매자별 `managerId`를 저장하고 자신의 상품만 수정한다. 초기 등록은 단순하지만 동일 상품의 중복 등록, SKU 충돌, 판매자 간 공유 상품 모델의 복잡성이 커진다.

### Option B: ADMIN이 카탈로그를 소유하고 판매자는 Offer를 소유

ADMIN만 CatalogProduct·ProductVariant·Media를 생성·수정·보관한다. 활성 Seller인 `PRODUCT_MANAGER`는 기존 ProductVariant를 조회하고 자신의 Offer·Inventory만 관리한다. 카탈로그의 단일 기준을 유지하는 대신 판매자가 Offer를 등록하기 전에 카탈로그를 검색해야 한다.

### Option C: 모든 역할이 같은 검색 응답을 사용

API를 단순하게 유지할 수 있지만 고객에게 관리자 상태·정확한 재고·판매자 간 정보가 노출되거나, 판매자 화면에서 경쟁 Offer 정보가 노출될 수 있다.

## Decision

Option B를 선택하고 Option C를 사용하지 않는다.

- CatalogProduct와 ProductVariant의 생성·수정·보관은 `ADMIN`만 수행한다. Media 등록·수정·보관도 동일하게 `ADMIN` 전용이다.
- `CatalogProduct`에는 관리자 소유자나 `managerId`를 저장하지 않는다. 모든 ADMIN이 모든 CatalogProduct·ProductVariant를 관리한다.
- `PRODUCT_MANAGER`는 활성 Seller를 가진 User를 뜻한다. CatalogProduct·ProductVariant를 변경하지 않고, 자신의 Offer와 Offer에 종속된 Inventory만 생성·수정·보관·조정한다.
- Offer 보관은 물리 삭제가 아닌 `status = ARCHIVED`이며 다시 활성화할 수 없다. Offer의 활성·비활성은 소유 Seller와 ADMIN이 수행하되, 보관된 Offer는 예외다.
- CatalogProduct 또는 ProductVariant가 보관되면 하위 Offer는 `INACTIVE`가 된다. 보관된 상품·Variant·Offer는 고객 검색과 구매 대상에서 제외한다. 이 처리는 멱등적이어야 한다.
- 관리자 변경 이력은 요청 본문의 `adminId`가 아니라 인증된 ADMIN을 기준으로 기록한다. 이력에는 대상, ADMIN, 변경 내용, 시각을 포함하며 일반 고객·판매자 응답에는 포함하지 않는다.
- 고객 검색은 `/api/v1/product/search`의 커서 기반 CatalogProduct 결과로 제공한다. 하위 Variant와 대표 공개 Offer를 포함하되 정확한 Inventory 수량, 비활성 Offer, 감사 정보는 제외한다.
- 판매자 카탈로그 조회는 `/api/v1/seller/catalog-products`의 페이지 기반 결과로 제공하며, 경쟁 판매자 정보 대신 자신의 `myOffer`만 반환한다.
- 관리자 카탈로그 조회는 `/api/v1/admin/catalog-products`의 페이지 기반 결과로 제공하며, 보관 상태와 관리용 Variant 정보를 포함한다. `managerId`는 반환하지 않는다.

## Consequences

### Positive

- 상품 메타데이터와 판매 조건의 소유권이 분리되어 다중 판매자 Offer를 안전하게 추가할 수 있다.
- SKU·상품 설명·Media의 단일 관리 주체가 명확해진다.
- 보관과 Offer 비활성화를 통해 이력을 보존하면서 공개·구매 상태를 차단할 수 있다.
- 고객·판매자·관리자에게 필요한 정보만 제공해 조회 권한과 응답 책임이 분리된다.

### Negative

- 판매자는 Offer 등록 전에 CatalogProduct·ProductVariant를 검색해야 한다.
- 고객 검색은 CatalogProduct, ProductVariant, Offer, Inventory를 조합하는 read model이 필요하다.
- 기존 `catalog_products.manager_id`와 플랫폼 기본 Offer 정책을 제거하는 데이터·스키마 마이그레이션이 필요하다.
- 상위 상품 보관과 하위 Offer 비활성화 사이의 이벤트·트랜잭션 일관성을 보장해야 한다.

### Follow-up

- `manager_id` 제거와 Offer `seller_id NOT NULL`, 보관 상태 컬럼을 새 Flyway 마이그레이션으로 반영한다.
- CatalogProduct·ProductVariant·Offer 보관 및 상위 보관 연계 처리에 대한 권한·멱등성 테스트를 추가한다.
- P2·P7·P9·P10에 정의한 요청·응답 JSON과 실제 공개 API의 필드·페이지 계약을 일치시킨다.
- 구현 시 P9·P10이 P2와 P5의 공개 application interface를 사용하는 방향과 Modulith 허용 의존성을 확정한다.

## Evidence

- [P2 Catalog 요구사항](../requirement/p2-catalog.md)
- [P7 관리자 요구사항](../requirement/p7-admin.md)
- [P8 Seller 요구사항](../requirement/p8-seller.md)
- [P9 Offer & Marketplace 요구사항](../requirement/p9-offer.md)
- [P10 Review 요구사항](../requirement/p10-review.md)
- [도메인 용어집](../domain-glossary.md)
- [아키텍처 문서](../architecture.md)
- [ADR-0002 상품 개념 분리](0002-separate-p2-product-concepts.md)
