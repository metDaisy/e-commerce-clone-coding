# P8 Seller (판매자)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 역할

- `USER`는 기본 구매자 역할이며 상품을 구매·주문·리뷰한다.
- 판매자는 별도 로그인 계정이 아니라 기존 사용자에게 연결된 `Seller`이다.
- 한 사용자는 구매자이면서 판매자일 수 있다.
- 판매자 신청이 승인된 `Seller`만 판매자 API를 사용할 수 있다.
- `PRODUCT_MANAGER`는 활성 `Seller`을 가진 입점 판매자 역할이다.
- `PRODUCT_MANAGER`도 구매자 API를 사용할 수 있다. 역할명은 구매자 기능을 배제하는 의미가 아니다.
- `ADMIN`은 플랫폼 전체 운영과 판매자 신청을 담당하며 판매자 전용 목록·주문 API는 사용하지 않는다. Offer·Inventory 운영은 P9에서 정의한다.

CatalogProduct·ProductVariant는 P2가 소유하며 생성·수정·보관은 `ADMIN`만 수행한다. `PRODUCT_MANAGER`는 활성 Seller를 가진 User이며, P9의 Offer 등록 흐름에서 P2 CatalogProduct·ProductVariant를 선택한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/seller/applications` | 로그인한 사용자 | 판매자 신청 |
| GET | `/api/v1/seller/profile` | `PRODUCT_MANAGER` + `ACTIVE` | 내 판매자 프로필 조회 |
| PATCH | `/api/v1/seller/profile` | `PRODUCT_MANAGER` + `ACTIVE` | 내 판매자 프로필 수정 |
| GET | `/api/v1/seller/orders` | `PRODUCT_MANAGER` + `ACTIVE` | 내 Offer가 포함된 주문 목록 |
| GET | `/api/v1/seller/orders/{orderId}` | `PRODUCT_MANAGER` + `ACTIVE` | 판매자 주문 상세 |

판매자 신청 승인·정지 API는 [P7 관리자·운영 요구사항](p7-admin.md)에서 정의한다.

## 3. 요구사항

### 3-1. 판매자 신청

`POST /api/v1/seller/applications`

요청:

```json
{
  "displayName": "Example Store",
  "businessName": "Example Inc.",
  "businessRegistrationNumber": "masked-value",
  "contactEmail": "seller@example.com",
  "contactPhone": "010-1234-5678"
}
```

- 신청자는 로그인한 사용자다. `ADMIN`은 판매자 신청 대상이 아니다.
- 사용자당 `PENDING` 신청 또는 `ACTIVE` Seller는 각각 하나만 허용한다. `REJECTED`·`SUSPENDED` Seller만 있는 사용자는 새 신청을 할 수 있다.
- 신청 상태는 `PENDING`, `ACTIVE`, `REJECTED`, `SUSPENDED`다.
- 사업자 등록번호 등 민감한 식별 정보는 응답·로그에 원문으로 노출하지 않는다.
- 신청이 승인되기 전에는 Offer를 등록할 수 없다.
- `PENDING → ACTIVE` 승인 시 사용자의 역할을 `PRODUCT_MANAGER`로 변경한다.
- `PENDING → ACTIVE`, `PENDING → REJECTED`, `ACTIVE → SUSPENDED`만 허용한다. `REJECTED` 또는 `SUSPENDED`가 되면 판매자 API를 차단하고 사용자의 역할을 `USER`로 되돌린다.

성공 응답 `201`:

```json
{
  "sellerId": "uuid",
  "displayName": "Example Store",
  "status": "PENDING",
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### 3-2. 판매자 프로필

- 승인된 판매자만 자신의 프로필을 조회·수정한다.
- 판매자 표시명·연락처만 수정할 수 있다.
- 판매자 정산 정보는 기본 요구사항에 포함하지 않는다.

`PATCH /api/v1/seller/profile` 요청:

```json
{ "displayName": "Updated Store", "contactPhone": "010-1234-5678" }
```

응답은 `sellerId`, `displayName`, `contactEmail`, `contactPhone`, `updatedAt`을 반환한다.

### 3-3. P9 Offer 흐름 참조

Seller의 CatalogProduct 선택과 Offer·Inventory 등록·관리는 [P9 Offer & Marketplace](p9-offer.md)에서 정의한다. P8은 Seller 신청·승인·프로필과 판매자 주문 조회만 소유한다.

### 3-4. 판매자 주문 조회

- 판매자는 자신의 Offer가 포함된 주문만 조회한다.
- 주문 목록은 `createdAt DESC, orderId DESC` 커서 기반 조회를 사용한다.
- `GET /api/v1/seller/orders`의 응답은 `data`, `nextCursor`, `hasNext`를 사용한다. `data`의 각 항목은 `orderId`, `orderNumber`, `status`, `createdAt`과 해당 판매자의 `offerId`, `variantId`, `quantity`, `unitPrice`, 배송 처리에 필요한 수령인·연락처·주소 스냅샷만 포함한다.
- 구매자의 개인정보는 배송 처리에 필요한 최소 정보만 반환한다.
- 기본 요구사항에서는 주문·배송 상태 변경을 ADMIN이 수행한다. 판매자별 배송 처리는 심화사항으로 둔다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 신청·프로필 요청 필드 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 판매자 승인 전 API 호출 |
| 403 | `SELLER_RESOURCE_ACCESS_DENIED` | 다른 판매자의 주문 접근 |
| 409 | `SELLER_APPLICATION_ALREADY_EXISTS` | 중복 신청 |
| 403 | `SELLER_SUSPENDED` | 정지된 판매자 API 사용 |
