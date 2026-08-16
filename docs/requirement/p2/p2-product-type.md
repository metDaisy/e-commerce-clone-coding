# P2 ProductType·ItemType 심화사항

이 문서는 현재 P2 API·데이터 모델에 포함하지 않는 내부 상품 유형 개념을 기록한다. 현재 확정 계약은 [P2 Policy](p2-policy.md), 상품 분류 API는 [Category API](p2-category.md)를 따른다.

## 1. 목적

Amazon식 카탈로그는 고객 탐색용 Browse Category와 별도로 상품 유형과 외부 분류 체계를 사용할 수 있다. ProductType과 ItemType은 현재 구현 대상이 아니며, 도입 전 소유권·버전·검증 규칙을 확정해야 한다.

## 2. 용어

| 용어 | 의미 | 예 |
|---|---|---|
| `ProductType` | 플랫폼이 관리하는 안정적인 상품 종류 코드 | `graphics_card`, `mobile_phone` |
| `ItemType` | 마켓플레이스·외부 카탈로그 체계에 맞춘 내부 배치 용어 | `Computer Graphics Cards` |
| Browse Category | 고객이 탐색하는 공개 계층. 현재 P2가 소유 | `전자기기 > 컴퓨터 > 그래픽카드` |

ProductType은 향후 attributes 스키마·등록 검증·검색 색인을 선택하는 기준이 될 수 있다. ItemType은 고객 탐색 계층을 대체하지 않고 외부 분류와의 매핑에 사용한다.

## 3. Category와의 관계

```text
Browse Category → 고객 탐색용 공개 계층
ProductType     → 플랫폼이 정의한 상품 종류
ItemType        → 외부·마켓플레이스 분류 체계의 내부 배치 값
```

하나의 CatalogProduct가 대표 Browse Category 하나에 연결되는 현재 정책은 ProductType·ItemType 도입 후에도 유지한다. ProductType·ItemType의 개수·관계는 별도 설계 대상이다.

## 4. 현재 제외 범위

- Entity·테이블·Repository를 만들지 않는다.
- ADMIN 관리 API를 만들지 않는다.
- Seller 등록 요청에서 필수 입력으로 받지 않는다.
- ProductType별 attributes 스키마를 강제하지 않는다.
- Category 생성·CatalogProduct 생성 시 함께 생성하거나 자동 추론하지 않는다.
- 현재 CatalogProduct 응답·검색 조건·주문 스냅샷에 포함하지 않는다.

## 5. 향후 설계 과제

- ProductType·ItemType의 소유자, 버전, 적용 마켓플레이스와 유효 기간
- ProductType별 attributes 스키마와 현재 동적 attributes의 공존 방식
- CatalogProduct와의 연결 개수·필수 여부·변경 이력
- 외부 분류 체계와의 매핑·동기화 실패 처리
- Seller 제안과 ADMIN 승인 절차
