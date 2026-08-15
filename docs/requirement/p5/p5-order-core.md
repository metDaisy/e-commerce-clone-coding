# P5 Order Core (주문 모델·생명주기)

주문 데이터 모델과 상태 생명주기를 정의한다. 주문서 생성은 [P5 Order Checkout](p5-order-checkout.md), 주문 조회와 목록은 [P5 Order History](p5-order-history.md)를 따른다.

## 1. 데이터 모델

### 1-1. Order

`Order`는 주문 전체의 생명주기와 금액·배송지 스냅샷을 소유하는 집계 루트다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | 사용자에게 표시하는 주문 식별자 |
| `userId` | UUID | 주문자 식별자. P1 User의 ID를 저장하며 DB FK는 생성하지 않는다. |
| `checkoutKey` | String | 로그인 사용자의 선택한 `cartItemIds` 조합에서 서버가 계산하는 내부 식별값. API 응답에는 포함하지 않는다. |
| `status` | Enum | `PENDING`, `PAID`, `CANCELED`, `EXPIRED` |
| `expiresAt` | Instant | 신규 생성 또는 갱신 시각 후 24시간 뒤의 만료 시각 |
| `subtotalAmount` | Decimal | Order Item 소계의 합(KRW) |
| `discountAmount` | Decimal | 쿠폰 할인액(KRW) |
| `pointUsedAmount` | Integer | 사용한 포인트 |
| `paidAmount` | Decimal | `subtotalAmount - discountAmount - pointUsedAmount`(KRW) |
| `shippingRecipientName` | String nullable | 최종 결제 시 확정한 배송지 수령인 스냅샷. `PENDING`에서는 null일 수 있다. |
| `shippingRecipientPhone` | String nullable | 최종 결제 시 확정한 배송지 연락처 스냅샷. `PENDING`에서는 null일 수 있다. |
| `shippingPostalCode` | String nullable | 최종 결제 시 확정한 배송지 우편번호 스냅샷. `PENDING`에서는 null일 수 있다. |
| `shippingAddressLine` | String nullable | 최종 결제 시 확정한 배송지 기본 주소·상세 주소 스냅샷. `PENDING`에서는 null일 수 있다. |
| `createdAt` | Instant | 주문 생성 시각 |
| `updatedAt` | Instant | 주문 최종 수정 시각. `EXPIRED` 전환 시각으로도 사용한다. |
| `canceledAt` | Instant nullable | 주문 취소 시각 |

### 1-2. OrderItem

`OrderItem`은 주문 생성 또는 `PENDING` 주문 갱신 시점의 상품 표시 정보와 가격을 보존한다. Order가 `PAID`, `CANCELED`, `EXPIRED`로 전이된 뒤에는 CatalogProduct, ProductVariant, Offer가 변경되거나 보관되어도 OrderItem을 변경하지 않는다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | 주문 항목 식별자 |
| `orderId` | UUID | 소속 Order 식별자. 같은 P5 내부 관계다. |
| `offerId` | UUID | 주문 당시 Offer 식별자 |
| `userCouponId` | UUID nullable | 해당 OrderItem에 적용한 P4 UserCoupon의 논리 참조. 쿠폰을 적용하지 않으면 null이다. |
| `catalogProductName` | String | 상품명 스냅샷 |
| `variantDisplayName` | String | Variant 표시명 스냅샷 |
| `quantity` | Integer | 주문 수량. 1 이상 |
| `unitPrice` | Decimal | 주문 당시 적용 단가(KRW) |
| `subtotal` | Decimal | `unitPrice × quantity`(KRW) |
| `couponDiscountAmount` | Integer | 해당 OrderItem에 적용한 쿠폰 할인액(KRW). 없으면 0이다. |
| `createdAt` | Instant | 주문 항목 생성 시각 |

`offerId`는 판매자 주문 조회와 구매 자격 검증을 위해 내부에 보존한다. 상품명과 Variant 표시명은 스냅샷으로 보존하므로 현재 OrderItem에는 `catalogProductId`와 `productVariantId`를 저장하지 않는다.

## 2. checkoutKey

`checkoutKey`는 요청 본문으로 받지 않고 서버가 `cartItemIds`만으로 계산한다. `userId`는 키에 포함하지 않으며 DB 유일성 제약에서 함께 사용한다.

```text
normalize(cartItemIds):
  1. 하나 이상의 UUID인지 검증한다.
  2. 중복 UUID가 있으면 오류로 처리한다.
  3. UUID를 하이픈이 포함된 소문자 canonical 문자열로 변환한다.
  4. UUID 문자열을 ASCII 오름차순으로 정렬한다.
  5. 쉼표로 연결한다.

checkoutKey = SHA-256(UTF-8("v1|" + normalize(cartItemIds)))의 소문자 hex 문자열
```

순수함수이므로 입력이 같으면 항상 같은 결과를 반환한다. `cartItemIds`의 순서는 의미가 없으므로 `[A, B]`와 `[B, A]`는 같은 키다. 수량·쿠폰·포인트는 키에 포함하지 않으므로 수량이나 사용 포인트가 변경되어도 같은 `PENDING` 주문을 최신 요청으로 갱신한다.

예시는 다음과 같다.

