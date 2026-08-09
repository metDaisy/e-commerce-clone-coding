# P3 Cart (장바구니)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](index.md#공통-api-계약)을 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/cart` | 로그인 | 활성 장바구니 조회 |
| POST | `/api/v1/cart/items` | 로그인 | 상품 추가 |
| PATCH | `/api/v1/cart/items/{cartItemId}` | 로그인 | 수량 변경 |
| DELETE | `/api/v1/cart/items/{cartItemId}` | 로그인 | 상품 삭제 |
| DELETE | `/api/v1/cart/items` | 로그인 | 전체 비우기 |
| POST | `/api/v1/cart/checkout-preview` | 로그인 | 주문 전 검증·금액 미리보기 |

## 2. 요구사항

### 2-1. 기본 규칙

- 사용자당 `ACTIVE` 상태의 장바구니는 하나만 존재한다.
- 상품을 처음 추가하면 장바구니를 자동 생성한다.
- 단일 상품 수량은 1~10개다.
- 장바구니 전체 상품 종류는 최대 50개다.
- 동일 Variant·Offer를 다시 추가하면 기존 수량에 합산한다.
- 수량을 0으로 변경하면 항목을 삭제한다.
- 모든 조회·변경 API는 장바구니 소유자를 검증한다.

### 2-2. 상품 추가

`POST /api/v1/cart/items`

요청:

```json
{
  "offerId": "uuid",
  "quantity": 2
}
```

성공 응답 `201` 또는 기존 항목 합산 시 `200`:

```json
{
  "cartItemId": "uuid",
  "offerId": "uuid",
  "productId": "uuid",
  "quantity": 2,
  "unitPrice": { "amount": 49900.00, "currency": "KRW" },
  "subtotal": { "amount": 99800.00, "currency": "KRW" }
}
```

### 2-3. 장바구니 조회

`GET /api/v1/cart`

응답:

```json
{
  "cartId": "uuid",
  "status": "ACTIVE",
  "items": [
    {
      "cartItemId": "uuid",
      "productId": "uuid",
      "variantId": "uuid",
      "offerId": "uuid",
      "name": "무선 헤드폰",
      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
      "quantity": 2,
      "unitPrice": { "amount": 49900.00, "currency": "KRW" },
      "subtotal": { "amount": 99800.00, "currency": "KRW" },
      "priceChanged": false,
      "outOfStock": false,
      "unavailable": false
    }
  ],
  "itemCount": 1,
  "subtotal": { "amount": 99800.00, "currency": "KRW" },
  "estimatedShippingFee": { "amount": 0.00, "currency": "KRW" },
  "total": { "amount": 99800.00, "currency": "KRW" }
}
```

- 조회 시 현재 가격·상품 보관 여부·구매 가능 재고를 재검증한다.
- 가격이 변경되면 이전 가격과 현재 가격을 함께 반환한다.
- 품절·보관 상품이 하나라도 있으면 결제 미리보기와 주문 생성을 차단한다.

### 2-4. 결제 전 검증

`POST /api/v1/cart/checkout-preview`

요청:

```json
{ "cartItemIds": ["uuid", "uuid"] }
```

응답:

```json
{
  "valid": true,
  "cartItemIds": ["uuid", "uuid"],
  "subtotal": { "amount": 99800.00, "currency": "KRW" },
  "shippingFee": { "amount": 0.00, "currency": "KRW" },
  "total": { "amount": 99800.00, "currency": "KRW" },
  "issues": []
}
```

### 2-5. 결제 후 처리

- 결제 성공 이벤트를 수신하면 주문에 포함된 항목을 장바구니에서 삭제한다.
- 부분 결제를 지원한다.
- 주문 실패 시 결제하지 않은 항목은 장바구니에 유지한다.

## 3. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 형식·수량 오류 |
| 400 | `CART_ITEM_LIMIT_EXCEEDED` | 수량 10개 또는 종류 50개 초과 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `CART_ACCESS_DENIED` | 타인의 장바구니 접근 |
| 404 | `CART_ITEM_NOT_FOUND` | 항목 없음 |
| 404 | `OFFER_NOT_FOUND` | 판매 오퍼 없음 |
| 409 | `OUT_OF_STOCK` | 재고 부족 |
| 409 | `CART_PRICE_CHANGED` | 결제 직전 가격 변경 |
| 409 | `CART_ITEM_UNAVAILABLE` | 보관·비활성 상품 |

## 4. 심화사항

- 비회원 장바구니와 로그인 후 장바구니 병합을 지원한다.
- 장바구니 저장 기간, 배송 그룹 분리, 판매자별 배송비를 지원한다.
- 가격 변동·품절·쿠폰 적용 가능 알림을 지원한다.
