# P5 Order History API

주문 상세와 주문 목록을 정의한다. Order 모델은 [Order API](p5-order.md), 주문 화면의 유효 세션은 [Order Session](p5-order-session.md)을 따른다.

## 1. 주문 상세

`GET /api/v1/orders/{orderId}`

권한: 로그인한 주문 소유자 본인. `PENDING` 주문은 유효한 OrderSession이 필요하고, 결제 완료·취소·만료 주문은 일반 본인 인증으로 조회한다.

### 성공 응답: `200 OK`

```json
{
  "orderId": "order-uuid",
  "status": "PAID",
  "items": [
    {
      "productName": "NVIDIA GPU 5080",
      "variantDisplayName": "16GB",
      "quantity": 1,
      "lineAmount": 1200000
    }
  ],
  "totalAmount": 1200000,
  "shippingAddress": {
    "recipient": "홍길동",
    "address1": "서울시",
    "address2": "101호"
  },
  "paymentStatus": "SUCCESS",
  "delivery": {
    "deliveryId": "delivery-uuid",
    "status": "PREPARING"
  },
  "createdAt": "2026-08-16T12:00:00Z"
}
```

### 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 403 | `ORDER-002` | 다른 사용자의 주문 | 주문을 조회할 수 없습니다. |
| 404 | `ORDER-001` | 주문이 없음 | 주문을 찾을 수 없습니다. |
| 423 | `ORDER-003` | 세션 없는 `PENDING` 주문 | 주문 화면 세션이 필요합니다. |

## 2. 주문 목록

`GET /api/v1/orders`

주문 목록은 페이지 기반 조회를 사용한다. 목록에는 결제 완료 또는 취소된 주문만 표시한다.

### Query parameters

| 파라미터 | 설명 |
|---|---|
| `page` | 0부터 시작하는 페이지 번호, 기본 `0` |
| `size` | 페이지 크기, 기본 `20`, 최대 `100` |
| `startDate` | 구매일 시작일(포함) |
| `endDate` | 구매일 종료일(포함) |
| `period` | `3M`, `6M`, `1Y` 최근 기간 preset |
| `keyword` | 구매 상품 검색어, 심화 과정 |

`period`와 `startDate`·`endDate`를 동시에 전달하면 `400 ORDER-005`를 반환한다. 날짜를 생략하면 기본 기간은 최근 3개월이다.

### 성공 응답: `200 OK`

```json
{
  "data": [
    {
      "orderId": "order-uuid",
      "status": "PAID",
      "summary": "NVIDIA GPU 5080 외 2건",
      "purchasedAt": "2026-08-16T12:00:00Z",
      "totalAmount": 1200000,
      "deliveryStatus": "PREPARING"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

목록의 상태 값은 `PAID`와 `CANCELED`다. 배송 상태는 결제 완료 주문에 대해서만 현재 Delivery 상태를 표시한다.

## 3. 키워드 검색

키워드 검색은 심화 과정으로 둔다. 사용자가 지정한 기간의 주문 항목 중 CatalogProduct, ProductVariant, Offer의 설명·표시 정보와 일치하는 주문을 반환한다.

초기 구현은 조회 시 조합하는 단순 검색을 기준으로 하며, 전문 검색 인덱스·비정규화·검색어 정규화 등 최적화 방법은 별도 설계 과제로 남긴다.

## 4. 예외

| HTTP | exceptionCode | 발생 조건 | client message |
|---:|---|---|---|
| 400 | `ORDER-004` | 날짜 형식·범위 오류 | 조회 기간을 확인해주세요. |
| 400 | `ORDER-005` | preset과 직접 기간 동시 사용 | 조회 조건을 하나만 선택해주세요. |
| 400 | `ORDER-006` | 페이지·크기 범위 오류 | 페이지 조건을 확인해주세요. |

공통 페이지 응답과 예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
