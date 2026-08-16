# P9 Inventory API

이 문서는 `Inventory` 데이터 모델과 재고 조정 API를 정의한다. 업무 정책은 [P9 Policy](p9-policy.md), Offer 생성·소유권은 [P9 Offer API](p9-offer.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Inventory` | Offer별 현재 구매 가능 수량 | 판매자·관리자 재고 조정 |
| `Offer` | 재고 대상과 판매 가능 상태 | [P9 Offer API](p9-offer.md) |

- Inventory는 Offer 생성 시 함께 생성되고 Offer당 하나만 존재한다.
- Inventory 생성·삭제 API는 제공하지 않는다.
- 결제·주문 상태에 따른 차감·복원은 P5/P6 이벤트 계약을 사용한다.

## 2. 데이터 모델

### 2-1. Inventory

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `inventoryId` | UUID | 예 | Inventory 식별자 |
| `offerId` | UUID | 예 | 대상 Offer. Offer당 하나 |
| `quantity` | INTEGER | 예 | 현재 수량. 0 이상 |
| `createdAt` | TIMESTAMP | 예 | Offer와 함께 생성된 시각 |
| `updatedAt` | TIMESTAMP | 예 | 수량 최종 변경 시각 |

`availabilityStatus`는 저장 필드가 아니라 다음 규칙으로 계산한다.

| 조건 | 값 |
|---|---|
| `quantity > 0`이고 Offer가 공개 가능함 | `IN_STOCK` |
| `quantity = 0` 또는 Offer가 공개 가능하지 않음 | `OUT_OF_STOCK` |

### 2-2. 관계와 제약

- `offerId`는 UNIQUE다. 하나의 Offer에 Inventory 하나만 연결한다.
- Offer 생성 트랜잭션에서 `quantity = 0`으로 생성한다.
- `quantity`는 음수가 될 수 없다.
- 재고 조정 결과가 음수가 되면 전체 조정을 실패시킨다.
- 재고 조정은 동시성 제어를 적용한다.
- 조정 작업의 응답에는 `previousQuantity`, `currentQuantity`, `reason`, `adjustedBy`, `adjustedAt`을 포함한다. 현재 범위에서는 별도 `InventoryAdjustment` aggregate를 두지 않는다.
- 고객 응답에는 정확한 수량을 반환하지 않고 `availabilityStatus`만 반환한다. 판매자·관리자 본인 조회에는 `inventoryQuantity`를 포함할 수 있다.

## 3. API 정의

### 3-1. 판매자 재고 조정

`POST /api/v1/seller/offers/{offerId}/inventory-adjustments`

권한: Offer 소유 `PRODUCT_MANAGER`와 `ACTIVE Seller`.

요청:

```json
{
  "quantityDelta": 20,
  "reason": "RESTOCK"
}
```

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "previousQuantity": 0,
  "currentQuantity": 20,
  "availabilityStatus": "IN_STOCK",
  "reason": "RESTOCK",
  "adjustedBy": "uuid",
  "adjustedAt": "2026-08-16T12:10:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `INVENTORY-001` | `quantityDelta`·`reason` 형식 오류 | 재고 조정값을 확인해 주세요. | 실패 필드 | 재고 조정 입력 검증 실패 |
| 400 | `INVENTORY-002` | 적용 후 수량이 음수 | 재고는 0보다 작을 수 없습니다. | 현재·요청 수량 | 재고 하한 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-002](../p8/p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-003](../p8/p8-seller-profile.md) | — | — | — | — |
| 404 | [OFFER-001](p9-offer.md) | — | — | — | — |
| 409 | [OFFER-005](p9-offer.md) | — | — | — | — |

### 3-2. 관리자 재고 조정

`POST /api/v1/admin/offers/{offerId}/inventory-adjustments`

권한: `ADMIN`.

요청과 성공 응답은 판매자 재고 조정 API와 같다. 관리자는 Seller 상태와 관계없이 운영 조정을 수행할 수 있지만 `ARCHIVED` Offer의 재고는 조정할 수 없다.

#### 성공 응답: `200 OK`

```json
{
  "offerId": "uuid",
  "previousQuantity": 0,
  "currentQuantity": 20,
  "availabilityStatus": "IN_STOCK",
  "reason": "RESTOCK",
  "adjustedBy": "uuid",
  "adjustedAt": "2026-08-16T12:10:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `INVENTORY-001` | 요청 필드 오류 | 재고 조정값을 확인해 주세요. | 실패 필드 | 관리자 재고 조정 입력 검증 실패 |
| 400 | `INVENTORY-002` | 적용 후 수량이 음수 | 재고는 0보다 작을 수 없습니다. | 현재·요청 수량 | 재고 하한 검증 실패 |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [ADMIN-001](../p7/p7-admin.md#4-공통-예외) | — | — | — | — |
| 404 | [OFFER-001](p9-offer.md) | — | — | — | — |
| 409 | [OFFER-005](p9-offer.md) | — | — | — | — |

## 4. 외부 연동과 보상

- `PaymentCompletedEvent` 이후 재고 차감을 처리한다.
- 재고 차감 실패 시 P6 Saga 계약에 따라 결제 환불·주문 취소 보상을 수행한다.
- 주문 취소 또는 Saga 보상 시 재고를 복원한다.
- 이벤트 발행·멱등 소비·보상 상태는 [P6 Infrastructure](../p6/p6-infrastructure.md), 결제 완료 시점은 [P5 Payment Process](../p5/p5-payment-process.md)를 따른다.
