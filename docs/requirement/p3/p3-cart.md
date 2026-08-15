# P3 Cart (장바구니)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/cart` | 로그인 | 회원 장바구니 조회 |
| POST | `/api/v1/cart/items` | 회원·비회원 | 상품 추가 |
| PATCH | `/api/v1/cart/items/{cartItemId}` | 로그인 | 수량 변경 |
| DELETE | `/api/v1/cart/items/{cartItemId}` | 로그인 | 상품 삭제 |
| DELETE | `/api/v1/cart/items` | 회원·비회원 | 전체 비우기 |

## 2. 데이터 모델

### 2-1. `carts`

| 컬럼 | 타입 | NULL | 규칙 |
|---|---|---:|---|
| `id` | UUID | N | 기본 키. 회원은 `userId`, 비회원은 서버 생성 UUID를 사용한다. |
| `expires_at` | TIMESTAMP WITH TIME ZONE | Y | 비회원 장바구니만 저장한다. 만료 후 정리 작업으로 삭제한다. |
| `created_at` | TIMESTAMP WITH TIME ZONE | N | 생성 시각 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | N | 변경 시각 |

- `status`와 `user_id` 컬럼은 사용하지 않는다. 회원 장바구니는 인증된 `userId`를 `id`로 사용하고 `expires_at`은 NULL로 둔다.
- 비회원 장바구니는 충돌을 검사한 서버 생성 UUID를 `id`로 사용하고 `expires_at`은 필수다.
- 장바구니 소유자는 회원의 인증 `userId` 또는 비회원의 `guest_cart_id` 쿠키로만 검증하며, 클라이언트가 전달한 `cartId`는 사용하지 않는다.

### 2-2. `cart_items`

| 컬럼 | 타입 | NULL | 규칙 |
|---|---|---:|---|
| `id` | UUID | N | 장바구니 항목 식별자(`cartItemId`) |
| `cart_id` | UUID | N | 같은 P3 내부의 `carts.id`를 참조한다. SQL FK와 Cascade 삭제를 적용한다. |
| `offer_id` | UUID | N | P9 Offer의 식별자 값을 저장하며 SQL FK는 생성하지 않는다. CatalogProduct·ProductVariant ID는 저장하지 않는다. |
| `quantity` | INTEGER | N | 1 이상. 해당 Offer의 `maxPurchaseQuantity` 이하 |
| `created_at` | TIMESTAMP WITH TIME ZONE | N | 생성 시각 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | N | 변경 시각 |

- `(cart_id, offer_id)`는 UNIQUE다. 동일 Offer를 추가하면 새 항목을 만들지 않고 수량을 합산한다.
- `quantity`는 해당 Offer의 양의 정수 `maxPurchaseQuantity` 이하로 한다. Offer별 제한은 P9가 소유하며, P3는 공개 조회 인터페이스만 사용한다.
- `catalogProductId`와 `variantId`는 Offer에서 조회할 수 있으므로 `cart_items`에 중복 저장하지 않는다.
- 품절·판매 중지 여부와 가격은 저장하지 않고, 조회 시 Offer·Inventory의 현재 상태와 최신 Offer 가격으로 계산한다.

## 3. 요구사항

### 3-1. 기본 규칙

- 회원당 장바구니는 하나만 존재한다.
- 상품을 처음 추가하면 장바구니를 자동 생성한다.
- 단일 Offer의 장바구니 수량은 1 이상이며 해당 Offer의 `maxPurchaseQuantity`를 초과할 수 없다. 장바구니에는 별도의 플랫폼 공통 수량 상한을 두지 않는다.
- 장바구니 전체 상품 종류는 최대 50개다.
- 장바구니에 담긴 모든 Offer 수량의 합계는 최대 1,000개다.
- 동일 Offer를 다시 추가하면 기존 수량에 합산하며, Offer는 정확히 하나의 ProductVariant에 속하므로 Variant를 별도로 비교하지 않는다.
- 수량을 0으로 변경하면 항목을 삭제한다.
- 모든 조회·변경 API는 장바구니 소유자를 검증한다.
- 비회원 장바구니는 상품 추가와 전체 비우기에 사용할 수 있다. 장바구니 화면 조회·수량 변경·개별 상품 삭제·주문 생성은 로그인 사용자만 사용할 수 있다.

### 3-2. 비회원 장바구니

장바구니 데이터는 서버에 저장하고 브라우저에는 `cartId`를 식별하는 쿠키만 저장한다.

#### 쿠키 발급 및 저장

