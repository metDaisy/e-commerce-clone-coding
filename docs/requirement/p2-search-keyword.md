# P2 SearchKeyword (심화사항)

## 1. 목적

SearchKeyword는 고객이 상품명에 사용하지 않은 동의어·약어·대체 표현으로도 상품을 찾을 수 있도록 검색 색인을 보조하는 내부 메타데이터다.

예:

```text
상품명: NVIDIA RTX 5080
SearchKeyword: gpu, gaming graphics card, ray tracing
```

SearchKeyword는 다음과 다르다.

- Category: 고객이 탐색하는 공개 상품 분류
- ProductType·ItemType: 상품 유형과 등록 속성 체계를 나타내는 내부 분류
- `attributes`: 상품의 사실·옵션·물류 정보를 담는 정형·비정형 메타데이터

## 2. 현재 범위

SearchKeyword는 현재 기본 구현 범위에서 제외한다.

- 별도 Entity·테이블을 만들지 않는다.
- Seller가 직접 입력하는 공개 태그 기능을 제공하지 않는다.
- 고객 상품 응답에 검색 키워드를 반환하지 않는다.
- 검색 키워드 검증·관리 API를 제공하지 않는다.
- 현재 P9 검색은 상품명·설명·브랜드·Variant 표시명과 Category 조건을 사용한다.

## 3. 향후 고려사항

향후 검색 품질이 필요해지면 다음을 심화 설계한다.

- 동의어·약어·다국어·오탈자 보정 규칙
- 검색어의 상품별 연결과 검색 색인 반영
- Seller 제안어의 ADMIN 승인·스팸 방지
- 검색어 노출 금지어·브랜드 오용·경쟁사 키워드 정책
- 키워드 변경 이력과 색인 재생성

저장 구조를 전역 CatalogKeyword N:M으로 할지 CatalogProduct별 검색어 목록으로 할지는 이 심화 단계에서 결정한다.
