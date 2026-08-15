# P5 Payment Method (결제 수단)

결제 수단 등록·조회·삭제를 정의한다. 최종 결제 과정은 [P5 Payment Process](p5-payment-process.md)를 따른다.

## 1. API 목록

| Method | URI | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/payment-methods` | 로그인 | 결제 수단 등록 |
| GET | `/api/v1/payment-methods` | 로그인 | 결제 수단 조회 |
| DELETE | `/api/v1/payment-methods/{paymentMethodId}` | 로그인 | 결제 수단 삭제 |

## 2. 공통 규칙

결제 수단은 로그인한 사용자에게 귀속된다. `userId`는 요청 본문으로 받지 않고 인증된 사용자에서 결정한다.

조회·사용·삭제는 본인 소유의 결제 수단만 가능하다. 다른 사용자의 `paymentMethodId`는 존재 여부를 노출하지 않도록 `PAYMENT_METHOD_NOT_FOUND`로 처리한다.

결제 수단 목록은 현재 범위에서 페이지 기반 조회를 사용하지 않는다. 삭제는 논리 삭제이며, 삭제된 결제 수단은 이후 조회와 신규 결제에 사용할 수 없다. 과거 Payment 이력은 유지한다.

## 3. 결제 수단 등록

`POST /api/v1/payment-methods`는 사용자의 결제 수단을 등록하고 `201 Created`를 반환한다.

카드 등록 요청 예시:

```json
{
  "methodType": "CREDIT_CARD",
  "cardNumber": "4111111111111111",
  "expiryMonth": 12,
  "expiryYear": 2030
}
```

`KAKAO_PAY`와 `BANK_TRANSFER`는 `methodType`만 전달한다. 현재 프로젝트에서는 실제 카드 인증·결제를 수행하지 않으므로 카드번호의 길이·숫자 여부·Luhn 체크·유효기간만 검증한다.

카드번호와 인증번호 같은 원문 민감 정보는 저장·로그·이벤트·응답에 남기지 않는다.

응답에는 결제수단 식별과 화면 표시용 정보만 포함한다.

```json
{
  "paymentMethodId": "uuid",
  "methodType": "CREDIT_CARD",
  "provider": "PAYMENT_SIMULATOR",
  "maskedNumber": "**** **** **** 1111",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

## 4. 결제 수단 조회

`GET /api/v1/payment-methods`는 로그인한 사용자의 삭제되지 않은 결제 수단 목록을 반환한다.

주문 화면은 이 API를 호출해 결제수단을 표시하고, 사용자가 선택한 `paymentMethodId`를 [최종 결제 요청](p5-payment-process.md#3-최종-결제)에 전달한다.

응답 예시:

```json
{
  "items": [
    {
      "paymentMethodId": "uuid",
      "methodType": "CREDIT_CARD",
      "provider": "PAYMENT_SIMULATOR",
      "maskedNumber": "**** **** **** 1111",
      "createdAt": "2026-08-16T12:00:00Z"
    }
  ]
}
```

## 5. 결제 수단 삭제

`DELETE /api/v1/payment-methods/{paymentMethodId}`는 결제 수단을 논리 삭제하고 `204 No Content`를 반환한다.

주문 화면에서 선택한 결제 수단을 삭제한 경우 최종 결제 시 `PAYMENT_METHOD_NOT_FOUND`가 반환될 수 있다. 클라이언트는 결제수단을 다시 조회하고 다른 결제 수단을 선택하게 한다.

## 6. 예외

| HTTP | 코드 | 발생 조건 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 결제 수단 요청 필드·카드 형식 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 404 | `PAYMENT_METHOD_NOT_FOUND` | 결제 수단 없음 또는 타인 소유 |
