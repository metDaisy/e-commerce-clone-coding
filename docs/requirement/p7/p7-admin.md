# P7 Admin API

이 문서는 P7 관리자 API의 공통 권한·진입점·예외를 정의한다. 업무 정책은 [P7 Policy](p7-policy.md), 기능별 데이터 모델과 API 계약은 각 리소스 문서를 따른다.

## 1. 관리자 API 관계

| API 그룹 | 원본 책임 | 상세 문서 |
|---|---|---|
| 사용자·Seller | P1 User·P8 Seller | [P7 Access API](p7-access.md) |
| Category·Catalog | P2 Catalog·P8 CatalogRegistrationRequest | [P7 Catalog Policy](p7-catalog.md), [P7 Category API](p7-category.md), [P7 Catalog Request API](p7-catalog-requests.md) |
| Offer | P9 Offer·OfferActivationRequest | [P7 Offer API](p7-offer.md) |
| Outbox·Saga | P6 Infrastructure | [P7 Operations API](p7-operations.md) |

- P7은 관리자 HTTP 진입점과 권한 검증을 담당한다.
- 원본 상태·payload·심사 이력은 각 소유 도메인에 저장한다.
- P7에는 `AdminReviewItem`과 같은 통합 영속 모델을 두지 않는다.
- 관리자 화면은 요청 유형별 원본 조회 API를 사용하고, 필요하면 저장하지 않는 조회 DTO로 목록을 조합한다.

## 2. 공통 권한과 응답

