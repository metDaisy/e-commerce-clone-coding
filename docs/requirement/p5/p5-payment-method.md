# P5 Payment Method API

사용자가 저장한 결제 수단의 등록·조회·삭제를 정의한다. 실제 결제 흐름은 [Payment Process](p5-payment-process.md), 정책은 [P5 Policy](p5-policy.md)를 따른다.

## 1. 데이터 모델과 API 관계

### 1-1. `PaymentMethod`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `paymentMethodId` | UUID | O | 결제 수단 식별자 |
| `userId` | UUID | O | 소유자 |
| `type` | Enum | O | `CARD` |
| `brand` | String | O | 카드 브랜드 |
| `maskedNumber` | String | O | 예: `****-****-****-1234` |
| `expiryMonth` | Integer | O | 만료 월 |
| `expiryYear` | Integer | O | 만료 연도 |
| `providerToken` | String | O | Simulator 결제용 토큰 |
| `deletedAt` | Instant | - | 논리 삭제 시각 |
| `createdAt` | Instant | O | 생성 시각 |

카드번호 원문, CVC, 인증 원문은 저장하지 않는다. 카드번호 형식과 Luhn check digit, 만료일 형식은 등록 시 검증하지만 실제 카드 인증은 Payment Simulator가 담당한다.

## 2. API 정의

### 2-1. 결제 수단 등록

`POST /api/v1/payment-methods`

권한: 로그인 사용자. 주문 화면에서 호출하면 OrderSession의 활동으로 인정한다.

요청 예시:

```json
{
  "cardNumber": "4111111111111111",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "cvc": "123"
}
```

#### 성공 응답: `201 Created`

```json
{
  "paymentMethodId": "payment-method-uuid",
  "type": "CARD",
  "brand": "VISA",
  "maskedNumber": "****-****-****-1111",
  "expiryMonth": 12,
  "expiryYear": 2030
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 400 | `PAYMENT-003` | 카드번호·만료일·CVC 형식 오류 | 결제 수단 정보를 확인해주세요. |
| 409 | `PAYMENT-004` | 동일 수단 중복 등록 | 이미 등록된 결제 수단입니다. |

### 2-2. 결제 수단 조회

`GET /api/v1/payment-methods`

권한: 로그인 사용자. 본인이 등록한 삭제되지 않은 결제 수단만 반환한다.

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "paymentMethodId": "payment-method-uuid",
      "type": "CARD",
      "brand": "VISA",
      "maskedNumber": "****-****-****-1111",
      "expiryMonth": 12,
      "expiryYear": 2030
    }
  ]
}
```

### 2-3. 결제 수단 삭제

`DELETE /api/v1/payment-methods/{paymentMethodId}`

권한: 소유자 본인. 논리 삭제하며 이미 삭제된 결제 수단은 멱등하게 처리한다.

#### 성공 응답: `204 No Content`

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 403 | `PAYMENT-005` | 다른 사용자의 결제 수단 | 결제 수단을 삭제할 수 없습니다. |
| 404 | `PAYMENT-006` | 결제 수단이 없음 | 결제 수단을 찾을 수 없습니다. |
