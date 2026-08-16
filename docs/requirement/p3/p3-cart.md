# P3 Cart API

이 문서는 `Cart`와 `Cart Item` 데이터 모델 및 장바구니 API를 정의한다. 업무 정책은 [P3 Policy](p3-policy.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Cart` | 회원 또는 비회원 장바구니의 식별자·수명·감사 시각 | 조회·상품 추가·전체 비우기 |
| `CartItem` | Cart에 담긴 P9 Offer 식별자와 수량 | 조회·추가·수량 변경·개별 삭제·전체 비우기 |

- P3는 두 모델의 원본과 관계를 소유한다.
- `offer_id`는 P9 Offer의 식별자만 저장하는 논리 참조다. P9의 필드를 P3에 복제하지 않는다.
- 상품명·썸네일은 P2 공개 조회 계약, 판매 상태·현재 가격·구매 제한·재고는 P9 공개 조회 계약으로 조회한다.
- P5 주문 생성은 선택한 `cartItemIds`를 전달하며, P3는 주문·결제 상태나 최종 주문 금액을 저장하지 않는다.

## 2. 데이터 모델

### 2-1. `carts`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | UUID | 예 | 회원은 인증된 `userId`를 사용하고, 비회원은 서버가 생성한 UUID를 사용한다. 클라이언트가 임의의 Cart ID를 지정하지 않는다. |
| `expiresAt` | `TIMESTAMP WITH TIME ZONE` | 비회원만 | 비회원 장바구니의 만료 시각. 회원 장바구니는 `NULL`이다. |
| `createdAt` | `TIMESTAMP WITH TIME ZONE` | 예 | 생성 시각 |
| `updatedAt` | `TIMESTAMP WITH TIME ZONE` | 예 | 마지막 변경 시각 |

회원 Cart의 소유자는 인증된 `userId`로 확인하고, 비회원 Cart의 소유자는 `guest_cart_id` 쿠키와 `id`의 일치로 확인한다. 요청 본문이나 URL의 `cartId`를 소유권 확인에 사용하지 않는다.

### 2-2. `cart_items`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | UUID | 예 | Cart Item 식별자(`cartItemId`) |
| `cartId` | UUID | 예 | 같은 P3의 `Cart.id`를 참조한다. DB 외래 키와 Cart 삭제 시 Cascade 삭제를 적용한다. |
| `offerId` | UUID | 예 | P9 Offer 식별자. P9 테이블에 대한 DB 외래 키는 생성하지 않는다. |
| `quantity` | INTEGER | 예 | 1 이상의 정수이며 해당 Offer의 `maxPurchaseQuantity` 이하여야 한다. |
| `createdAt` | `TIMESTAMP WITH TIME ZONE` | 예 | 생성 시각 |
| `updatedAt` | `TIMESTAMP WITH TIME ZONE` | 예 | 마지막 변경 시각 |

### 2-3. 관계와 제약

- `(cartId, offerId)`는 UNIQUE다. 같은 Offer를 다시 추가하면 새 행을 만들지 않고 수량을 합산한다.
- Cart는 회원당 하나만 존재한다. 회원 Cart의 논리 식별자는 `userId`이며 `expiresAt`은 `NULL`이다.
- 비회원 Cart는 `expiresAt`이 필수이고 `guest_cart_id`의 `Max-Age`와 같은 30일 기준을 사용한다.
- 한 Cart의 서로 다른 Offer 수는 최대 50개, 모든 Cart Item 수량의 합은 최대 1,000개다.
- `quantity = 0` 변경은 Cart Item 삭제로 처리한다. 저장되는 Cart Item의 수량은 항상 1 이상이다.
- P9 Offer의 `maxPurchaseQuantity`는 P9 공개 계약으로 조회한다. P3는 Offer별 제한 값을 소유하거나 복제하지 않는다.
- Cart Item 추가·수정·삭제와 수량 합산은 Cart의 유일성·전체 제한을 함께 검증하는 원자적 변경이어야 한다.
- 결제 성공 후 Cart Item 정리는 이벤트 재전달에도 같은 결과가 되도록 멱등적으로 처리한다.

## 3. API 정의

성공 응답은 공통 봉투 없이 P3 Response DTO를 직접 반환한다. 비회원 상품 추가 시 새 `guest_cart_id` 또는 갱신된 만료 시각을 `Set-Cookie`로 반환한다.

### 3-1. 장바구니 조회

`GET /api/v1/cart`

권한: 로그인 사용자

비회원의 장바구니 화면 조회는 `401 AUTH-001`이다. 로그인 성공 후 P11 Auth가 게스트 장바구니를 병합하면 클라이언트는 이 API를 다시 호출한다.

#### 성공 응답: `200 OK`

```json
{
  "cartId": "uuid",
  "items": [
    {
      "cartItemId": "uuid",
      "offerId": "uuid",
      "name": "무선 헤드폰",
      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
      "quantity": 2,
      "unitPrice": { "amount": 49900, "currency": "KRW" },
      "subtotal": { "amount": 99800, "currency": "KRW" },
      "outOfStock": false,
      "unavailable": false
    }
  ],
  "itemCount": 1,
  "totalQuantity": 2,
  "subtotal": { "amount": 99800, "currency": "KRW" },
  "total": { "amount": 99800, "currency": "KRW" }
}
```

- `itemCount`는 서로 다른 Offer의 개수이고 `totalQuantity`는 모든 Cart Item 수량의 합이다.
- `subtotal`과 `total`은 상품 금액 합계이며 배송비를 포함하지 않는다.
- 가격과 상품 상태는 조회 시 P2·P9 공개 계약으로 재확인한다. 품절은 `outOfStock`, 판매 중지·보관은 `unavailable`로 표시한다.
- 조회는 성공하되 구매 불가능한 항목을 삭제하지 않는다. P5는 주문 생성과 결제 직전에 조건을 다시 검증한다.
- 주문 화면에서 장바구니로 돌아온 요청이면 P5 OrderSession의 현재 디바이스 쿠키만 삭제하는 `Set-Cookie`를 함께 반환한다. 세션 규칙은 [Order Session](../p5/p5-order-session.md)을 따른다.
- Cart 응답에는 P2 내부 `catalogProductId`·`variantId`를 포함하지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |

### 3-2. 상품 추가

`POST /api/v1/cart/items`

권한: 회원·비회원

요청:

```json
{
  "offerId": "uuid",
  "quantity": 2
}
```

서버는 Offer의 존재·판매 가능 상태·`maxPurchaseQuantity`를 확인하고 기존 항목이면 수량을 합산한다. 신규 비회원 Cart면 UUID와 `guest_cart_id`를 생성한다.

#### 성공 응답: `204 No Content`

응답 본문은 없다. 비회원은 `Set-Cookie: guest_cart_id=...; Max-Age=2592000; HttpOnly; Secure; SameSite=Lax; Path=/`를 사용한다. 실제 운영 환경에서는 `Secure`를 적용하며 쿠키 값은 URL·본문·로그에 기록하지 않는다.

#### 예외

이 표에서 `CART-xxx`는 P3가 소유하는 예외 코드다. Offer 미존재는 P9가 소유하므로 [P9 Offer](../p9/p9-offer.md)의 원본 코드를 사용한다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CART-002` | `offerId` 형식 또는 `quantity`가 올바르지 않음 | 상품과 수량을 확인해 주세요. | 실패 필드와 수정 방법 | 입력 형식·검증 원인 |
| 400 | `CART-003` | 합산 수량이 Offer의 `maxPurchaseQuantity` 초과 | 구매 가능한 수량을 초과했습니다. | `offerId`, 허용 최대 수량 | P9 구매 제한 조회 결과 |
| 400 | `CART-004` | 서로 다른 Offer가 50개 초과 | 장바구니 상품 종류가 너무 많습니다. | 최대 종류 수 `50` | Cart 항목 수 |
| 400 | `CART-005` | 전체 수량이 1,000개 초과 | 장바구니 수량이 너무 많습니다. | 최대 전체 수량 `1000` | Cart 수량 합계 |
| 404 | [OFFER-001](../p9/p9-offer.md) | — | — | — | — |
| 409 | `CART-006` | Offer가 판매 중지·보관되어 구매 불가 | 현재 구매할 수 없는 상품입니다. | `offerId` | P9 상태와 의존 리소스 상태 |

Offer 미존재의 원본 정의는 [P9 Offer](../p9/p9-offer.md)를 따른다. P3는 P9의 비공개 예외·내부 원인을 클라이언트에 노출하지 않는다.

### 3-3. 수량 변경

`PATCH /api/v1/cart/items/{cartItemId}`

권한: 로그인 사용자

요청:

```json
{
  "quantity": 3
}
```

`quantity`가 `0`이면 해당 Cart Item을 삭제한다. 비회원은 개별 수량 변경을 사용할 수 없고 `AUTH-001`을 반환한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `CART-002` | 수량이 정수가 아니거나 음수 | 수량을 확인해 주세요. | `quantity`와 수정 방법 | 입력 형식·검증 원인 |
| 400 | `CART-003` | Offer의 구매 제한 초과 | 구매 가능한 수량을 초과했습니다. | 허용 최대 수량 | P9 구매 제한 조회 결과 |
| 400 | `CART-005` | 변경 후 전체 수량이 1,000개 초과 | 장바구니 수량이 너무 많습니다. | 최대 전체 수량 `1000` | Cart 수량 합계 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 404 | `CART-001` | 항목이 없거나 요청자의 Cart가 아님 | 장바구니 항목을 찾을 수 없습니다. | 없음 | Cart·소유자 조회 원인 |
| 404 | [OFFER-001](../p9/p9-offer.md) | — | — | — | — |
| 409 | `CART-006` | Offer가 판매 중지·보관됨 | 현재 구매할 수 없는 상품입니다. | `offerId` | P9 상태와 의존 리소스 상태 |

### 3-4. Cart Item 삭제

`DELETE /api/v1/cart/items/{cartItemId}`

권한: 로그인 사용자

#### 성공 응답: `204 No Content`

삭제된 항목이 이미 없거나 다른 사용자의 항목이면 성공으로 위장하지 않고 `CART-001`을 반환한다. 비회원은 개별 삭제를 사용할 수 없다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 404 | `CART-001` | 항목이 없거나 요청자의 Cart가 아님 | 장바구니 항목을 찾을 수 없습니다. | 없음 | Cart·소유자 조회 원인 |

### 3-5. 장바구니 전체 비우기

`DELETE /api/v1/cart/items`

권한: 회원·비회원

- 회원은 Cart Item을 모두 삭제하고 회원 Cart 자체는 유지한다.
- 비회원은 쿠키로 식별된 Cart Item과 Cart를 함께 삭제하고 `guest_cart_id`를 `Max-Age=0`으로 폐기한다.
- 비회원 쿠키가 없거나 이미 만료된 경우에도 결과가 빈 장바구니와 같으므로 `204`를 반환한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |

### 3-6. 로그인 후 비회원 Cart 병합 계약

이 작업은 P3가 별도의 공개 HTTP URI로 제공하지 않는다. 로그인 성공 처리는 P11 Auth가 담당하고, P11은 P3가 공개한 모듈 계약에 인증된 `userId`와 `guest_cart_id`를 전달한다.

- 회원 Cart가 없으면 `id = userId`인 Cart를 준비한 뒤 병합한다.
- 같은 Offer는 수량을 합산하고 종류·Offer별·전체 수량 제한을 다시 적용한다.
- 병합은 원자적으로 처리한다. 제한 초과나 유효하지 않은 Offer가 하나라도 있으면 회원 Cart를 변경하지 않고 게스트 Cart와 쿠키를 유지하며 문제 목록을 반환한다.
- 병합 성공 시 게스트 Cart의 항목과 Cart를 삭제하고 `guest_cart_id`를 즉시 폐기한다.

병합 계약은 다음 결과를 반환한다.

| 결과 | 코드 | 조건 | details |
|---|---|---|---|
| 성공 | `MERGED` | 모든 항목이 제한을 만족하며 병합됨 | 병합된 Cart Item 수 |
| 실패 | `CART-007` | Offer별·종류·전체 수량 제한을 만족할 수 없음 | 충돌한 Offer와 제한 정보 |
| 실패 | [OFFER-001](../p9/p9-offer.md) | 병합 대상 Offer 원본 예외 참조 | — |

실패 결과의 원본 예외는 P11 로그인 응답에서 재정의하지 않고 P3 병합 계약을 참조한다. 실패 시 회원 Cart·게스트 Cart·쿠키는 모두 유지한다.
- 만료된 게스트 Cart는 과거 항목을 반환하지 않고 폐기 대상으로 처리한다.

## 4. 결제 완료 후 Cart Item 정리

P5가 결제 성공 사실을 확정한 뒤 주문에 포함된 `cartItemId`를 P3의 공개 계약으로 전달한다. P3는 해당 항목만 삭제하고, 이미 삭제된 항목이 포함되어도 전체 처리가 실패하지 않도록 멱등적으로 처리한다. 결제 실패·주문 실패 시 아직 결제되지 않은 Cart Item은 유지한다.