- 비회원이 상품을 처음 추가하면 서버는 UUID v4를 생성해 비회원 장바구니의 `cartId`로 사용하고, `Set-Cookie`로 `guest_cart_id`를 발급한다.
- `guest_cart_id`에는 `userId`, 이메일, 상품 정보 등 개인정보를 포함하지 않는다.
- 쿠키는 로그인 요청에도 함께 전송되어야 하므로 `HttpOnly`, `Secure(운영 환경)`, `SameSite=Lax`, `Path=/` 속성을 사용한다.
- `guest_cart_id`는 장바구니 접근에 사용되는 bearer 값이므로 URL·요청 본문·로그에 포함하지 않는다.
- 비회원 장바구니는 별도의 `userId`나 `deviceId`를 저장하지 않는다.

#### 만료 정책

- `guest_cart_id`의 `Max-Age`는 30일로 한다.
- 서버의 비회원 장바구니에도 `expiresAt`을 저장하고, 쿠키 만료 시각과 동일한 기준으로 관리한다.
- 비회원 상품 추가 또는 로그인 병합 요청이 들어오면 서버는 쿠키의 `cartId`로 장바구니를 조회한 뒤 `expiresAt`을 확인한다. 만료된 장바구니는 사용할 수 없는 것으로 처리하고, 해당 쿠키를 삭제한다.
- 만료된 장바구니의 존재 여부나 과거 상품 정보를 클라이언트에 노출하지 않는다. 만료 후 상품 추가 요청에서는 새 쿠키와 새 장바구니를 발급한다.
- 비회원 상품 추가가 실제로 발생하면 만료 시각을 30일 연장하고 쿠키의 `Max-Age`도 갱신한다. 회원 장바구니에는 비회원 만료 정책을 적용하지 않으며, 단순 조회만으로는 만료 시각을 연장하지 않는다.

#### 비회원 장바구니 비우기

- 비회원은 `DELETE /api/v1/cart/items`를 호출해 쿠키로 식별된 장바구니를 전체 비울 수 있다.
- 비우기 성공 시 비회원 `cart_items`와 `carts`를 함께 삭제하고 `guest_cart_id` 쿠키를 `Max-Age=0`으로 폐기한다.
- 비회원 쿠키가 없거나 이미 만료된 경우에도 장바구니가 비워진 상태이므로 `204 No Content`를 반환한다. 이 API는 멱등적이다.
- 이후 비회원이 상품을 다시 추가하면 새로운 UUID와 `guest_cart_id` 쿠키를 발급한다.

#### 만료 장바구니 삭제

- 쿠키 만료는 브라우저에서만 발생하므로 쿠키 만료만으로 서버 데이터가 삭제되지는 않는다. 서버는 별도의 정리 작업을 수행해야 한다.
- 스케줄러는 최소 하루 한 번 `expiresAt < 현재 시각`인 비회원 장바구니를 조회한다.
- 정리 작업은 장바구니 항목을 먼저 삭제한 후 비회원 장바구니를 삭제한다.
- 정리 작업은 여러 번 실행되어도 결과가 달라지지 않도록 멱등적으로 구현한다. 대량 데이터는 배치 단위로 처리한다.
- 요청 처리 중 만료가 확인된 장바구니는 스케줄러를 기다리지 않고 즉시 삭제할 수 있다.
- 회원 장바구니와 주문 완료·결제 이력은 이 정리 작업의 대상이 아니다.

#### 로그인 시 병합

- 비회원이 로그인하면 요청의 `guest_cart_id`로 비회원 장바구니를 찾고, `cartId = userId`인 회원 장바구니와 병합한다.
- 회원 장바구니가 없으면 `cartId = userId`인 장바구니를 생성한 후 병합한다.
- 동일 Offer는 기존 수량에 합산하며, 기본 규칙의 Offer·종류·전체 수량 제한을 병합 결과에도 적용한다.
- 병합에 실패한 상품은 임의로 삭제하지 않고 병합 결과의 문제 목록으로 반환한다.
- 병합이 성공하면 비회원 장바구니의 항목과 장바구니를 삭제하고, 기존 `guest_cart_id` 쿠키는 `Max-Age=0`으로 즉시 폐기한다.

#### 장바구니 화면 접근

클라이언트가 장바구니 화면에서 `GET /api/v1/cart`를 호출했을 때 비회원이면 `401 AUTHENTICATION_REQUIRED`를 반환한다. 클라이언트는 로그인 화면으로 이동하고, 로그인 성공 후 장바구니를 병합한 다음 다시 조회한다.

### 3-3. 상품 추가

`POST /api/v1/cart/items`

요청:

```json
{
  "offerId": "uuid",
  "quantity": 2
}
```