```text
[A, B] == [B, A]
[A(quantity=1)] == [A(quantity=3)]
[A] != [A, B]
```

동일 사용자의 동일한 `checkoutKey`에는 `PENDING` 주문을 하나만 유지한다. DB에는 다음 부분 유일 제약을 둔다.

```text
UNIQUE (userId, checkoutKey)
WHERE status = 'PENDING'
```

`PAID`, `CANCELED`, `EXPIRED` 주문은 같은 조합으로 여러 건이 존재할 수 있다.

## 3. 관계와 불변식

| 관계 | 카디널리티 | 규칙 |
|---|---:|---|
| `Order` : `OrderItem` | 1 : N | 주문은 하나 이상의 항목을 가지며, 항목은 Order 없이 존재할 수 없다. |
| `OrderItem` : `UserCoupon` | 논리 참조 | 해당 주문 항목에 적용한 쿠폰을 가리키며, 결제 성공 시 사용 처리하고 취소 시 복원한다. |
| `Order` : `Payment` | 1 : N | Payment 이력과의 논리적 관계다. 최종 결제·실패·재시도·환불 규칙은 [P5 Payment](p5-payment.md)에 둔다. |
| `Order` : `Delivery` | 1 : 0..1 | Delivery와의 논리적 관계다. 결제 성공 후 배송 생성·상태 규칙은 [P5 Delivery](p5-delivery.md)에 둔다. |
| `Order` : `Address` | 논리 참조 | 주문 화면에서 별도 조회한 주소를 최종 결제 시 복사하며 이후 P1 Address 변경의 영향을 받지 않는다. |

- 선택하지 않은 Cart Item은 주문에 포함하지 않고 기존 장바구니에 유지한다.
- `subtotalAmount`는 모든 OrderItem의 `subtotal` 합과 같다.
- `discountAmount`는 모든 OrderItem의 `couponDiscountAmount` 합과 같다.
- `couponDiscountAmount`는 0 이상이며 해당 OrderItem의 결제 대상 금액을 초과할 수 없다.
- `paidAmount`는 0보다 작을 수 없다.
- `PENDING` Order의 OrderItem 가격·상품명·Variant 표시명은 재검증 시 최신 요청 기준으로 갱신할 수 있다. Order가 `PAID`, `CANCELED`, `EXPIRED`로 전이된 뒤에는 변경하지 않는다.

## 4. 상태와 만료

| 상태 | 설명 | 허용 전이 |
|---|---|---|
| `PENDING` | 결제 대기(24시간) | `PAID`, `CANCELED`, `EXPIRED` |
| `PAID` | 결제 완료 | `CANCELED` |
| `CANCELED` | 사용자 취소 또는 보상 완료 | 없음 |
| `EXPIRED` | 결제 대기 시간 만료 | 없음 |

- 신규 주문은 `expiresAt`을 `createdAt + 24시간`으로 설정한다.
- 기존 `PENDING` 주문을 최신 요청으로 갱신하면 `expiresAt`을 갱신 시각 + 24시간으로 연장한다.
- 사용자가 뒤로 가기하거나 접속을 종료해도 서버는 즉시 주문을 변경하지 않는다. `PENDING` 주문은 만료 시각까지 유지한다.
- 스케줄러는 만료된 주문을 원자적으로 `PENDING → EXPIRED`로 변경하고 `updatedAt`을 기록한다.
- 주문 생성 시점에는 재고·쿠폰·포인트를 차감하거나 배송을 생성하지 않으므로 만료 시 별도 복원 처리가 필요하지 않다.
- `EXPIRED` 주문에는 결제를 요청할 수 없다. 결제가 필요하면 장바구니에서 새 주문을 생성한다.

## 5. 주문 취소

`POST /api/v1/orders/{orderId}/cancel`

- `PENDING`은 즉시 취소한다. 성공한 결제와 배송은 존재하지 않으며, 이전 결제 실패 이력은 Payment에 남을 수 있다.
- `EXPIRED`는 이미 종단 상태이므로 취소하거나 결제할 수 없다.
- `PAID`는 배송 상태가 `PREPARING`일 때만 취소할 수 있다.
- `PAID` 취소 시 [P5 Payment](p5-payment.md)의 결제를 환불하고 쿠폰·포인트·재고를 복원한다.
- `PAID` 취소 시 [P5 Delivery](p5-delivery.md)의 배송을 `PREPARING → CANCELED`로 변경한다.
- 배송이 `SHIPPED`, `IN_TRANSIT`, `DELIVERED`이면 취소할 수 없다.
- 부분 취소는 지원하지 않으며 주문 전체를 취소한다.
- 보상 흐름의 재시도와 실패 처리는 [P6 Outbox & Saga](../p6/p6-infrastructure.md)를 따른다.

응답:

```json
{
  "orderId": "uuid",
  "status": "CANCELED",
  "refundStatus": "COMPLETED",
  "deliveryStatus": "CANCELED",
  "restoredPointAmount": 5000,
  "restoredCoupon": true,
  "restoredInventory": true,
  "canceledAt": "2026-08-09T12:10:00Z"
}
```

`PENDING` 주문 취소의 `refundStatus`는 `NOT_REQUIRED`, `deliveryStatus`는 `null`이다.
