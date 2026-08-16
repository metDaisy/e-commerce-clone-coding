# P8 SellerOrder API

이 문서는 Seller가 자신의 Offer가 포함된 주문을 조회하는 읽기 API를 정의한다. 주문 원본과 상태는 [P5 Order](../p5/p5-policy.md), Offer 소유권은 [P9 Offer](../p9/p9-index.md), 업무 정책은 [P8 Seller Policy](p8-policy.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Order`, `OrderItem` | 주문 원본·상태·상품·가격 스냅샷. P5 소유 | P5 Order API |
| `Offer` | Seller와 주문 항목의 연결. P9 소유 | P9 Offer 공개 계약 |
| `SellerOrderView` | Seller 소유권으로 필터링한 조회 응답 모델 | 주문 목록·상세 조회 |

- P8은 Order 원본이나 SellerOrder 영속 모델을 생성·수정하지 않는다.
- 주문 항목의 `offerId`와 P9의 Seller 소유권을 기준으로 본인 주문 항목만 반환한다.
- 주문·배송 상태 변경은 P5가 소유하며 P8은 읽기만 제공한다.

## 2. 데이터 모델

### 2-1. `SellerOrderView`

`SellerOrderView`는 P5 주문 원본을 Seller 소유권에 맞게 조합한 API 응답 모델이다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `orderId` | UUID | 예 | 주문 식별자 |
| `orderItemId` | UUID | 예 | Seller에게 귀속된 주문 항목 식별자 |
| `status` | ENUM | 예 | P5 Order의 주문 상태 |
| `createdAt` | TIMESTAMP | 예 | 주문 생성 시각 |
| `productName` | String | 예 | 주문 시점 상품명 스냅샷 |
| `variantDisplayName` | String | 예 | 주문 시점 Variant 표시명 스냅샷 |
| `quantity` | INTEGER | 예 | 주문 수량 |
| `unitPrice` | Money | 예 | 주문 시점 단가 |
| `shippingRecipientName` | String | 예 | 배송 처리에 필요한 수령인 스냅샷 |
| `shippingRecipientPhone` | String | 예 | 배송 처리에 필요한 연락처 스냅샷 |
| `shippingAddress` | Object | 예 | 배송 처리에 필요한 주소 스냅샷 |

### 2-2. 관계와 제약

- 한 Seller는 자신의 Offer가 포함된 주문 항목을 여러 개 조회할 수 있다.
- 여러 Seller의 Offer가 포함된 주문은 Seller별 주문 항목만 반환한다.
- Seller는 자신의 Offer가 포함되지 않은 주문을 조회할 수 없다.
- `catalogProductId`, `variantId`, `offerId` 같은 내부 Catalog·Offer 식별자는 Seller 주문 응답에 포함하지 않는다.
- 구매자의 개인정보는 배송 처리에 필요한 최소 정보만 반환한다.
- P5의 주문 상태·배송 상태·취소 규칙을 P8에서 재정의하지 않는다.

## 3. API 정의

### 3-1. 판매자 주문 목록 조회

`GET /api/v1/seller/orders`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

- 커서 기반 조회를 사용한다.
- 정렬은 `createdAt DESC, orderId DESC`로 고정한다.
- 응답은 `data`, `nextCursor`, `hasNext`를 사용한다.

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "orderId": "uuid-order",
      "orderItemId": "uuid-item",
      "status": "PAID",
      "createdAt": "2026-08-16T12:00:00Z",
      "productName": "무선 헤드폰",
      "variantDisplayName": "블랙 / 대형",
      "quantity": 2,
      "unitPrice": { "amount": 49900.00, "currency": "KRW" },
      "shippingRecipientName": "홍길동",
      "shippingRecipientPhone": "010-1234-5678",
      "shippingAddress": {
        "postalCode": "06236",
        "addressLine": "서울특별시 강남구 테헤란로 1"
      }
    }
  ],
  "nextCursor": null,
  "hasNext": false
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |

### 3-2. 판매자 주문 상세 조회

`GET /api/v1/seller/orders/{orderId}`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`

#### 성공 응답: `200 OK`

목록 항목과 같은 SellerOrderView 구조를 사용하며, 해당 주문에 포함된 본인 Seller의 주문 항목만 반환한다.

```json
{
  "orderId": "uuid-order",
  "status": "PAID",
  "createdAt": "2026-08-16T12:00:00Z",
  "items": [
    {
      "orderItemId": "uuid-item",
      "productName": "무선 헤드폰",
      "variantDisplayName": "블랙 / 대형",
      "quantity": 2,
      "unitPrice": { "amount": 49900.00, "currency": "KRW" },
      "shippingRecipientName": "홍길동",
      "shippingRecipientPhone": "010-1234-5678",
      "shippingAddress": {
        "postalCode": "06236",
        "addressLine": "서울특별시 강남구 테헤란로 1"
      }
    }
  ]
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](p8-seller-profile.md) | — | — | — | — |
| 404 | [ORDER-001](../p5/p5-order.md) | — | — | — | — |
