# P2 ProductType·ItemType (심화사항)

## 1. 목적

Amazon식 카탈로그는 고객용 Browse Category 외에 상품 유형을 표현하는 내부 분류를 사용할 수 있다. 이 문서는 향후 ProductType과 ItemType을 도입할 때의 개념만 정의하며, 현재 구현 계약은 아니다.

## 2. 용어

### ProductType

플랫폼이 관리하는 안정적인 상품 유형 코드다.

```text
graphics_card
mobile_phone
wireless_headphone
```

ProductType은 상품이 어떤 종류인지 표현하고, 향후 attributes 스키마·등록 검증·검색 색인을 선택하는 기준이 될 수 있다.

### ItemType

특정 마켓플레이스나 카탈로그 분류 체계에서 상품을 더 구체적인 위치에 배치하기 위한 분류 용어다.

```text
Computer Graphics Cards
Cell Phones
Wireless Headphones
```

ItemType은 고객용 Category와 유사해 보일 수 있지만, 고객 탐색 계층 자체가 아니라 상품 등록·분류·검색 배치를 위한 내부 값으로 취급한다.

## 3. Category와의 관계

```text
Browse Category
→ 고객이 탐색하는 공개 계층

ProductType
→ 플랫폼이 정의한 상품 종류

ItemType
→ 외부·마켓플레이스 분류 체계에 맞춘 내부 배치 용어
```

하나의 CatalogProduct가 하나의 대표 Browse Category에 연결되고 부모 경로로 분류되는 것과 ProductType·ItemType의 개수·관계는 별개의 문제다. ProductType·ItemType을 도입하더라도 현재의 단일 Category 연결을 대체하지 않는다.

## 4. 현재 제외 범위

- ProductType·ItemType Entity와 테이블을 만들지 않는다.
- ADMIN ProductType 관리 API를 만들지 않는다.
- Seller 등록 요청에서 ProductType·ItemType을 필수로 받지 않는다.
- ProductType별 attributes 스키마를 강제하지 않는다.
- Category 생성 시 ProductType·ItemType을 함께 생성하지 않는다.

## 5. 향후 설계 과제

- ProductType과 ItemType의 소유자·버전·마켓플레이스 범위
- ProductType별 attributes 스키마와 기존 동적 attributes의 공존 방식
- 하나의 CatalogProduct에 ProductType·ItemType을 하나만 연결할지 여부
- 외부 분류 체계와의 매핑·동기화 정책
- Seller 제안과 ADMIN 승인 절차
