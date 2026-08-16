# ADR-0008: Catalog 분류 관계와 Offer 리뷰 귀속

- Status: Proposed
- Date: 2026-08-15
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

기존 Catalog 모델은 Category를 CatalogProduct의 단일 `category_id`로 연결하고, Review를 ProductVariant에 귀속하는 방향으로 문서와 V1 스키마가 작성되어 있었다. 이번 결정에서는 단일 대표 Category와 부모 계층 검색을 유지하고, 구매자는 상품 자체가 아니라 판매자의 Offer를 구매하므로 판매자별 구매 경험을 구분하는 부분만 확장한다.

검색 키워드와 ProductType·ItemType은 Amazon식 확장 개념이지만 현재 기본 범위에 포함하지 않는다.

## Decision Drivers

- 상품은 하나의 대표 Category에 연결하고, 부모·하위 Category 계층으로 분류 검색할 수 있어야 한다.
- 동일 Variant를 여러 Seller가 판매할 때 Review를 Seller별 Offer로 구분해야 한다.
- P2·P9·P10의 도메인 경계와 공개 interface를 유지해야 한다.
- 기존 V1 스키마와의 차이를 숨기지 않고 후속 마이그레이션 대상으로 명확히 해야 한다.

## Considered Options

### Option A: Category 단일 연결·ProductVariant 리뷰 유지

현재 스키마는 단일 대표 Category를 표현할 수 있으며, 서로 다른 Seller의 구매 경험이 하나의 Review 목록으로 합쳐지는 문제는 별도로 해결해야 한다.

### Option B: Category N:M 및 Offer 리뷰

상품 분류와 Seller별 구매 경험을 정확히 표현하지만 Category 연결과 Review FK·유니크 제약을 마이그레이션해야 한다.

## Decision

- Category와 CatalogProduct는 `CatalogProduct.categoryId`를 통한 1:N 관계로 정의한다. 상품의 전체 Category 경로는 `parentId`를 따라 계산하고, 하위 Category 검색은 P2가 제공한다.
- Category 생성·수정은 ADMIN이 수행하고, Seller는 P8 등록 요청에서 새 Category를 제안할 수 있다. P2가 검증을 소유하고 P7이 관리자 진입점을 제공한다.
- Review는 구매 당시의 Offer에 귀속한다. 작성 요청은 `orderItemId`로 구매·배송 완료와 구매 당시 Offer를 검증하고, Review 내부에는 `offerId`를 저장한다.
- 고객·Seller 응답에는 내부 CatalogProduct·ProductVariant ID를 포함하지 않는다.

## Consequences

### Positive

- 상품의 대표 Category와 부모 경로를 단순한 단일 FK로 관리할 수 있다.
- 동일 상품을 판매하는 Seller별 Review를 분리할 수 있다.
- 분류·판매 조건·리뷰의 책임 경계가 명확해진다.

### Negative

- 여러 분류 트리를 하나의 상품에 직접 연결하는 기능은 제공하지 않는다. 추가 분류가 필요하면 계층 설계를 먼저 변경해야 한다.
- Review의 `variant_id` FK와 `(user_id, variant_id)` 유니크 제약을 Offer 기준으로 변경해야 한다.
- 상품 상세 화면에서 여러 Offer의 Review를 통합할지 P9 read model이 별도로 결정해야 한다.

### Follow-up

- `catalog_products.category_id`를 대표 Category FK로 유지하고, Category 경로·하위 검색 규칙을 P2에 반영한다.
- `reviews.variant_id`와 기존 중복 제약을 Offer 기준으로 변경하는 Flyway 마이그레이션을 추가한다.
- 기존 Tag 구현은 현재 목표 범위에서 제외하며, 구현 정리 시 별도 제거·마이그레이션을 결정한다.
- P7·P8·P9·P10 API 예시와 검증 테스트를 새 관계에 맞게 갱신한다.

## Evidence

- [P2 Catalog](../requirement/p2/p2-catalog.md)
- [P2 Category](../requirement/p2/p2-category.md)
- [P2 SearchKeyword 심화사항](../requirement/p2/p2-search-keyword.md)
- [P2 ProductType·ItemType 심화사항](../requirement/p2/p2-product-type.md)
- [P10 Review](../requirement/p10/p10-review.md)
- [Domain ERD](../domain-erd.md)
- [V1 schema](../../src/main/resources/db/migration/V1__init_schema.sql)
