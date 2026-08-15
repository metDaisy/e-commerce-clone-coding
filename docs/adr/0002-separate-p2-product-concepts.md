# ADR-0002: P2 상품군·ProductVariant·Offer·재고 책임 분리

- Status: Accepted
- Date: 2026-08-09
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

Amazon과 유사한 상품 화면은 상품 설명, 색상·사이즈 조합, 판매 가격·판매자, 재고 수량을 서로 다른 기준으로 표시한다. 이를 하나의 `CatalogProduct`에 모두 넣으면 ProductVariant 추가, 복수 판매자, 가격 정책, 재고 차감으로 확장할 때 스키마와 API를 다시 설계해야 한다.

현재 프로젝트에는 별도 Seller 모듈이 없고, 초기 구현은 단순해야 한다. 따라서 실제 서비스의 확장성을 고려하면서도 첫 구현의 등록 흐름은 기본 구매 단위 하나로 제한할 필요가 있다.

## Decision Drivers

- 상품 전시 정보와 구매 가능 정보를 분리한다.
- 초기 구현의 등록·조회 흐름을 과도하게 복잡하게 만들지 않는다.
- 색상·사이즈 ProductVariant, 복수 Offer, 판매자, 가격 정책으로 확장할 수 있어야 한다.
- 재고 차감과 복원을 독립적으로 검증할 수 있어야 한다.

## Considered Options

### Option A: 하나의 CatalogProduct에 가격·재고·옵션을 모두 포함

초기 CRUD는 단순하지만 ProductVariant와 복수 판매 Offer를 추가할 때 CatalogProduct 스키마와 API가 크게 변경된다.

### Option B: CatalogProduct, ProductVariant, Offer, Inventory로 분리

모델은 늘어나지만 각 책임이 명확하고, CatalogProduct·ProductVariant 등록과 Seller의 Offer 등록을 단계적으로 구현할 수 있다.

## Decision

Option B를 선택한다.

- `CatalogProduct`는 상품명, 설명, 브랜드, 카테고리, 상품 속성, 전시 미디어를 소유하며 `ADMIN`만 생성·수정·보관한다. 관리자 소유자나 `managerId`는 저장하지 않는다.
- `ProductVariant`는 실제 구매 조합을 소유하고 서버 생성 `variantId`로 식별하며 `ADMIN`만 생성·수정·보관한다.
- `Offer`는 활성 Seller가 ProductVariant를 판매하기 위한 가격·판매 상태·판매자 조건을 소유한다. 하나의 Seller는 같은 ProductVariant에 Offer를 하나만 가진다.
- `Inventory`는 Offer 생성 시 함께 생성되며 Offer별 가용 수량·차감·복원 규칙을 소유한다.
- Seller가 아닌 플랫폼 기본 Offer는 기본 모델에 두지 않는다. 고객 공개 조회는 활성 Seller의 공개 Offer만 조합한다.
- 상품과 판매 제안의 삭제는 물리 삭제보다 보관 상태 전환을 우선하며, CatalogProduct·ProductVariant 보관 시 하위 Offer를 `INACTIVE`로 전환한다.
- 고객·Seller·ADMIN 조회는 서로 다른 목적의 read model을 사용한다. 고객은 공개 Offer 요약, Seller는 자신의 `myOffer`, ADMIN은 보관 상태와 관리 메타데이터를 본다.

## Consequences

### Positive

- CatalogProduct·ProductVariant와 판매자의 Offer 등록을 분리해 각 권한 경계를 명확히 유지할 수 있다.
- ProductVariant, 복수 판매자, 가격 정책, 재고를 독립적으로 확장할 수 있다.
- 상품 상세 응답에서 전시 정보와 실제 구매 가능 조건을 구분할 수 있다.

### Negative

- 상품 등록과 조회 시 여러 객체를 조합해야 한다.
- 가격과 재고의 일관성을 보장하기 위해 Offer·Inventory 경계와 트랜잭션 규칙을 지켜야 한다.
- 기존 `manager_id`와 플랫폼 기본 Offer를 제거하는 스키마·데이터 마이그레이션이 필요하다.

### Follow-up

- P2 요구사항의 요청·응답 예시와 예외 코드를 이 모델에 맞춰 유지한다.
- 장바구니·주문은 `CatalogProduct`가 아니라 구매 가능한 ProductVariant·Offer를 참조한다.
- 복수 Offer와 가격 정책을 도입할 때 선택 규칙, 동시성, 멱등성을 별도 ADR로 기록한다.

## Evidence

- [P2 Catalog 요구사항](../requirement/p2/p2-catalog.md)
- [아키텍처 문서](../architecture.md)

