# P8 Seller & Marketplace (판매자)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. 범위와 역할

- `USER`는 기본 구매자 역할이며 상품을 구매·주문·리뷰한다.
- 판매자는 별도 로그인 계정이 아니라 기존 사용자에게 연결된 `SellerProfile`이다.
- 한 사용자는 구매자이면서 판매자일 수 있다.
- 판매자 신청이 승인된 `SellerProfile`만 판매자 API를 사용할 수 있다.
- `PRODUCT_MANAGER`는 활성 `SellerProfile`을 가진 입점 판매자 역할이다.
- `PRODUCT_MANAGER`도 구매자 API를 사용할 수 있다. 역할명은 구매자 기능을 배제하는 의미가 아니다.
- `ADMIN`은 플랫폼 전체 운영과 판매자 신청을 담당하며 판매자 API를 사용하지 않는다.

카탈로그 상품·Variant는 P2가 소유한다. 판매자는 기존 Variant에 자신의 Offer를 등록하고 가격·판매 상태·재고를 관리한다.

## 2. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/seller/applications` | 로그인한 사용자 | 판매자 신청 |
| GET | `/api/v1/seller/profile` | `PRODUCT_MANAGER` + `ACTIVE` | 내 판매자 프로필 조회 |
| PATCH | `/api/v1/seller/profile` | `PRODUCT_MANAGER` + `ACTIVE` | 내 판매자 프로필 수정 |
| GET | `/api/v1/seller/offers` | `PRODUCT_MANAGER` + `ACTIVE` | 내 Offer 목록 |
| POST | `/api/v1/seller/offers` | `PRODUCT_MANAGER` + `ACTIVE` | Variant Offer 등록 |
| PATCH | `/api/v1/seller/offers/{offerId}` | `PRODUCT_MANAGER` + `ACTIVE` | Offer 수정·비활성화 |
| POST | `/api/v1/seller/offers/{offerId}/inventory-adjustments` | `PRODUCT_MANAGER` + `ACTIVE` | 재고 조정 |
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
- 사용자당 활성 신청 또는 `ACTIVE` 판매자 프로필은 하나만 허용한다.
- 신청 상태는 `PENDING`, `ACTIVE`, `REJECTED`, `SUSPENDED`다.
- 사업자 등록번호 등 민감한 식별 정보는 응답·로그에 원문으로 노출하지 않는다.
- 신청이 승인되기 전에는 Offer를 등록할 수 없다.
- `PENDING → ACTIVE` 승인 시 사용자의 역할을 `PRODUCT_MANAGER`로 변경한다.
- `ACTIVE → SUSPENDED` 또는 승인 취소 시 판매자 API를 차단하고 사용자의 역할을 `USER`로 되돌린다.

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

### 3-3. Offer 관리

`POST /api/v1/seller/offers`

요청:

```json
{
  "variantId": "uuid",
  "price": { "amount": 49900.00, "currency": "KRW" }
}
```

- `PRODUCT_MANAGER`이면서 `SellerProfile.status = ACTIVE`인 판매자만 존재하는 Variant에 Offer를 등록한다.
- `sellerId`는 요청 본문으로 받지 않고 인증된 SellerProfile에서 결정한다.
- 판매자는 자신의 Offer만 조회·수정·비활성화할 수 있다.
- 기본 요구사항에서는 하나의 SellerProfile이 같은 Variant에 Offer를 하나만 등록한다.
- 상품명·설명·카테고리·SKU는 P2의 CatalogProduct·Variant가 소유한다.

### 3-4. 판매자 재고

- 판매자는 자신의 Offer 재고만 조정한다.
- 재고 수량은 0 미만이 될 수 없다.
- 구매자 주문에 의한 재고 차감 규칙은 P2·P5·P6을 따른다.

### 3-5. 판매자 주문 조회

- 판매자는 자신의 Offer가 포함된 주문만 조회한다.
- 주문 목록은 `createdAt DESC, orderId DESC` 커서 기반 조회를 사용한다.
- 구매자의 개인정보는 배송 처리에 필요한 최소 정보만 반환한다.
- 기본 요구사항에서는 주문·배송 상태 변경을 ADMIN이 수행한다. 판매자별 배송 처리는 심화사항으로 둔다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 신청·Offer 요청 필드 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `SELLER_APPROVAL_REQUIRED` | 판매자 승인 전 API 호출 |
| 403 | `SELLER_RESOURCE_ACCESS_DENIED` | 다른 판매자의 Offer·주문 접근 |
| 404 | `VARIANT_NOT_FOUND` | Variant 없음 |
| 404 | `OFFER_NOT_FOUND` | Offer 없음 |
| 409 | `SELLER_APPLICATION_ALREADY_EXISTS` | 중복 신청 |
| 409 | `SELLER_OFFER_ALREADY_EXISTS` | 같은 Variant에 Offer 중복 등록 |
| 409 | `SELLER_SUSPENDED` | 정지된 판매자 사용 |
