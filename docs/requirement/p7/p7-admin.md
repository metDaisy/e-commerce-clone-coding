# P7 Admin & Operations (관리자·운영)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 범위와 공통 규칙

P7은 관리자 전용 진입점과 운영 기능을 정의한다. 실제 도메인 데이터와 상태의 소유권은 각 도메인에 남겨두고, P7은 공개된 application interface를 통해 관리 작업을 호출한다.

- 모든 P7 API는 역할 집합에 `ADMIN`을 포함한 사용자만 호출할 수 있다.
- `USER`는 구매자 기능을 사용하며 P7 API에 접근할 수 없다.
- `PRODUCT_MANAGER`만 보유한 사용자는 P8 Seller API와 P9 Offer·Inventory API를 사용하며 P7 API에는 접근할 수 없다.
- 플랫폼 전체 운영 기능은 `ADMIN`만 수행한다. `PRODUCT_MANAGER`는 플랫폼 운영자가 아니라 입점 판매자다.
- 로그인하지 않았거나 권한이 없으면 각각 `401 AUTHENTICATION_REQUIRED`, `403 ACCESS_DENIED`를 반환한다.
- 관리자가 자기 자신의 `ADMIN` 권한을 해제하는 요청은 거부한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/admin/users/{userId}/roles` | `ADMIN` | 사용자 역할 추가 |
| DELETE | `/api/v1/admin/users/{userId}/roles/{role}` | `ADMIN` | 사용자 역할 삭제 |
| PATCH | `/api/v1/admin/seller-applications/{sellerId}/status` | `ADMIN` | 판매자 신청 승인·거절·정지 |
| POST | `/api/v1/admin/categories` | `ADMIN` | 카테고리 생성 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 카테고리 수정 |
| GET | `/api/v1/admin/catalog-products` | `ADMIN` | CatalogProduct·ProductVariant 관리 목록 |
| GET | `/api/v1/admin/catalog-registration-requests` | `ADMIN` | 판매자 Catalog 등록 요청 목록 |
| POST | `/api/v1/admin/catalog-registration-requests/{requestId}/approve` | `ADMIN` | 판매자 Catalog 등록 요청 승인 |
| POST | `/api/v1/admin/catalog-registration-requests/{requestId}/reject` | `ADMIN` | 판매자 Catalog 등록 요청 거절 |
| PATCH | `/api/v1/admin/offers/{offerId}/status` | `ADMIN` | Offer 활성·비활성 |
| GET | `/api/v1/admin/offers/activation-requests` | `ADMIN` | Offer 활성화 요청 목록 |
| POST | `/api/v1/admin/offers/activation-requests/{requestId}/approve` | `ADMIN` | Offer 활성화 요청 승인 |
| POST | `/api/v1/admin/offers/activation-requests/{requestId}/reject` | `ADMIN` | Offer 활성화 요청 거절 |
| PATCH | `/api/v1/admin/offers/{offerId}/price` | `ADMIN` | Offer 가격 운영 수정 |
| POST | `/api/v1/admin/offers/{offerId}/inventory-adjustments` | `ADMIN` | Offer 재고 운영 조정 |
| GET | `/api/v1/admin/outbox/events` | `ADMIN` | 이벤트 처리 현황 |
| GET | `/api/v1/admin/outbox/events/{eventId}` | `ADMIN` | 이벤트 상세 |
| POST | `/api/v1/admin/outbox/events/{eventId}/retry` | `ADMIN` | 실패 이벤트 재처리 |
| GET | `/api/v1/admin/sagas/{sagaId}` | `ADMIN` | Saga 상태 조회 |
| POST | `/api/v1/admin/sagas/{sagaId}/retry` | `ADMIN` | Saga 보상 재시도 |

## 3. 관리자 심사 통합 모델

P7에는 여러 도메인의 관리자 심사 대상을 한 화면에서 조회하기 위한 `AdminReviewItem`을 둘 수 있다. 이 모델은 관리자용 통합 조회·정렬·필터를 위한 모델이며, 원본 요청의 상세 payload와 업무 상태를 소유하지 않는다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `reviewItemId` | UUID | 관리자 심사 항목 식별자 |
| `requestType` | ENUM | `SELLER_APPLICATION`, `CATALOG_REGISTRATION`, `OFFER_ACTIVATION` (`CATEGORY`는 `CATALOG_REGISTRATION`의 하위 유형) |
| `sourceRequestId` | UUID | 원본 요청 식별자. 판매자 신청은 `sellerId`를 사용한다. |
| `sourceModule` | ENUM | `P8_SELLER`, `P9_OFFER` |
| `sellerId` | UUID | 심사 대상 판매자 |
| `requestedByUserId` | UUID | 실제 요청 제출 User |
| `subjectId` | UUID | 심사 대상 Seller·CatalogProduct·ProductVariant·Offer 식별자 |
| `summary` | JSONB | 관리자 목록에 필요한 최소 요약 정보. 원본 payload는 저장하지 않는다. |
| `status` | ENUM | `PENDING`, `APPROVED`, `REJECTED` |
| `submittedAt` | TIMESTAMP | 원본 요청 제출 시각 |
| `processedByUserId` | UUID | 심사를 처리한 ADMIN |
| `processedAt` | TIMESTAMP | 심사 처리 시각 |
| `createdAt`, `updatedAt` | TIMESTAMP | 통합 항목 생성·최종 동기화 시각 |

- 원본 데이터는 각 소유 모듈에 저장한다. Seller 신청은 P8 `Seller`, Category·CatalogProduct·Variant 생성 요청은 P8 `CatalogRegistrationRequest`, Offer 재활성화 요청은 P9 `OfferActivationRequest`가 원본이다.
- P7 `AdminReviewItem`의 상태 변경은 원본 요청의 승인·거절 처리 결과를 반영한 것이다. P7만 변경하고 원본을 변경하지 않는다.
- `summary`는 목록 표시용 복제 데이터이므로 원본과 불일치할 수 있다. 상세 화면은 `sourceModule`과 `sourceRequestId`로 원본 모듈에 조회한다.
- 원본 요청이 삭제되거나 처리된 뒤에도 관리자 감사·목록 이력이 필요하므로 `AdminReviewItem`은 물리 삭제하지 않는다.

## 4. 기능별 요구사항

| 문서 | 범위 |
|---|---|
| [P7 Access & Seller Review](p7-access.md) | 사용자 역할 변경, 세션 무효화, 판매자 신청 심사 |
| [P7 Catalog Administration](p7-catalog.md) | Catalog 관리 문서 모음 |
| [P7 Category Administration](p7-category.md) | Category 생성·수정 |
| [P7 CatalogProduct Administration](p7-catalog-products.md) | CatalogProduct·ProductVariant 관리자 목록 |
| [P7 Catalog Registration Review](p7-catalog-requests.md) | 판매자 Catalog 등록 요청 심사 |
| [P7 Offer Operations](p7-offer.md) | Offer 활성·비활성, 활성화 요청 심사, 가격·재고 운영 진입점 |
| [P7 Operations](p7-operations.md) | 쿠폰·배송·Outbox·Saga 운영 |

판매자 신청은 P8 Seller가 소유한다. 신청 시 `Seller` 레코드를 `PENDING`으로 저장하고, P7은 해당 레코드를 관리자 심사 대상으로 조회·변경한다. 상세 데이터 모델은 [P8 Seller 데이터 모델](../p8/p8-seller.md#2-1-데이터-모델)을 따른다.

CatalogProduct·ProductVariant의 정식 생성·수정·보관은 P2 Catalog가 소유한다. Offer·Inventory의 상세 상태·가격·재고 규칙은 P9가 소유한다. P7은 각 도메인의 공개 application interface와 관리자 HTTP 진입점을 제공한다.

## 5. 공통 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 또는 역할 값 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | ADMIN 권한 부족 |
| 403 | `CANNOT_CHANGE_OWN_ADMIN_ROLE` | 자기 자신의 ADMIN 권한 변경 시도 |
| 400 | `INVALID_CATEGORY_PARENT` | 부모가 없거나 자기 자신·하위 카테고리를 부모로 지정함 |
| 400 | `CATEGORY_CYCLE_DETECTED` | 부모 연결에서 순환 참조가 발생함 |
| 400 | `CATEGORY_DEPTH_EXCEEDED` | 카테고리 깊이가 3단계를 초과함 |
| 400 | `CATEGORY_NAME_INVALID` | 카테고리 이름이 비어 있거나 공백뿐임 |
| 400 | `CATEGORY_UPDATE_EMPTY` | 수정할 카테고리 필드가 없음 |
| 404 | `USER_NOT_FOUND` | 사용자 없음 |
| 409 | `ROLE_ALREADY_ASSIGNED` | 이미 보유한 역할을 다시 추가함 |
| 409 | `ROLE_NOT_ASSIGNED` | 보유하지 않은 역할을 삭제하려 함 |
| 409 | `USER_ROLE_NOT_REMOVABLE` | 기본 `USER` 역할 삭제 시도 |
| 404 | `SELLER_NOT_FOUND` | 판매자 프로필 없음 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `DELIVERY_NOT_FOUND` | 배송 없음 |
| 404 | `CATALOG_REGISTRATION_REQUEST_NOT_FOUND` | Catalog 등록 요청 없음 |
| 404 | `OUTBOX_EVENT_NOT_FOUND` | 이벤트 없음 |
| 404 | `SAGA_NOT_FOUND` | Saga 없음 |
| 404 | `OFFER_ACTIVATION_REQUEST_NOT_FOUND` | Offer 활성화 요청 없음 |
| 409 | `CATEGORY_HAS_CHILDREN` | 하위 카테고리 또는 상품 존재 |
| 409 | `CATALOG_REGISTRATION_REQUEST_NOT_PENDING` | 이미 처리된 Catalog 등록 요청 승인·거절 시도 |
| 409 | `EVENT_ALREADY_PUBLISHED` | 발행 완료 이벤트 재처리 |
| 409 | `OFFER_ACTIVATION_REQUEST_ALREADY_PENDING` | Offer에 처리 중인 활성화 요청이 이미 있음 |
| 409 | `OFFER_ACTIVATION_REQUEST_NOT_PENDING` | 이미 처리된 활성화 요청을 다시 처리함 |
| 409 | `SAGA_NOT_RETRYABLE` | 재시도할 수 없는 Saga 상태 |
