# ADR-0017: CatalogProduct와 ProductVariant의 페이지 기반 통합 조회

- Status: Accepted
- Date: 2026-08-30
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

관리자와 Product Manager는 운영 관리와 Offer 등록을 위해 CatalogProduct와 연결된
ProductVariant를 함께 조회한다. 기존 구현에는 CatalogProduct 목록을 페이지 기반으로 조회하는
API와 별도의 커서 기반 조회 API가 함께 존재했으며, Variant 단건 조회 API도 제공하고 있었다.

현재 사용 목적은 내부 관리 및 등록 대상 탐색이며, 대규모 공개 상품 검색이나 빈번한 데이터 변경을
전제로 하지 않는다. 따라서 커서 기반 페이지네이션과 Variant 단건 조회는 현재 요구사항에 비해
구현·테스트·문서 복잡성만 증가시킨다.

## Decision Drivers

- 관리자·Product Manager가 CatalogProduct와 연결된 ProductVariant를 한 번에 확인해야 한다.
- 조회 API를 페이지 기반으로 단순화한다.
- CatalogProduct와 ProductVariant 조회 권한을 일관되게 적용한다.
- 별도의 Variant 조회 API와 커서 상태 관리에 따른 복잡성을 제거한다.
- P9의 고객용 Product API와 P2의 관리·등록용 Catalog 조회를 분리한다.

## Considered Options

### Option A: CatalogProduct 페이지 조회와 Variant 단건·커서 조회 병행

CatalogProduct는 페이지 또는 커서로 조회하고, Variant는 별도 단건 API로 조회한다.

- 장점: 대규모 목록에서 커서 기반 조회를 사용할 수 있고 Variant만 별도로 조회할 수 있다.
- 단점: API·DTO·Repository·테스트가 늘어나며, CatalogProduct와 Variant를 함께 확인하려면 여러
  조회 흐름이 필요하다. 현재 사용 목적에는 과도하다.

### Option B: 페이지 기반 CatalogProduct 통합 조회

CatalogProduct 목록·상세 API가 연결된 ProductVariant 목록을 함께 반환하고, CatalogProduct와
Variant의 별도 단건 조회 API는 제공하지 않는다.

- 장점: 관리자와 Product Manager의 사용 흐름이 한 번의 조회로 완성되고 API 계약과 테스트가
  단순해진다.
- 단점: 매우 큰 목록이나 실시간 변경이 잦은 공개 검색에는 커서 기반 조회보다 적합하지 않을 수
  있다. 해당 요구는 P9 Product API에서 별도로 판단한다.

## Decision

Option B를 선택한다.

- `GET /api/v1/catalog-products`는 `page`, `size`, `keyword`, `categoryId`, `tag`, `sort`와
  관리자용 상태 필터를 사용해 페이지 기반으로 조회한다.
- `GET /api/v1/catalog-products/{catalogProductId}`는 CatalogProduct와 연결된 ProductVariant
  목록을 함께 반환한다.
- 응답의 `variants` 필드에 ProductVariant를 포함하며, 별도의
  `GET /api/v1/product-variants/{variantId}` 및 `/cursor` 조회 API는 제공하지 않는다.
- CatalogProduct와 Variant 조회에는 동일한 관리자·Product Manager 권한 정책을 적용한다.
- Variant 생성·수정·아카이빙 명령 API는 조회 통합과 별개로 유지한다.

## Consequences

### Positive

- 관리·등록 화면이 CatalogProduct 조회 한 번으로 필요한 Variant를 함께 얻는다.
- 커서 payload, codec, 서명 키, 커서 전용 검증 및 관련 테스트가 필요 없어졌다.
- CatalogProduct와 ProductVariant의 조회 권한 및 응답 계약이 일관된다.

### Negative

- CatalogProduct 목록에 연결된 Variant를 함께 조합하는 조회 비용이 발생한다.
- 대규모 공개 검색이나 고빈도 변경 데이터에는 페이지 기반 방식이 적합하지 않을 수 있다.
- Variant만 필요한 호출도 CatalogProduct 조회를 통해야 한다.

### Follow-up

- P2 Catalog와 P2 ProductVariant 문서에서 커서 및 Variant 단건 조회 정의를 제거한다.
- CatalogProduct 조회 통합 테스트에서 목록·상세 응답의 `variants`를 검증한다.
- 향후 P9 Product API에서 대규모 고객용 검색이 필요해지면 별도의 페이지네이션 전략을 결정한다.

## Evidence

- [P2 Catalog](../requirement/p2/p2-catalog.md)
- [P2 ProductVariant](../requirement/p2/p2-product-variant.md)
- [CatalogQueryController.java](../../src/main/java/io/github/metdaisy/amaazon/catalog/presentation/controller/CatalogQueryController.java)
- [CatalogQueryService.java](../../src/main/java/io/github/metdaisy/amaazon/catalog/application/service/catalog/CatalogQueryService.java)
- [CatalogProductQueryIntegrationTest.java](../../src/test/java/io/github/metdaisy/amaazon/catalog/presentation/controller/CatalogProductQueryIntegrationTest.java)
