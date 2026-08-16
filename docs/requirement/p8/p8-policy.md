# P8 Seller Policy

이 문서는 Seller 신청·프로필·상태와 판매자 관점 기능의 API 독립적인 업무 정책을 정의한다. 데이터 모델과 API 계약은 각 리소스 API 문서를 따른다.

## 1. 범위와 책임

### 범위

- 기존 `User`가 Seller가 되기 위한 신청과 승인된 Seller 프로필을 관리한다.
- `SellerApplication`, `SellerApplicationReview`, `Seller`, `SellerStatusHistory`의 원본과 생명주기를 소유한다.
- 판매자의 Catalog 등록 요청 원본과 심사 당시 입력 스냅샷을 소유한다.
- 판매자 관점의 주문 조회와 Seller 소유권 필터링을 제공한다.

### 범위 밖

- User 프로필·역할 저장은 P1 User가 소유한다.
- 관리자 승인·거절·Seller 상태 변경 진입점은 P7 Admin이 소유한다.
- Category·CatalogProduct·ProductVariant의 정식 데이터와 검증은 P2 Catalog가 소유한다.
- Seller별 Offer·Inventory·판매 상태는 P9 Offer가 소유한다.
- 주문·배송 원본과 상태는 P5 Order가 소유한다.

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Seller 신청·심사 원본 | P8 Seller | [SellerApplication API](p8-seller-application.md) |
| Seller 프로필·상태 이력 | P8 Seller | [Seller API](p8-seller-profile.md) |
| Catalog 등록 요청 원본·스냅샷 | P8 Seller | [CatalogRegistrationRequest API](p8-catalog-requests.md) |
| 관리자 심사·Seller 상태 변경 진입점 | P7 Admin | [P7 Access](../p7/p7-access.md), [P7 Policy](../p7/p7-policy.md) |
| 정식 Catalog 생성·검증 | P2 Catalog | [P2 Policy](../p2/p2-policy.md) |
| Offer·Inventory와 판매 상태 | P9 Offer | [P9 Index](../p9/p9-index.md) |
| 주문 원본·배송 상태 | P5 Order | [P5 Policy](../p5/p5-policy.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `User` | 인증 주체이자 Seller 신청자. Seller와 별도의 구매자 프로필이다. |
| `Seller` | 승인된 판매자 프로필. 하나의 User에 최대 하나만 연결한다. |
| `SellerApplication` | Seller 생성 전 신청 원본. User당 여러 건을 보존할 수 있다. |
| `SellerApplicationReview` | 신청 승인·거절 결과의 불변 이력. |
| `PRODUCT_MANAGER` | `ACTIVE Seller`의 판매자 변경 기능을 위한 User 역할. |
| `ACTIVE` Seller | Seller API의 전체 기능을 사용할 수 있는 상태. |
| `SUSPENDED` Seller | 변경 기능은 차단되지만 본인 Seller 프로필과 P9 Offer 목록·상세 조회는 가능한 상태. |
| `ADMIN` | P7 관리자 API를 통해 신청과 Seller 상태를 심사·운영하는 행위자. |

## 3. 핵심 업무 규칙

1. User와 Seller의 관계는 `User 1 : 0..1 Seller`다. `Seller.sellerId`는 별도 식별자이며 `Seller.userId`는 유일해야 한다.
2. User와 SellerApplication의 관계는 `User 1 : 0..N SellerApplication`이다. 단, `PENDING` 신청은 User당 하나만 허용한다.
3. 신청 시 Seller를 생성하지 않는다. 신청 승인 시에만 `ACTIVE Seller`를 생성하고 `PRODUCT_MANAGER`를 추가한다.
4. 신청 거절은 Seller를 생성하지 않으며, 거절된 신청은 보존하고 재신청은 새로운 SellerApplication으로 저장한다.
5. `ACTIVE` Seller는 P8의 프로필 수정·Catalog 등록 요청·판매자 주문 조회와 P9의 Offer·Inventory 변경 기능을 사용할 수 있다.
6. `SUSPENDED` Seller는 본인 Seller 프로필 조회와 본인 Offer 목록·상세 조회만 사용할 수 있다. Catalog 등록 요청과 주문 조회를 포함한 변경·업무 API는 사용할 수 없다.
7. Seller가 `SUSPENDED`가 되면 P9는 `ARCHIVED`가 아닌 모든 Offer를 `INACTIVE`로 변경한다. Offer·Inventory는 삭제하지 않으며, 재활성화 시 Offer를 자동 복구하지 않는다.
8. 인증 주체의 `userId`와 Seller의 `userId`로 소유권을 확인하며, `sellerId`·`requestedByUserId`를 클라이언트 요청 본문으로 받지 않는다.

## 4. 불변식과 상태 전이

### 불변식

- `Seller.userId`는 하나의 Seller에만 연결된다.
- Seller에는 신청·심사 원본과 민감한 신청 증빙 원문을 저장하지 않는다.
- Seller 상태는 `ACTIVE`, `SUSPENDED`만 사용한다. `INACTIVE`는 Offer 상태다.
- SellerApplication의 처리 상태는 `PENDING`, `APPROVED`, `REJECTED`다.
- 처리된 신청·심사 이력·Seller 상태 이력은 수정·삭제하지 않는다.
- `PRODUCT_MANAGER`의 자동 제거는 `ACTIVE → SUSPENDED`가 정상 처리된 경우에만 수행한다.
- Offer 비활성화·재고 부족·Catalog 요청 거절·주문 문제만으로 `PRODUCT_MANAGER`를 제거하지 않는다.
- 관리자가 역할을 명시적으로 제거한 경우 Seller 상태는 바뀌지 않으며, 재활성화 시 역할을 자동 복구하지 않는다.

### 상태 전이

| 리소스 | 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|---|
| SellerApplication | `PENDING` | 관리자 승인 | `APPROVED` + Seller `ACTIVE` 생성 | P7 Admin + P8 Seller |
| SellerApplication | `PENDING` | 관리자 거절 | `REJECTED` | P7 Admin + P8 Seller |
| Seller | `ACTIVE` | 관리자 정지 | `SUSPENDED` + Offer 비활성화 | P7 Admin + P8/P9 |
| Seller | `SUSPENDED` | 관리자 재활성화 | `ACTIVE` + 정지로 제거된 역할 복구 | P7 Admin + P8 Seller |
| Offer | `ACTIVE` | Seller 정지 | `INACTIVE` (`SYSTEM`) | P9 Offer |
| Offer | `INACTIVE` (`SELLER-002` 원인) | Seller 재활성화 후 판매자 확인 | `ACTIVE` | Seller + P9 Offer |

## 5. 도메인 간 규칙과 예외 소유권

- P1 User의 공개 User 식별자·역할 계약만 사용하며 User 내부 모델과 Repository를 참조하지 않는다.
- P2 Catalog의 공개 검증·생성 계약을 사용하며 정식 Catalog 데이터를 직접 생성하지 않는다.
- P7 Admin은 P8 원본의 승인·거절과 Seller 상태 변경 HTTP 진입점을 제공한다.
- P9 Offer는 Seller 상태 변경에 따라 Offer를 비활성화하고, Seller 소유자 조회·변경 권한을 판정한다.
- P5 Order는 주문 원본·상태를 소유하고 P8은 판매자별 조회 결과만 제공한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P1 User | 신청자·Seller 연결, 역할 집합 | [P1 Policy](../p1/p1-policy.md), [User API](../p1/p1-user.md) |
| P2 Catalog | Catalog 등록 요청 검증·정식 생성 | [P2 Policy](../p2/p2-policy.md) |
| P5 Order | 판매자 주문 조회와 주문 상태 | [P5 Policy](../p5/p5-policy.md), [Order Core](../p5/p5-order-core.md) |
| P7 Admin | 관리자 심사·상태 변경 | [P7 Policy](../p7/p7-policy.md), [P7 Access](../p7/p7-access.md) |
| P9 Offer | Offer 비활성화·소유권·재고 | [P9 Index](../p9/p9-index.md) |

## 6. API 문서와의 관계

- Seller 신청·프로필·Catalog 등록 요청·판매자 주문 API의 데이터 모델과 응답은 각 리소스 API 문서에서 정의한다.
- 이 정책과 API 문서가 충돌하면 이 정책을 기준으로 API 문서를 수정한다.
- 공통 오류 응답 필드와 인증은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
