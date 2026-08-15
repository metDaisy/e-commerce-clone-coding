# P5 Order History (주문 조회·주문 목록)

주문 소유자의 주문 상세와 주문 목록 조회를 정의한다. 주문 모델과 상태는 [P5 Order Core](p5-order-core.md), 주문 화면의 사용 흐름은 [P5 Order Checkout](p5-order-checkout.md)를 따른다.

## 1. 주문 상세

`GET /api/v1/orders/{orderId}`

- 주문 생성 응답과 같은 주문서·금액 구조를 반환한다. 배송지 확정 전 `PENDING` 주문의 `shippingAddress`는 null이다.
- 결제 완료 주문은 최종 결제 시 저장한 배송지 스냅샷을 `shippingAddress`로 반환한다.
- `PENDING` 주문은 [P5 Order Session](p5-order-session.md)의 유효한 Checkout Cookie가 있을 때만 조회하고 결제를 재시도할 수 있다.
- 주문 화면은 [P5 Payment Method](p5-payment-method.md)의 `GET /api/v1/payment-methods`로 결제수단을 조회해 표시한다.
- `PAID` 주문은 `delivery`에 `deliveryId`, `status`를 포함한다.
- 구매자용 조회는 `order.userId`와 인증 사용자 ID가 같은 경우에만 허용한다.

## 2. 주문 목록

`GET /api/v1/orders`

- 페이지 기반 조회를 사용한다. 기본 Query는 `page`, `size`, 기간 조건을 사용한다.
- `page`는 0부터 시작하며 기본값은 `0`이다. 기본 `size`는 20, 최대값은 100이다.
- 기간을 지정하지 않으면 최근 3개월을 기본값으로 사용한다.
- 기간 preset은 `dateRange=LAST_3_MONTHS`, `dateRange=LAST_6_MONTHS`, `dateRange=LAST_1_YEAR`를 지원한다.
- 직접 기간을 조회할 때는 `startDate`와 `endDate`를 함께 전달하며, 두 날짜를 포함한 범위로 조회한다. `dateRange`와 `startDate`·`endDate`는 동시에 사용할 수 없다.
- 날짜 형식은 `YYYY-MM-DD`다. `startDate`는 `endDate`보다 늦을 수 없다.
- 주문 상태는 목록에 표시하지만 현재 목록 필터로 제공하지 않는다.
- 로그인한 사용자의 주문만 반환한다.
- 주문 목록에는 결제가 완료되었거나 취소·만료된 주문만 표시한다. `PENDING` 주문은 주문 화면에서 결제 대기 상태로 관리하고 주문 목록에서는 제외한다.
- 정렬은 `createdAt DESC, orderId DESC`로 고정한다.
- 주문 이력 응답은 `orderId`, `status`, `itemSummary`, `purchasedAt`, `paidAmount`, `deliveryStatus`만 반환한다. `itemSummary`는 대표 상품명을 기준으로 `NVIDIA GPU 5080 외 1개`처럼 표시한다. `offerId`, `catalogProductId`, `variantId`는 반환하지 않는다.
- 응답은 공통 페이지 형식의 `data`, `page`, `size`, `totalElements`, `totalPages`를 사용한다.

목록 항목 규칙:

- `status`는 `PAID`(결제완료), `CANCELED`(취소), `EXPIRED`(만료)를 사용한다.
- `purchasedAt`은 결제가 완료된 시각이다. 결제 전에 취소·만료된 주문은 `null`이다.
- `paidAmount`는 최종 결제 금액이며, 결제 전에 취소·만료된 주문은 `0`이다.
- `deliveryStatus`는 배송 상태이며, 결제 후 취소된 주문은 `CANCELED`, 결제 전 취소·만료 주문은 `null`이다.

예시:

`GET /api/v1/orders?page=0&size=20&dateRange=LAST_3_MONTHS`

직접 기간 조회 예시:

`GET /api/v1/orders?page=0&size=20&startDate=2026-01-01&endDate=2026-03-31`

성공 응답 `200`:

```json
{
  "data": [
    {
      "orderId": "uuid",
      "status": "PAID",
      "itemSummary": "NVIDIA GPU 5080 외 1개",
      "purchasedAt": "2026-08-09T12:00:00Z",
      "paidAmount": 89800.00,
      "deliveryStatus": "PREPARING"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## 3. 심화 사항: 구매 상품 설명 키워드 검색

- `keyword` 검색은 기본 기능에 포함하지 않는다.
- 날짜 조건으로 먼저 로그인 사용자의 주문 이력을 제한한 뒤, 해당 주문에 포함된 상품을 검색한다.
- 검색 대상은 연결된 `CatalogProduct.description`, `ProductVariant.description`, `Offer.description`의 설명 텍스트다.
- 설명 중 하나라도 키워드와 일치하면 해당 Order를 목록에 포함한다. 하나의 주문에 여러 상품이 일치해도 주문은 한 번만 반환한다.
- 검색 결과는 기존 주문 목록 요약 응답과 페이지 형식을 그대로 사용한다.
- 현재 OrderItem은 상품명과 Variant 표시명만 스냅샷으로 보존하므로, 설명의 과거 시점 보존이 필요할 경우 설명 스냅샷 또는 별도 검색 모델을 추가해야 한다.
- 대량 데이터에서의 조인, 비정규화 read model, PostgreSQL Full-Text Search 등 어떤 방식으로 최적화할지는 심화 설계에서 결정한다.