- 모든 `/api/v1/admin/**` API는 `ADMIN` 역할을 요구한다.
- `USER`와 `PRODUCT_MANAGER`만 가진 사용자는 P7 API를 호출할 수 없다.
- 로그인하지 않은 요청은 `401 AUTH-001`, 권한이 없는 요청은 `403 ADMIN-001`을 반환한다.
- 관리자가 자기 자신의 `ADMIN` 역할을 제거하는 요청은 거부한다.
- 성공 응답은 각 API의 Response DTO를 직접 반환하고, 공통 성공 봉투를 사용하지 않는다.
- 예외 응답은 [공통 API 계약](../index.md#공통-api-계약)의 오류 필드를 사용한다.

## 3. 관리자 API 목록

| Method | URI | 권한 | 상세 문서 |
|---|---|---|---|
| POST | `/api/v1/admin/users/{userId}/roles` | `ADMIN` | [Access API](p7-access.md) |
| DELETE | `/api/v1/admin/users/{userId}/roles/{role}` | `ADMIN` | [Access API](p7-access.md) |
| PATCH | `/api/v1/admin/seller-applications/{applicationId}/status` | `ADMIN` | [Access API](p7-access.md) |
| PATCH | `/api/v1/admin/sellers/{sellerId}/status` | `ADMIN` | [Access API](p7-access.md) |
| POST | `/api/v1/admin/categories` | `ADMIN` | [Category API](p7-category.md) |
| PATCH | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | [Category API](p7-category.md) |
| GET | `/api/v1/admin/catalog-products` | `ADMIN` | [CatalogProduct API](p7-catalog-products.md) |
| GET | `/api/v1/admin/catalog-registration-requests` | `ADMIN` | [Catalog Request API](p7-catalog-requests.md) |
| POST | `/api/v1/admin/catalog-registration-requests/{requestId}/approve` | `ADMIN` | [Catalog Request API](p7-catalog-requests.md) |
| POST | `/api/v1/admin/catalog-registration-requests/{requestId}/reject` | `ADMIN` | [Catalog Request API](p7-catalog-requests.md) |
| PATCH | `/api/v1/admin/offers/{offerId}/status` | `ADMIN` | [Offer API](p7-offer.md) |
| GET | `/api/v1/admin/offers/activation-requests` | `ADMIN` | [Offer API](p7-offer.md) |
| POST | `/api/v1/admin/offers/activation-requests/{requestId}/approve` | `ADMIN` | [Offer API](p7-offer.md) |
| POST | `/api/v1/admin/offers/activation-requests/{requestId}/reject` | `ADMIN` | [Offer API](p7-offer.md) |
| PATCH | `/api/v1/admin/offers/{offerId}/price` | `ADMIN` | [P9 Offer API](../p9/p9-offer.md) |
| POST | `/api/v1/admin/offers/{offerId}/inventory-adjustments` | `ADMIN` | [P9 Inventory API](../p9/p9-inventory.md) |
| GET | `/api/v1/admin/outbox/events` | `ADMIN` | [Operations API](p7-operations.md) |
| GET | `/api/v1/admin/outbox/events/{eventId}` | `ADMIN` | [Operations API](p7-operations.md) |
| POST | `/api/v1/admin/outbox/events/{eventId}/retry` | `ADMIN` | [Operations API](p7-operations.md) |
| GET | `/api/v1/admin/sagas/{sagaId}` | `ADMIN` | [Operations API](p7-operations.md) |
| POST | `/api/v1/admin/sagas/{sagaId}/retry` | `ADMIN` | [Operations API](p7-operations.md) |

## 4. 공통 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | `AUTH-001` | Access Token이 없거나 유효하지 않음 | 로그인이 필요합니다. | 없음 | 인증 실패 원인과 requestId |
| 403 | `ADMIN-001` | `ADMIN` 권한 없음 | 관리자 권한이 필요합니다. | 없음 | 요청 사용자와 필요한 역할 |
| 403 | `ADMIN-002` | 자신의 `ADMIN` 역할 제거 | 해당 관리자 권한은 변경할 수 없습니다. | 없음 | 대상 User와 요청 관리자 |
| 400 | `ADMIN-003` | 공통 요청 필드 검증 실패 | 입력값을 확인해 주세요. | 실패 필드와 수정 방법 | 실제 입력값과 내부 검증 원인 |
| 400 | `ADMIN-004` | 목록 page·size·filter 검증 실패 | 목록 조회 조건을 확인해 주세요. | 실패 query | 입력값과 검증 원인 |
| 500 | `SYSTEM-001` | 예측하지 못한 내부 오류 | 요청을 처리하지 못했습니다. | 생략 | 내부 원인·stack trace·requestId |

P7이 소유하는 추가 예외 코드는 다음 번호를 사용한다. 한 코드는 하나의 의미만 가지며, 이미 사용한 번호를 재사용하지 않는다.

| exceptionCode | 의미 |
|---|---|
| `ADMIN-005` | 이미 부여된 역할 |
| `ADMIN-006` | `PRODUCT_MANAGER`를 추가할 활성 Seller 없음 |
| `ADMIN-007` | 보유하지 않은 역할 삭제 |
| `ADMIN-008` | 기본 `USER` 역할 삭제 시도 |
| `ADMIN-009` | Seller 신청 없음 |
| `ADMIN-010` | `PENDING`이 아닌 Seller 신청 처리 |
| `ADMIN-011` | Seller 신청 거절 사유·메시지 오류 |
| `ADMIN-012` | 승인 결과 Seller 중복 |
| `ADMIN-013` | Seller 없음 |
| `ADMIN-014` | Seller 상태 전이 충돌 |
| `ADMIN-015` | Seller 상태 변경 요청 오류 |
| `ADMIN-016` | Catalog 등록 요청 없음 |
| `ADMIN-017` | `PENDING`이 아닌 Catalog 등록 요청 처리 |
| `ADMIN-018` | Catalog 등록 승인 충돌 |
| `ADMIN-019` | Catalog 등록 거절 사유·메시지 오류 |
| `ADMIN-020` | Category 수정 필드 없음 |
| `ADMIN-021` | Offer 비활성화 사유·메시지 오류 |
| `ADMIN-022` | Offer 활성화 요청 없음 |
| `ADMIN-023` | `PENDING`이 아닌 Offer 활성화 요청 처리 |
| `ADMIN-024` | Offer 활성화 요청 거절 사유·메시지 오류 |
| `ADMIN-025` | Outbox 이벤트 재시도 불가 |
| `ADMIN-026` | Saga 재시도 불가 |
| `ADMIN-027` | Outbox 이벤트 없음 |
| `ADMIN-028` | Saga 없음 |

도메인별 상태 충돌과 업무 예외는 각 리소스 API 문서에서 정의한다.

## 5. 정책과 API의 관계

- 승인·거절·상태 변경의 허용 조건은 [P7 Policy](p7-policy.md)를 따른다.
- API별 예외는 각 리소스 문서의 예외 매트릭스에 등록한다.
- `AUTH-001`과 `SYSTEM-001`은 [공통 API 계약](../index.md#공통-api-계약)의 원본 정의를 따른다.
- 다른 도메인의 예외를 그대로 전달할 때는 원본 도메인의 exceptionCode·client message·system message 정책을 참조한다.
- P7이 외부 도메인 오류를 관리자용 오류로 변환할 때만 `ADMIN-xxx` 코드를 사용하고, 변환 원인과 원본 코드를 system message에 기록한다.
