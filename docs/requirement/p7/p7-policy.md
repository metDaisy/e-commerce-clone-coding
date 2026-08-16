# P7 Admin Policy

이 문서는 관리자 기능의 범위·책임과 API에 독립적인 운영 정책을 정의한다. 데이터 모델과 API 계약은 각 관리자 API 문서를 따른다.

## 1. 범위와 책임

### 범위

- `ADMIN` 사용자의 플랫폼 관리 권한과 세션 무효화
- 판매자 신청의 수동 승인·거절과 판매자 상태 관리
- Category·CatalogProduct·ProductVariant 등록 요청의 관리자 심사
- Offer의 관리자 비활성화와 판매자 재활성화 요청 심사
- Outbox·Saga 실패 상태의 운영 조회와 제한된 재시도

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| 사용자와 역할 집합 원본 | P1 User | [P1 User](../p1/p1-user.md) |
| 세션 무효화 | P11 Auth | [P11 Policy](../p11/p11-policy.md), [Session API](../p11/p11-session.md) |
| Seller 신청·프로필·상태 | P8 Seller | [P8 Seller 신청](../p8/p8-seller-application.md), [P8 Seller 프로필](../p8/p8-seller-profile.md) |
| Catalog 원본·검증 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| Offer·Inventory 원본·상태 | P9 Offer | [P9 Offer](../p9/p9-offer.md) |
| Outbox·Saga 상태 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| 관리자 HTTP 진입점 | P7 Admin | [P7 Admin API](p7-admin.md) |

P7은 외부 도메인의 내부 Entity·Repository·서비스를 소유하거나 직접 참조하지 않는다. 각 도메인이 공개한 application interface와 이벤트만 사용한다.

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `ADMIN` | 플랫폼 전체 운영자. P7 관리자 API를 호출하고 심사·상태 변경·재시도를 수행한다. |
| `USER` | 기본 구매자 역할. P7 관리자 API를 호출할 수 없다. |
| `PRODUCT_MANAGER` | 활성 Seller에 연결된 판매자 역할. 플랫폼 전체 관리자가 아니다. |
| 관리자 심사 요청 | Seller·Catalog·Offer 도메인이 원본으로 저장하고 `PENDING` 상태에서 ADMIN의 결정을 기다리는 요청 |
| 수동 승인 | 자동 조건만으로 승인하지 않고 ADMIN이 원본 요청과 현재 상태를 확인한 뒤 승인하는 처리 |
| 비활성화 | 판매 노출이나 판매자 변경을 제한하지만 원본 리소스를 물리 삭제하지 않는 상태 변경 |

## 3. 핵심 업무 규칙

- 모든 P7 API는 `ADMIN` 역할을 요구한다. 인증 실패는 `401`, 권한 부족은 `403`으로 응답한다.
- 판매자 신청은 회사명과 연락처만 받는 간단한 신청이며, MVP에서는 서류·사업자등록번호를 받지 않는다.
- 판매자 신청은 항상 `PENDING`으로 저장한 뒤 ADMIN이 직접 승인·거절한다. 승인 시에만 Seller를 `ACTIVE`로 생성하고 `PRODUCT_MANAGER` 역할을 추가한다.
- Catalog 등록 요청은 P8 `CatalogRegistrationRequest`가 원본이다. P7은 요청을 직접 복제하지 않고 목록·상세를 조회한 뒤 승인 또는 거절한다.
- Offer가 ADMIN에 의해 비활성화되면 판매자에게 사유 코드와 해결 방법을 전달한다. 판매자는 문제 해결 후 활성화 요청을 제출하고 ADMIN의 승인을 받아야 한다.
- P7에는 통합 심사 영속 모델을 두지 않는다. 통합 화면이 필요하면 도메인별 원본 조회를 조합한 조회 DTO만 사용한다.
- 관리자 조치는 처리 주체·처리 시각·결과를 원본 도메인 이력에 기록한다.
- Outbox·Saga 재시도는 실패 상태에서만 허용하며, P6의 멱등성과 상태 전이를 따른다.
- 비밀번호·토큰·OAuth secret·세션 비밀값은 관리자 응답과 화면에 노출하지 않는다.

