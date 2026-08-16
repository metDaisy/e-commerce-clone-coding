# P2 SearchKeyword 심화사항

이 문서는 현재 P2 API·데이터 모델에 포함하지 않는 검색 보조 메타데이터를 기록한다. 현재 상품 공통 정보와 검색 조합의 기준은 [CatalogProduct API](p2-catalog-product.md)와 [P9 Marketplace](../p9/p9-marketplace.md)를 따른다.

## 1. 목적과 용어

`SearchKeyword`는 상품명에 없는 동의어·약어·대체 표현으로도 상품을 찾도록 검색 색인을 보조하는 내부 메타데이터다.

예:

```text
상품명: NVIDIA RTX 5080
SearchKeyword: gpu, gaming graphics card, ray tracing
```

| 용어 | 의미 |
|---|---|
| Category | 고객이 탐색하는 공개 상품 분류 |
| ProductType·ItemType | 상품 유형과 외부 분류 체계 |
| `attributes` | 상품 사실·옵션·물류 정보를 담는 CatalogProduct·ProductVariant 메타데이터 |
| SearchKeyword | 검색어 확장을 위한 색인 보조 값 |

SearchKeyword는 고객에게 표시하는 태그가 아니며 CatalogProduct의 사실 속성을 대체하지 않는다.

## 2. 현재 제외 범위

- Entity·테이블·Repository를 만들지 않는다.
- Seller가 직접 입력하는 공개 태그 기능을 제공하지 않는다.
- 고객·Seller·관리자 상품 응답에 검색 키워드를 반환하지 않는다.
- 검색 키워드 검증·관리 API를 제공하지 않는다.
- 현재 P9 검색은 상품명·설명·브랜드·Variant 표시명과 Category 조건을 사용한다.

## 3. 향후 설계 과제

- 동의어·약어·다국어·오탈자 보정 규칙과 금지어 정책
- 상품별 키워드 연결, 색인 반영, 변경 시 재색인 멱등성
- Seller 제안어의 ADMIN 승인·스팸 방지·브랜드 오용 방지
- 키워드 변경 이력과 검색 품질 측정
- 전역 `CatalogKeyword` N:M과 CatalogProduct별 목록 중 하나의 저장 구조

이 문서의 내용은 현재 P2 계약을 변경하지 않는다. 도입 시에는 P2 Policy와 P9 Marketplace의 검색 책임을 함께 갱신한다.
