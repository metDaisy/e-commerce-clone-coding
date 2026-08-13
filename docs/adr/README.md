# Architecture Decision Records

ADR은 되돌리기 어렵거나 여러 모듈의 구현을 제한하는 결정을 배경과 함께 보존한다. 현재 구조 설명은 `../architecture.md`, 구현 상태는 `../current-state.md`, 고민 과정은 `../dev-dairy.md`의 책임이다.

## ADR을 작성할 때

- 합리적인 대안이 둘 이상이며 선택에 장기 비용이 있다.
- 모듈 seam, 의존 방향, 트랜잭션, 이벤트 의미가 바뀐다.
- 데이터 모델, 상태 머신, 보안 또는 일관성 전략을 결정한다.
- 이후 에이전트와 리뷰어에게 같은 이유를 반복해서 설명할 가능성이 크다.

단순 리팩터링, 라이브러리 패치 버전 변경, 코드에서 명백한 구현 세부사항은 ADR로 만들지 않는다.

## 파일과 상태

- 파일명: `NNNN-short-kebab-title.md`
- 번호는 재사용하지 않는다.
- 상태: `Proposed`, `Accepted`, `Rejected`, `Superseded`
- 승인된 ADR은 내용을 덮어써서 역사를 바꾸지 않는다. 결정이 바뀌면 새 ADR을 만들고 기존 문서에 `Superseded by ADR-NNNN`을 기록한다.
- 새 문서는 [template.md](template.md)를 복사해 작성한다.

## 인덱스

| 번호 | 제목 | 상태 | 문서 |
|---|---|---|---|
| ADR-0001 | 요구사항을 공통 인덱스와 도메인 문서로 분리 | Accepted | [0001-split-requirements-by-domain.md](0001-split-requirements-by-domain.md) |
| ADR-0002 | P2 상품군·ProductVariant·Offer·재고 책임 분리 | Accepted | [0002-separate-p2-product-concepts.md](0002-separate-p2-product-concepts.md) |
| ADR-0003 | OAuth provider는 프로젝트 정의 enum으로 관리 | Accepted | [0003-project-defined-oauth-provider.md](0003-project-defined-oauth-provider.md) |
| ADR-0004 | 스키마 외래 키는 Modulith 도메인 경계를 따른다 | Accepted | [0004-schema-fk-follows-modulith-boundaries.md](0004-schema-fk-follows-modulith-boundaries.md) |
| ADR-0005 | 상품 소유권·수명주기·역할별 조회 모델 | Superseded | [0005-product-ownership-lifecycle-and-read-models.md](0005-product-ownership-lifecycle-and-read-models.md) |
| ADR-0006 | Catalog·Offer·Review 요구사항 경계 분리 | Accepted | [0006-split-catalog-offer-and-review-contexts.md](0006-split-catalog-offer-and-review-contexts.md) |

ADR이 추가되면 번호, 제목, 상태, 대체 관계를 이 목록에 기록한다.
