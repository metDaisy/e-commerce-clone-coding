# P5 Delivery API

배송 서비스·운송사·fulfillment를 사용하지 않는 프로젝트에서 결제 완료 주문의 배송 상태를 임의 전환하는 규칙을 정의한다. 주문·결제와의 연결은 [P5 Policy](p5-policy.md), 이벤트 전달은 [P6 Infrastructure](../p6/p6-infrastructure.md)를 따른다.

## 1. 데이터 모델과 API 관계

### 1-1. `Delivery`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `deliveryId` | UUID | O | 배송 식별자 |
| `orderId` | UUID | O | 배송 대상 주문 |
| `status` | Enum | O | `PREPARING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED` |
| `statusChangedAt` | Instant | O | 현재 상태 진입 시각 |
| `createdAt` | Instant | O | 배송 생성 시각 |
| `updatedAt` | Instant | O | 변경 시각 |

배송지와 운송장 번호는 현재 모델에 포함하지 않는다. 배송지는 Order가 결제 확정 시 저장한 스냅샷으로 조회한다.

### 1-2. 관계와 제약

- 하나의 `PAID` Order마다 Delivery 하나를 생성한다.
- 결제 성공 Webhook 처리 전에는 Delivery를 생성하지 않는다.
- Delivery 상태는 정의된 순서로만 전환한다.
- 각 상태는 `90초` 동안 유지한 뒤 다음 상태로 전환한다.

## 2. API 정의

### 2-1. 배송 상세 조회

`GET /api/v1/deliveries/{deliveryId}`

권한: 연결된 Order의 소유자 본인.

#### 성공 응답: `200 OK`

```json
{
  "deliveryId": "delivery-uuid",
  "orderId": "order-uuid",
  "status": "PREPARING",
  "statusChangedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

| statusCode | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 403 | `DELIVERY-001` | 연결된 Order의 소유자가 아님 | 배송 정보를 조회할 수 없습니다. | 없음 | Order 소유권 검증 실패와 요청자·배송 식별자를 기록한다. |
| 404 | `DELIVERY-002` | 배송 식별자가 존재하지 않음 | 배송 정보를 찾을 수 없습니다. | 없음 | 배송 식별자와 요청 식별자를 기록한다. |

배송 조회에서 발생하는 접근 권한·존재 여부 오류는 Delivery API가 `DELIVERY_*` 코드로 반환한다. Order 소유권 확인에 필요한 내부 조회 오류는 외부 응답에 그대로 노출하지 않는다.

### 2-2. 운영 상태 전환

고객용 상태 변경 API는 제공하지 않는다. 스케줄러가 다음 전환을 수행한다.

```text
PREPARING --90초--> SHIPPED --90초--> IN_TRANSIT --90초--> DELIVERED
```

운영자 수동 전환이 필요하면 P7 Admin의 운영 API로 제공하며, 상태 전환 규칙과 잘못된 상태 전환의 예외는 이 문서를 참조한다. 스케줄러 실패는 고객 API 예외가 아니므로 시스템 로그와 P6 운영 흐름으로 처리한다.
