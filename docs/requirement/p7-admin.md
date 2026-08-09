# P7 Admin & Operations (관리자·운영)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위

P7은 관리자 전용 진입점과 운영 기능을 정의한다. 상품·쿠폰·주문·이벤트 데이터의 소유권은 각각 P2~P6에 유지하며, P7은 공개된 application interface를 통해 해당 기능을 호출한다.

- 모든 P7 API는 `ADMIN`만 호출할 수 있다.
- `USER`는 구매자 기능을 사용하며 P7 API에 접근할 수 없다.
- `PRODUCT_MANAGER`는 판매자 기능과 P2의 상품·재고 API를 사용한다. P7 API에는 접근할 수 없다.
- 플랫폼 전체 운영 기능은 `ADMIN`만 수행한다. `PRODUCT_MANAGER`는 플랫폼 운영자가 아니라 입점 판매자다.
- 로그인하지 않았거나 권한이 없으면 각각 `401 AUTHENTICATION_REQUIRED`, `403 ACCESS_DENIED`를 반환한다.
- 관리자가 자기 자신의 `ADMIN` 권한을 해제하는 요청은 거부한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| PATCH | `/api/v1/admin/users/{userId}/role` | `ADMIN` | 사용자 권한 변경 |
| PATCH | `/api/v1/admin/seller-applications/{sellerId}/status` | `ADMIN` | 판매자 신청 승인·거절·정지 |
| POST | `/api/v1/admin/categories` | `ADMIN` | 카테고리 생성 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 카테고리 수정 |
| DELETE | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 카테고리 삭제 |
| POST | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 생성 |
| PATCH | `/api/v1/admin/coupons/{couponId}` | `ADMIN` | 쿠폰 수정·비활성화 |
| GET | `/api/v1/admin/coupons` | `ADMIN` | 쿠폰 관리 목록 |
| PATCH | `/api/v1/admin/deliveries/{deliveryId}/status` | `ADMIN` | 배송 상태 변경 |
| GET | `/api/v1/admin/outbox/events` | `ADMIN` | 이벤트 처리 현황 |
| GET | `/api/v1/admin/outbox/events/{eventId}` | `ADMIN` | 이벤트 상세 |
| POST | `/api/v1/admin/outbox/events/{eventId}/retry` | `ADMIN` | 실패 이벤트 재처리 |
| GET | `/api/v1/admin/sagas/{sagaId}` | `ADMIN` | Saga 상태 조회 |
| POST | `/api/v1/admin/sagas/{sagaId}/retry` | `ADMIN` | Saga 보상 재시도 |

판매자의 상품 등록·수정·보관·재고 조정은 `PRODUCT_MANAGER`가 P2 API로 수행할 수 있으므로 P7에 중복 정의하지 않는다. 플랫폼 운영 목적의 동일 작업은 `ADMIN`이 수행한다.

## 3. 요구사항

### 3-1. 사용자 권한 변경

`PATCH /api/v1/admin/users/{userId}/role`

요청:

```json
{
  "role": "PRODUCT_MANAGER"
}
```

- 허용 역할은 `USER`, `PRODUCT_MANAGER`, `ADMIN`이다.
- `PRODUCT_MANAGER`는 활성 `SellerProfile`을 가진 판매자에게만 부여할 수 있다.
- 판매자 신청 승인 시 `SellerProfile.status`를 `ACTIVE`로 변경하고 사용자의 역할을 `PRODUCT_MANAGER`로 변경한다.
- 판매자 정지 또는 승인 취소 시 판매자 API를 차단하고 역할을 `USER`로 되돌린다.
- 요청자는 자신의 `ADMIN` 권한을 해제할 수 없다.
- 대상 사용자가 없으면 `USER_NOT_FOUND`를 반환한다.
- 변경 결과는 다음과 같이 반환한다.

```json
{
  "userId": "uuid",
  "role": "PRODUCT_MANAGER",
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 카테고리 관리

- 카테고리 생성·수정·삭제의 상세 규칙은 [P2 카테고리 요구사항](p2-product.md#3-1-카테고리)을 따른다.
- 루트 카테고리와 하위 카테고리의 `parentId`, `depth` 관계를 검증한다.
- 하위 카테고리나 상품이 존재하는 카테고리는 삭제할 수 없다.

### 3-3. 판매자 신청 관리

`PATCH /api/v1/admin/seller-applications/{sellerId}/status`

요청:

```json
{
  "status": "ACTIVE",
  "reason": "사업자 정보 확인 완료"
}
```

- `PENDING → ACTIVE` 또는 `PENDING → REJECTED` 전환을 허용한다.
- `ACTIVE → SUSPENDED` 전환으로 판매자 Offer 등록·수정을 차단한다.
- 승인·정지 이력과 처리 관리자를 기록한다.

### 3-4. 쿠폰 관리

- 쿠폰 생성·수정·비활성화·관리 목록의 상세 규칙은 [P4 쿠폰 요구사항](p4-coupon.md)을 따른다.
- 쿠폰 발급·내 쿠폰 조회는 구매자 기능이므로 P4에 남긴다.

### 3-5. 배송 운영

`PATCH /api/v1/admin/deliveries/{deliveryId}/status`의 상태 전이와 응답은 [P5 배송 요구사항](p5-order-payment-delivery.md#2-6-배송)을 따른다.

- `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED` 순서만 허용한다.
- `SHIPPED` 전환 시 운송장 번호가 필요하다.
- 배송 상태 변경은 관리자만 수행한다.

### 3-6. 이벤트·Saga 운영

- Outbox 조회·실패 이벤트 재처리·Saga 조회·보상 재시도는 P6의 상태 전이와 멱등성 규칙을 따른다.
- 이미 성공 처리된 이벤트 또는 재시도할 수 없는 Saga는 다시 처리하지 않는다.
- 운영 API 응답에는 비밀번호·토큰·OAuth secret 등 민감 정보를 포함하지 않는다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 필드 또는 역할 값 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `ACCESS_DENIED` | ADMIN 권한 부족 |
| 403 | `CANNOT_CHANGE_OWN_ADMIN_ROLE` | 자기 자신의 ADMIN 권한 변경 시도 |
| 404 | `USER_NOT_FOUND` | 사용자 없음 |
| 404 | `SELLER_NOT_FOUND` | 판매자 프로필 없음 |
| 404 | `CATEGORY_NOT_FOUND` | 카테고리 없음 |
| 404 | `COUPON_NOT_FOUND` | 쿠폰 없음 |
| 404 | `DELIVERY_NOT_FOUND` | 배송 없음 |
| 404 | `OUTBOX_EVENT_NOT_FOUND` | 이벤트 없음 |
| 404 | `SAGA_NOT_FOUND` | Saga 없음 |
| 409 | `CATEGORY_HAS_CHILDREN` | 하위 카테고리 또는 상품 존재 |
| 409 | `EVENT_ALREADY_PUBLISHED` | 발행 완료 이벤트 재처리 |
| 409 | `SAGA_NOT_RETRYABLE` | 재시도할 수 없는 Saga 상태 |
