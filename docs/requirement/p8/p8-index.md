# P8 Seller 문서 안내

P8은 Seller 신청·프로필·Catalog 등록 요청과 판매자 관점의 주문 조회를 정의한다. 업무 정책은 [P8 Seller Policy](p8-policy.md), 리소스 모델과 API 계약은 아래 API 문서에서 정의한다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P8 Seller Policy](p8-policy.md) | 정책 | Seller 범위·책임, User 관계, 역할·상태, 불변식, 도메인 간 규칙 |
| [SellerApplication API](p8-seller-application.md) | 데이터 모델·API | 신청·심사 모델, 신청 생성 API, 예외 매트릭스 |
| [Seller API](p8-seller-profile.md) | 데이터 모델·API | Seller 프로필·상태 이력, 프로필 조회·수정 API |
| [CatalogRegistrationRequest API](p8-catalog-requests.md) | 데이터 모델·API | Catalog 등록 요청, 입력 스냅샷, 판매자 요청 API |
| [SellerOrder API](p8-seller-orders.md) | 조회 모델·API | 판매자 주문 목록·상세 조회와 소유권 필터링 |

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| Seller 신청·프로필·상태 원본 | P8 Seller | [P8 Seller Policy](p8-policy.md), [Seller API](p8-seller-profile.md) |
| Catalog 등록 요청 원본·심사 스냅샷 | P8 Seller | [CatalogRegistrationRequest API](p8-catalog-requests.md) |
| User·역할 집합 | P1 User | [P1 Policy](../p1/p1-policy.md) |
| 관리자 승인·거절·Seller 상태 변경 | P7 Admin | [P7 Access](../p7/p7-access.md) |
| 정식 Catalog 데이터 | P2 Catalog | [P2 Policy](../p2/p2-policy.md) |
| Offer·Inventory·판매 상태 | P9 Offer | [P9 Index](../p9/p9-index.md) |
| 주문 원본·배송 상태 | P5 Order | [P5 Policy](../p5/p5-policy.md) |

- P8은 User·Catalog·Offer·Inventory·Order의 내부 모델과 Repository를 소유하거나 복제하지 않는다.
- 다른 도메인의 예외는 API 응답에 포함할 수 있지만, 원본 코드와 메시지는 해당 도메인 문서를 따른다.
- 공통 URI, 성공 응답, 인증, 페이지네이션과 예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 3. 문서 작성 순서

1. [P8 Seller Policy](p8-policy.md)에서 Seller 생명주기와 권한 규칙을 정한다.
2. 각 리소스 API 문서에서 정책을 만족하는 데이터 모델과 API를 정의한다.
3. API별 성공 응답·P8 예외와 외부 도메인 예외 참조를 완성한다.
4. 리소스와 책임이 추가되면 이 문서의 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 이 문서는 안내와 책임 경계만 작성하고 정책·필드·API 계약을 중복하지 않는다.
- 정책 문서가 API 문서보다 우선하며, API 문서는 정책을 만족하는 구체 계약만 정의한다.
- P2 Catalog, P7 Admin, P9 Offer, P5 Order의 원본 규칙을 P8 문서에 복제하지 않고 링크로 참조한다.
