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

모델은 늘어나지만 각 책임이 명확하고, 초기에는 기본 ProductVariant와 기본 Offer 하나만 생성해 구현 난이도를 제한할 수 있다.

## Decision

Option B를 선택한다.

- `CatalogProduct`는 상품명, 설명, 브랜드, 카테고리, 상품 속성, 전시 미디어를 소유한다.
- `ProductVariant`는 실제 구매 조합과 SKU를 소유한다. 초기 요구사항에서는 기본 ProductVariant 하나를 생성한다.
- `Offer`는 ProductVariant의 가격, 판매 상태, 상품 상태, 판매자 식별자, 배송·Prime 표시 확장 지점을 소유한다. 초기 요구사항에서는 기본 Offer 하나를 생성한다.
- `Inventory`는 Offer의 가용 수량과 차감·복원 규칙을 소유한다.
- 별도 Seller 모듈이 추가되기 전까지 `Offer.sellerId`는 선택값이다. 값이 없으면 플랫폼 기본 Offer로 처리한다.
- 상품 삭제는 물리 삭제보다 보관 상태 전환을 우선하며, 주문·리뷰·관심상품의 참조 이력을 보존한다.

## Consequences

### Positive

- 기본 상품 등록은 단일 ProductVariant·Offer로 단순하게 유지할 수 있다.
- ProductVariant, 복수 판매자, 가격 정책, 재고를 독립적으로 확장할 수 있다.
- 상품 상세 응답에서 전시 정보와 실제 구매 가능 조건을 구분할 수 있다.

### Negative

- 상품 등록과 조회 시 여러 객체를 조합해야 한다.
- 가격과 재고의 일관성을 보장하기 위해 Offer·Inventory 경계와 트랜잭션 규칙을 지켜야 한다.
- Seller 모듈 도입 시 `sellerId`의 nullable 정책을 정리하고 데이터 마이그레이션해야 한다.

### Follow-up

- P2 요구사항의 요청·응답 예시와 예외 코드를 이 모델에 맞춰 유지한다.
- 장바구니·주문은 `CatalogProduct`가 아니라 구매 가능한 ProductVariant·Offer를 참조한다.
- 복수 Offer와 가격 정책을 도입할 때 선택 규칙, 동시성, 멱등성을 별도 ADR로 기록한다.

## Evidence

- [P2 상품·재고 요구사항](../requirement/p2-product.md)
- [아키텍처 문서](../architecture.md)