성공 시 신규 상품 추가와 기존 항목 수량 합산 모두 `204 No Content`를 반환한다. 응답 본문은 없으며, 회원은 `GET /api/v1/cart`로 다시 조회하고 비회원은 로그인 후 장바구니 화면에서 조회한다.

### 3-4. 장바구니 조회

`GET /api/v1/cart`

로그인 사용자만 호출할 수 있다. 비회원 호출은 `401 AUTHENTICATION_REQUIRED`로 처리한다.

응답:

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
       "unitPrice": { "amount": 49900.00, "currency": "KRW" },
       "subtotal": { "amount": 99800.00, "currency": "KRW" },
      "outOfStock": false,
      "unavailable": false
    }
  ],
  "itemCount": 1,
  "totalQuantity": 2,
  "subtotal": { "amount": 99800.00, "currency": "KRW" },
  "total": { "amount": 99800.00, "currency": "KRW" }
}
```

- `itemCount`는 서로 다른 Offer의 개수이고, `totalQuantity`는 모든 cart item의 `quantity` 합계다.
- 장바구니의 `total`은 상품 금액 합계이며 배송비는 포함하지 않는다. 현재 프로젝트는 배송비를 사용하지 않으므로 P5 주문 금액에도 배송비를 더하지 않는다.
- 구매자 응답은 내부 `catalogProductId`·`variantId`를 제외하고 장바구니 표시와 결제 검증에 필요한 정보만 반환한다.
- 조회 시 최신 Offer 가격·상품 보관 여부·구매 가능 재고를 재검증한다. 품절·보관 상품은 `outOfStock`·`unavailable`로 표시하고, P5 주문서 생성을 차단한다. 장바구니 조회 자체는 성공시킨다.
- 이 검증은 장바구니 화면에서 수행하는 1차 검증이다. P5 주문서 생성과 최종 결제 시에도 동일한 핵심 조건을 다시 검증한다.
- P5 주문 화면에서 장바구니로 이동한 뒤 호출된 경우 요청에 포함된 Checkout Cookie의 세션을 정리하고, 응답의 `Set-Cookie`로 현재 브라우저의 Checkout Cookie를 삭제한다. 세션 정리는 Cookie의 `tokenHash`에 해당하는 세션만 대상으로 한다.
- 장바구니의 주문 버튼은 선택한 `cartItemIds`, 쿠폰과 Cart Item의 매핑인 `couponApplications`, 사용할 `pointAmount`를 `POST /api/v1/orders`에 전달한다. 주소는 별도로 조회한다. 성공하면 반환된 `orderId`의 주문 화면으로 이동하고, 주문서 생성 검증에 실패하면 오류 메시지를 표시한 뒤 `/cart`로 이동한다.

### 3-5. 결제 후 처리

- 결제 성공 이벤트를 수신하면 주문에 포함된 항목을 장바구니에서 삭제한다.
- 장바구니의 일부 Cart Item만 선택해 하나의 주문으로 생성할 수 있다. 이를 `선택 상품 주문`이라 하며, 결제 금액을 나누는 분할 결제는 지원하지 않는다.
- 주문 실패 시 결제하지 않은 항목은 장바구니에 유지한다.

## 4. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청 형식·수량 오류 |
| 400 | `OFFER_QUANTITY_LIMIT_EXCEEDED` | 해당 Offer의 `maxPurchaseQuantity` 초과 |
| 400 | `CART_ITEM_COUNT_LIMIT_EXCEEDED` | 장바구니 상품 종류 50개 초과 |
| 400 | `CART_TOTAL_QUANTITY_LIMIT_EXCEEDED` | 장바구니 전체 수량 1,000개 초과 |
| 401 | `AUTHENTICATION_REQUIRED` | 장바구니 화면 조회·주문 생성에 로그인 필요 |
| 404 | `CART_ITEM_NOT_FOUND` | 항목 없음 또는 타인의 항목 접근 |
| 404 | `OFFER_NOT_FOUND` | 판매 오퍼 없음 |
| 409 | `OFFER_UNAVAILABLE` | 존재하지만 판매 중지·아카이빙된 Offer |
| 409 | `CART_MERGE_LIMIT_EXCEEDED` | 로그인 시 비회원 장바구니 병합 결과가 제한 초과 |

`GET /api/v1/cart`를 비회원이 호출하면 다음과 같이 응답한다.

- HTTP: `401`
- 코드: `AUTHENTICATION_REQUIRED`
- 클라이언트 메시지: `장바구니를 확인하려면 로그인을 해주세요.`
- 서버 로그 원인: `GUEST_CART_READ_REQUIRES_AUTHENTICATION`
