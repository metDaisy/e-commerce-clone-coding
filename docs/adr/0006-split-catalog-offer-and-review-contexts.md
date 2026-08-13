# ADR-0006: Catalog·Offer·Review 요구사항 경계 분리

- Status: Accepted
- Date: 2026-08-13
- Deciders: 사용자와 Codex
- Supersedes: ADR-0005의 조회 모델·P 번호 경계 결정
- Superseded by: 없음

## Context

기존 P2 문서에는 Category, CatalogProduct, ProductVariant, 상품 Media, Offer, Inventory, 고객용 상품 검색, Review가 함께 들어 있었다. 이 구성은 상품 화면을 설명하기에는 편하지만, Catalog 메타데이터·판매 조건·구매 이력 기반 콘텐츠의 소유권과 모듈 의존 방향을 흐린다.

특히 P2가 Offer를 직접 조합하면 Offer가 CatalogProduct의 Variant를 검증하는 방향과 순환 의존이 생길 수 있다. 또한 `/api/v1/catalog-products/{catalogProductId}`는 고객용 상품 상세와 판매자가 Offer 등록 전에 확인하는 카탈로그 조회를 동시에 표현하고 있었다.

## Decision Drivers

- 도메인별 aggregate와 변경 주체를 명확히 분리한다.
- Catalog와 Offer 사이의 의존 방향을 단방향으로 유지한다.
- 고객에게 제공하는 조합 상품 정보와 판매자용 카탈로그 정보를 분리한다.
- Review의 구매·배송 완료 자격과 Review Media를 Catalog에서 분리한다.
- API URI만 보아도 조회 대상과 권한을 구분할 수 있어야 한다.

## Considered Options

### Option A: P2 Product 문서에 모든 상품 관련 기능 유지

문서 탐색은 단순하지만 P2가 Offer·Inventory·Review의 책임까지 갖게 되고, 구현 모듈과 API 소유권이 불명확해진다.

### Option B: Catalog·Offer·Review를 독립 문서로 분리

문서와 공개 seam이 늘어나지만 각 aggregate의 소유권과 의존 방향을 명확히 할 수 있다.

## Decision

Option B를 선택한다.

- **P2 Catalog**는 Category, CatalogProduct, ProductVariant, 상품용 Media, 판매자·관리자 카탈로그 조회를 소유한다. CatalogProduct·ProductVariant·상품용 Media의 생성·수정·보관은 `ADMIN`만 수행한다.
- **P8 Seller**는 Seller 신청·승인·프로필과 판매자 주문 조회만 소유한다. Offer·Inventory는 소유하지 않는다.
- **P9 Offer & Marketplace**는 Offer, Inventory, 가격·판매 상태, 판매자의 Offer 등록 흐름, 고객용 상품 검색·상세를 소유한다. P9가 P2의 공개 Catalog interface를 호출해 Variant를 검증하고, P2는 P9를 직접 호출하지 않는다.
- **P10 Review**는 Review와 리뷰 Media, 구매·배송 완료 자격 검증을 소유한다. 고객용 상품 상세는 P10의 Review 요약만 조합한다.
- `GET /api/v1/catalog-products/{catalogProductId}`는 `PRODUCT_MANAGER`이면서 `Seller.status = ACTIVE`인 판매자 전용 CatalogProduct·Variant 조회다. Offer·가격·재고·Review는 포함하지 않는다.
- 고객용 조합 상세는 `GET /api/v1/product/{catalogProductId}`, 고객용 검색은 `GET /api/v1/product/search`, 리뷰 목록은 `GET /api/v1/product/{catalogProductId}/reviews`에서 제공한다.
- `GET /api/v1/categories`는 전체 트리를 반환한다. Category가 설명·SEO·랜딩 콘텐츠를 갖기 전까지 `GET /api/v1/categories/{categoryId}`는 추가하지 않는다.
- 관리자 전용 변경 API는 `/api/v1/admin` 아래에 둔다. 판매자 전용 Offer 흐름은 `/api/v1/seller` 아래에 둔다.

## Consequences

### Positive

- CatalogProduct·ProductVariant, Offer·Inventory, Review의 변경 주체가 분명해진다.
- P9 → P2, P10 → P2/P5 방향의 공개 interface를 설계할 수 있어 순환 의존을 피할 수 있다.
- 고객·판매자·관리자 응답을 목적별로 분리할 수 있다.
- P2를 먼저 구현하면서 P9 Offer와 P10 Review를 후속 단계로 독립 구현할 수 있다.

### Negative

- P2·P9·P10 문서 사이를 오가야 하며, 고객용 상세는 여러 도메인의 read model을 조합해야 한다.
- 기존 P2·P8의 Offer·Review API와 파일 경로를 새 문서로 이동하는 작업이 필요하다.
- `catalog::api`, `offer::api`, `review::api`의 공개 interface와 이벤트 계약을 별도로 정의해야 한다.

### Follow-up

- P2 문서를 `p2-catalog.md`로 유지하고 P9·P10 문서를 요구사항 인덱스에 등록한다.
- 기존 P8 Offer·Inventory 절과 기존 P2 Review·고객 검색 절을 제거하고 새 문서로 이동한다.
- `/api/v1/catalog-products/{catalogProductId}`의 `PRODUCT_MANAGER + ACTIVE Seller` 권한과 고객용 P9 상세 API를 각각 테스트한다.
- 구현 시 P9의 Catalog 참조와 P10의 Order 참조를 Modulith 공개 interface로 제한한다.

## Evidence

- [P2 Catalog 요구사항](../requirement/p2-catalog.md)
- [P8 Seller 요구사항](../requirement/p8-seller.md)
- [P9 Offer & Marketplace 요구사항](../requirement/p9-offer.md)
- [P10 Review 요구사항](../requirement/p10-review.md)
- [아키텍처 문서](../architecture.md)
