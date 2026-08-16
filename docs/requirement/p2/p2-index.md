# P2 Catalog 문서 안내

P2는 고객이 구매할 상품의 기준 정보와 분류 체계를 소유한다. 정책과 각 리소스의 데이터·API 계약은 아래 문서에서 정의한다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P2 Policy](p2-policy.md) | 정책 | 범위·책임, 행위자, 업무 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [Catalog API](p2-catalog.md) | 공통 계약 | 리소스 관계, 공개 application interface, 역할별 조회 경계 |
| [Category API](p2-category.md) | 데이터 모델·API | Category 계층, 생성·수정·조회, 제약과 예외 |
| [CatalogProduct API](p2-catalog-product.md) | 데이터 모델·API | 상품 공통 정보, 식별자, attributes, Media, 보관·조회 |
| [ProductVariant API](p2-product-variant.md) | 데이터 모델·API | 구매 단위, attributes, 등록·수정·보관·조회 |
| [ProductType·ItemType](p2-product-type.md) | 심화사항 | 현재 제외한 내부 상품 유형 체계 |
| [SearchKeyword](p2-search-keyword.md) | 심화사항 | 현재 제외한 검색 보조 키워드 |

리소스별 행위자·상태·API가 더 분리되어야 할 때만 `[resource]-[concern].md`를 추가한다. 추가 문서는 이 표에 등록하고 담당 문서에서 범위를 참조한다.

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| Category 원본·계층·검증 | P2 Catalog | [Category API](p2-category.md) |
| CatalogProduct 원본·상태·CatalogProduct Media | P2 Catalog | [CatalogProduct API](p2-catalog-product.md) |
| ProductVariant 원본·상태 | P2 Catalog | [ProductVariant API](p2-product-variant.md) |
| 관리자 HTTP 진입점·승인 처리 | P7 Admin | [P7 Catalog](../p7/p7-catalog.md), [P7 Category](../p7/p7-category.md) |
| Seller 등록 요청 | P8 Seller | [P8 Catalog Requests](../p8/p8-catalog-requests.md) |
| 가격·판매 상태·재고·Offer Media | P9 Offer | [P9 Offer](../p9/p9-offer.md), [P9 Inventory](../p9/p9-inventory.md) |
| 공통 업로드·파일 저장·CDN | P12 Media·공통 인프라 | [P12 Media](../p12/p12-media.md) |
| Review와 Review Media | P10 Review | [P10 Review](../p10/p10-review.md) |

- P2가 소유하는 것은 상품의 기준 정보이며 판매자별 가격·재고·판매 가능 여부가 아니다.
- P7·P8·P9는 P2의 domain·infra 내부 패키지나 Repository를 직접 참조하지 않고 공개 application interface만 사용한다.
- 외부 도메인의 예외를 P2 API 응답에 포함할 때도 코드·메시지·로그 원본은 해당 도메인 문서를 참조한다.
- URI, 성공 응답, 예외 필드, 페이지네이션, 인증은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 3. 문서 작성 순서

1. [P2 Policy](p2-policy.md)에서 범위·책임과 확정 업무 규칙을 정한다.
2. [Catalog API](p2-catalog.md)에서 리소스 관계와 공개 interface를 정한다.
3. Category·CatalogProduct·ProductVariant 문서에서 모델, API, 성공 응답, 예외를 완성한다.
4. 이 문서의 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 이 문서는 안내와 책임 경계만 작성하고 정책·필드·API 계약을 중복하지 않는다.
- 구현 결과가 달라지는 선택 표현을 사용하지 않고 하나의 기본 동작을 확정한다.
- 정책 문서와 리소스 문서가 충돌하면 P2 Policy를 기준으로 리소스 문서를 수정한다.
- 현재 구현 여부는 [current-state.md](../../current-state.md)에서 확인하며, 구현되지 않은 계약을 구현 완료로 표현하지 않는다.