## 4. 불변식과 상태 전이

### 불변식

- 관리자는 자기 자신의 `ADMIN` 역할을 제거할 수 없다.
- 한 User에게 `PENDING` Seller 신청은 하나만 존재할 수 있다.
- 처리된 Seller·Catalog·Offer 심사 요청은 다시 승인·거절할 수 없다.
- CatalogProduct·ProductVariant 생성 승인만으로 Seller의 Offer·Inventory를 생성하지 않는다.
- `ARCHIVED` Offer는 다시 `ACTIVE`로 전환할 수 없다.
- `FAILED`가 아닌 Outbox 이벤트와 `COMPENSATION_FAILED`가 아닌 Saga는 관리자 재시도 대상이 아니다.

### 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| Seller 신청 `PENDING` | 수동 승인 | `APPROVED` + Seller `ACTIVE` | ADMIN |
| Seller 신청 `PENDING` | 수동 거절 | `REJECTED` | ADMIN |
| Seller `ACTIVE` | 운영상 정지 | `SUSPENDED` | ADMIN |
| Seller `SUSPENDED` | 정지 사유 해소 | `ACTIVE` | ADMIN |
| Offer `ACTIVE` | 정책·정보·권리·안전 문제 | `INACTIVE` | ADMIN |
| Offer `INACTIVE` | 판매자 요청 승인 및 의존 리소스 활성 | `ACTIVE` | ADMIN |
| Outbox `PENDING` | 발행 성공 | `PUBLISHED` | P6 |
| Outbox 발행 실패 | 재시도 한도 초과 | `FAILED` | P6 |
| Saga 보상 진행 중 | 보상 재시도 한도 초과 | `COMPENSATION_FAILED` | P6 |

## 5. 도메인 간 규칙과 예외 소유권

- P1은 역할 집합 변경과 `UserRolesChangedEvent`를 소유한다. P11은 이벤트를 소비해 세션을 무효화한다.
- P8은 Seller 신청·프로필·상태와 Catalog 등록 요청을 소유한다. P7은 공개 application interface로 승인·거절을 요청한다.
- P2는 Category·CatalogProduct·ProductVariant의 생성·검증·보관을 소유한다.
- P9는 Offer·Inventory의 상태·가격·재고를 소유한다.
- P6는 Outbox·Saga의 발행·보상 상태를 소유한다.
- 외부 도메인 예외를 P7의 관리자 오류로 변환하는 경우, 변환 이유와 원본 예외 코드를 API 문서에 기록한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P1 User | 역할 집합 조회·변경, 역할 변경 이벤트 | [P1 User](../p1/p1-user.md) |
| P2 Catalog | Category·CatalogProduct·Variant 검증·생성 | [P2 Catalog](../p2/p2-catalog.md) |
| P6 Infrastructure | Outbox·Saga 상태와 재시도 | [P6 Infrastructure](../p6/p6-infrastructure.md) |
| P8 Seller | Seller 신청·상태·Catalog 등록 요청 | [P8 Seller Policy](../p8/p8-policy.md) |
| P9 Offer | Offer 활성·비활성·Inventory 상태 | [P9 Offer](../p9/p9-offer.md) |
| P11 Auth | 역할 변경 후 세션 무효화 | [Session API](../p11/p11-session.md) |

## 6. API 문서와의 관계

- 관리자 공통 URI·권한·오류는 [P7 Admin API](p7-admin.md)에서 정의한다.
- 기능별 데이터 모델·요청·성공 응답·예외 매트릭스는 각 리소스 API 문서에서 정의한다.
- 이 정책과 API 문서가 충돌하면 이 정책을 기준으로 API 문서를 수정한다.
- 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
