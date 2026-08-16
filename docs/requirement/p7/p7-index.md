# P7 Admin 문서 안내

P7은 관리자 전용 관리 기능과 운영 API 진입점을 정의한다. 업무 데이터의 원본과 상태는 각 도메인이 소유하고, P7은 관리자 권한으로 공개 application interface를 호출한다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P7 Policy](p7-policy.md) | 정책 | 범위·책임, 행위자, 업무 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [P7 Admin API](p7-admin.md) | 공통 관리자 API | 관리자 권한, API 목록, 공통 예외, 원본 요청 조회 원칙 |
| [P7 Access API](p7-access.md) | 권한·Seller API | 역할 변경, 세션 무효화, Seller 신청·상태 관리 |
| [P7 Catalog Policy](p7-catalog.md) | Catalog 관리자 정책 | Catalog 책임 경계와 Category·등록 요청 심사 규칙 |
| [P7 Category API](p7-category.md) | Category API | 관리자 Category 생성·수정 |
| [P7 CatalogProduct API](p7-catalog-products.md) | CatalogProduct 조회 API | 관리자 CatalogProduct·ProductVariant 목록 |
| [P7 Catalog Request API](p7-catalog-requests.md) | CatalogRegistrationRequest API | Category·CatalogProduct·Variant 요청 조회·승인·거절 |
| [P7 Offer API](p7-offer.md) | Offer 운영 API | Offer 비활성화와 재활성화 요청 심사 |
| [P7 Operations API](p7-operations.md) | 운영 인프라 API | Outbox·Saga 실패 조회와 제한된 재시도 |

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| 관리자 권한·HTTP 진입점 | P7 Admin | [P7 Policy](p7-policy.md), [P7 Admin API](p7-admin.md) |
| User·Role 원본과 역할 변경 이벤트 | P1 User | [P1 User](../p1/p1-user.md) |
| Seller 신청·Seller 상태·Catalog 요청 원본 | P8 Seller | [P8 Seller Policy](../p8/p8-policy.md) |
| Category·CatalogProduct·ProductVariant 원본 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| Offer·Inventory 원본 | P9 Offer | [P9 Offer](../p9/p9-offer.md) |
| Outbox·Saga 상태 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| 세션 무효화 | P11 Auth | [Session API](../p11/p11-session.md) |
| 쿠폰·주문·배송 원본 | P4·P5 | [P4 Policy](../p4/p4-policy.md), [Coupon API](../p4/p4-coupon.md), [P5 Order](../p5/p5-index.md) |

- P7은 다른 도메인의 내부 모델·Repository·서비스 구현을 소유하지 않는다.
- 관리자 심사 요청은 각 도메인의 원본 모델을 직접 조회한다. 통합 영속 모델은 두지 않는다.
- 공통 URI, 성공 응답 원칙, 예외 응답 필드, 페이지네이션, 인증은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 3. 문서 작성 순서

1. [P7 Policy](p7-policy.md)에서 관리자 범위와 확정 업무 규칙을 정한다.
2. [P7 Admin API](p7-admin.md)에서 공통 권한과 API 그룹을 등록한다.
3. 각 리소스 API에서 도메인 모델·요청·성공 응답·예외를 정의한다.
4. 이 문서의 문서 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 정책은 [P7 Policy](p7-policy.md)에만 작성하고 API 문서와 중복하지 않는다.
- P7이 소유하지 않는 모델의 필드는 식별자와 공개 계약만 참조한다.
- 구현 결과가 달라질 수 있는 선택 표현 대신 기본 동작 하나를 확정한다.
- 관리자 화면을 위해 원본 요청을 P7에 복제하지 않는다.
