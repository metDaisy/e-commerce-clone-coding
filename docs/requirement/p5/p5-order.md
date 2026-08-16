# P5 Order API

Order의 데이터 모델과 주문 리소스 API를 정의한다. 업무 정책은 [P5 Policy](p5-policy.md), 주문 화면 흐름은 [Order Checkout](p5-order-checkout.md)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | API |
|---|---|---|
| `Order` | 주문 소유자, 항목, 금액, 상태, 배송지 스냅샷 | 생성·상세·목록·취소 |
| `OrderItem` | 주문 시점의 상품·Variant·Offer·가격·수량 스냅샷 | Order 응답에 포함 |

## 2. 데이터 모델

### 2-1. `Order`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `orderId` | UUID | O | 사용자가 조회하는 주문 식별자 |
| `userId` | UUID | O | 주문 소유자 |
| `status` | Enum | O | `PENDING`, `PAID`, `CANCELED`, `EXPIRED` |
| `totalAmount` | Integer | O | 쿠폰·포인트를 반영한 최종 주문 금액(KRW) |
| `usedPointAmount` | Integer | O | 사용한 포인트, 미사용 시 `0` |
| `checkoutKey` | String | O | 정렬된 Cart Item ID 집합에서 계산한 주문 재진입 키 |
| `shippingAddress` | Snapshot | - | 결제 확정 시 저장한 배송지 스냅샷 |
| `expiresAt` | Instant | O | `PENDING` 만료 시각, 생성 후 24시간 |
| `createdAt` | Instant | O | 생성 시각 |
| `updatedAt` | Instant | O | 변경 시각 |

`shippingFee`와 `currency` 필드는 사용하지 않는다. 주문 생성 시 배송지는 요청 본문에 넣지 않고 별도 주소 API로 조회하며, 결제 시 선택한 주소를 스냅샷으로 저장한다.

### 2-2. `OrderItem`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `orderItemId` | UUID | O | 주문 항목 식별자 |
| `orderId` | UUID | O | 소속 주문 |
| `cartItemId` | UUID | O | 주문으로 전환한 장바구니 항목 |
| `catalogProductId` | UUID | O | 상품 공통 정보 식별자 |
| `variantId` | UUID | O | 구매한 상품 Variant 식별자 |
| `offerId` | UUID | O | 구매한 판매 조건 식별자 |
| `productName` | String | O | 주문 시점 상품명 스냅샷 |
| `variantDisplayName` | String | O | 주문 시점 Variant 표시명 스냅샷 |
| `unitPrice` | Integer | O | 주문 시점 단가(KRW) |
| `quantity` | Integer | O | 주문 수량(1 이상) |
| `lineAmount` | Integer | O | `unitPrice × quantity` |

`catalogProductId`, `variantId`, `offerId`는 구매 대상을 추적하고 결제·주문 조회 시 검증하기 위해 보관한다. 화면 표시가 변경되어도 주문 내역이 달라지지 않도록 이름과 가격은 스냅샷으로 함께 보관한다.

### 2-3. 관계와 제약

- 하나의 Order는 하나 이상의 OrderItem을 가진다.
- `(userId, checkoutKey, status=PENDING)` 조합은 하나만 존재한다.
- OrderItem의 `cartItemId`는 주문 생성 시점의 사용자 소유 Cart Item이어야 한다.
- `totalAmount = Σ lineAmount - couponDiscount - usedPointAmount`이며 음수가 될 수 없다.
- `PAID` 이후 OrderItem과 주문 금액은 수정하지 않는다.

## 3. API 정의

### 3-1. 주문 상세 조회

`GET /api/v1/orders/{orderId}`

권한: 주문 소유자 본인. `PENDING` 주문은 유효한 OrderSession이 있어야 한다.

#### 성공 응답: `200 OK`

```json
{
  "orderId": "order-uuid",
  "status": "PENDING",
  "items": [
    {
      "orderItemId": "item-uuid",
      "catalogProductId": "catalog-uuid",
      "variantId": "variant-uuid",
      "offerId": "offer-uuid",
      "productName": "NVIDIA GPU 5080",
      "variantDisplayName": "16GB",
      "unitPrice": 1200000,
      "quantity": 1,
      "lineAmount": 1200000
    }
  ],
  "couponDiscountAmount": 0,
  "usedPointAmount": 0,
  "totalAmount": 1200000,
  "shippingAddress": null,
  "expiresAt": "2026-08-17T12:00:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 403 | `ORDER-002` | 다른 사용자의 `orderId` | 주문을 조회할 수 없습니다. |
| 404 | `ORDER-001` | 주문이 없음 | 주문을 찾을 수 없습니다. |
| 410 | `ORDER-007` | `EXPIRED` 주문을 주문 화면에서 조회 | 주문이 만료되었습니다. |
| 423 | `ORDER-003` | `PENDING` 주문의 세션 없음 | 주문 화면 세션이 필요합니다. |

### 3-2. 주문 생성 또는 갱신

`POST /api/v1/orders`

권한: 로그인 사용자. 최초 생성은 인증만 필요하며, 기존 `PENDING` 주문 갱신은 해당 OrderSession이 필요하다.

#### 요청

```json
{
  "cartItemIds": ["cart-item-a", "cart-item-b"],
  "couponApplications": [
    { "cartItemId": "cart-item-a", "userCouponId": "user-coupon-a" }
  ],
  "pointAmount": 1000
}
```

배송지는 별도 주소 API에서 조회·선택한다. 같은 `cartItemIds`의 유효한 `PENDING` 주문이 있으면 수량·쿠폰 매핑·포인트를 최신 요청으로 갱신한다.

#### 성공 응답: `201 Created` 또는 `200 OK`

새 주문은 `201 Created`, 기존 주문 갱신은 `200 OK`를 반환하며 응답 구조는 주문 상세와 같다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 400 | `ORDER-008` | Cart Item ID가 비어 있거나 중복 | 장바구니 항목을 확인해주세요. |
| 409 | `ORDER-009` | 현재 가격이 요청 시점과 다름 | 상품 가격이 변경되었습니다. |
| 409 | `ORDER-010` | 재고가 부족함 | 재고가 부족한 상품이 있습니다. |
| 409 | `ORDER-011` | 쿠폰 대상·기간·사용 상태가 유효하지 않음 | 적용할 수 없는 쿠폰이 있습니다. |
| 422 | `ORDER-012` | 보유 포인트보다 많이 사용함 | 사용할 포인트를 확인해주세요. |
| 423 | `ORDER-018` | 다른 디바이스가 주문 화면을 점유함 | 다른 기기에서 주문이 진행 중입니다. |

주문 생성 실패 시 서버 HTTP Redirect를 수행하지 않는다. 클라이언트는 오류 메시지를 표시하고 `/cart`로 이동한다.

### 3-3. 주문 취소

`POST /api/v1/orders/{orderId}/cancel`

권한: 주문 소유자 본인. 전체 주문 단위로만 취소한다.

#### 성공 응답: `200 OK`

```json
{
  "orderId": "order-uuid",
  "status": "CANCELED"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 403 | `ORDER-002` | 주문 소유자가 아님 | 주문을 취소할 수 없습니다. |
| 404 | `ORDER-001` | 주문이 없음 | 주문을 찾을 수 없습니다. |
| 409 | `ORDER-014` | 취소할 수 없는 상태 | 현재 주문 상태에서는 취소할 수 없습니다. |
