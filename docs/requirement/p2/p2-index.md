# P2 Index (Catalog)

P2는 고객이 구매할 상품의 기준 정보인 카탈로그를 정의한다. 상품의 공통 정보는 `CatalogProduct`가, 고객이 선택하는 실제 구매 단위는 `ProductVariant`가 관리한다. `Category`는 카탈로그 상품의 분류 체계를 제공한다.

## 1. 범위

- `Category`: 카탈로그 상품의 계층형 분류
- `CatalogProduct`: 상품명, 설명, 브랜드, 식별자, 동적 속성 등 상품 공통 정보
- `ProductVariant`: 색상·용량 등 선택 가능한 상품 변형과 실제 구매 단위
- `Media`: CatalogProduct에 연결되는 상품 이미지 및 미디어
- ProductType·ItemType, SearchKeyword: 현재 구현에서 제외한 심화 설계

P2는 상품의 기준 정보와 조회 인터페이스를 소유한다. 판매자별 판매 가격·재고·판매 상태는 P9 Offer 및 Inventory가 소유하고, 판매자 카탈로그 등록 요청은 P8에서 처리한다. 관리자용 등록·수정 진입점은 P7에서 제공한다.

## 2. 도메인 관계

```text
Category 1 : N CatalogProduct
Category 1 : N Category (parent-child)
CatalogProduct 1 : N ProductVariant
ProductVariant 1 : N Offer       (P9)
Offer          1 : 1 Inventory    (P9)
Offer          1 : N Review       (P10)
```

CatalogProduct와 ProductVariant의 식별자는 서버가 생성한다. 고객·판매자 응답에는 내부 식별자를 직접 노출하지 않고, 필요한 경우 P2의 공개 조회 인터페이스를 통해 검증한다.

## 3. 처리 흐름

```text
관리자 Category 등록·수정
        ↓
관리자 CatalogProduct 등록·수정
        ↓
CatalogProduct에 ProductVariant 등록
        ↓
P8 판매자 등록 요청 / P9 Offer·Inventory 연결
        ↓
P9 상품 화면 및 주문 화면에서 카탈로그 정보 조회
```

Category와 CatalogProduct, ProductVariant의 생성·수정·보관 정책은 관리자 권한을 기준으로 한다. P2는 Offer, Inventory, Review를 직접 생성하거나 고객 화면의 상품 검색 결과를 조합하지 않는다.

## 4. 문서 구성

### 기본 요구사항

- [P2 Catalog](p2-catalog.md): P2 범위, 소유권, 도메인 관계, 공개 application interface
- [P2 Category](p2-category.md): Category 계층, 생성·수정, CatalogProduct 연결
- [P2 CatalogProduct](p2-catalog-product.md): 상품 공통 정보, 동적 attributes, Media, 보관·조회
- [P2 ProductVariant](p2-product-variant.md): 상품 변형, 구매 단위, 등록·수정·조회

### 심화사항

- [P2 ProductType·ItemType](p2-product-type.md): 상품 유형과 카탈로그 분류 용어의 향후 설계
- [P2 SearchKeyword](p2-search-keyword.md): 검색 보조 키워드와 효율적인 검색의 향후 설계

## 5. 다른 도메인과의 경계

| 도메인 | P2와의 관계 |
|---|---|
| P7 Admin | Category·CatalogProduct·ProductVariant 관리 진입점 제공 |
| P8 Seller | 판매자 카탈로그 등록 요청을 P2에 전달 |
| P9 Offer | ProductVariant에 판매자별 가격·재고·판매 상태 연결 |
| P10 Review | 구매 가능한 Offer 및 ProductVariant에 리뷰 연결 |
| P5 Order | 주문 시점의 상품·Variant 표시 정보 조회 |

공통 응답 형식과 HTTP 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
