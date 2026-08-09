# E-Commerce Domain Requirements (기능 명세서)

이 문서는 도메인 요구사항의 인덱스와 모든 API에 공통으로 적용되는 계약을 정의한다.

## 도메인 요구사항 문서

구체적인 도메인 규칙은 다음 문서를 기준으로 한다.

| 도메인 | 문서 |
|---|---|
| P1 User & Auth | [p1-user-auth.md](p1-user-auth.md) |
| P2 Catalog & Inventory | [p2-product.md](p2-product.md) |
| P3 Cart | [p3-cart.md](p3-cart.md) |
| P4 Coupon | [p4-coupon.md](p4-coupon.md) |
| P5 Order, Payment, Delivery | [p5-order-payment-delivery.md](p5-order-payment-delivery.md) |
| P6 Outbox & Saga | [p6-infrastructure.md](p6-infrastructure.md) |
| P7 Admin & Operations | [p7-admin.md](p7-admin.md) |
| P8 Seller & Marketplace | [p8-seller.md](p8-seller.md) |

현재 구현 여부는 [current-state.md](../current-state.md)를 확인한다. 구현 순서는 각 작업 요청과 현재 상태를 기준으로 결정한다.

## 문서 작성 규칙

- `요구사항`은 현재 구현해야 하는 확정 계약이다.
- `심화사항`은 현재 계약을 깨지 않고 확장해야 하는 목표다.
- API URI, 권한, 요청·응답 JSON, 성공 상태, 예외 코드를 생략하지 않는다.
- “또는”, “필요 시”, “지원할 수 있다”처럼 구현 결과를 달리 만들 수 있는 표현은 사용하지 않는다. 선택지가 필요한 경우 하나를 기본 동작으로 지정하고 나머지를 심화사항으로 분리한다.
- 데이터베이스 제약조건, 상태 전이, 트랜잭션 경계, 멱등성 규칙을 명시한다.
- 비밀번호, 토큰, OAuth secret, 결제 원문 등 민감 정보는 API 응답·로그·이벤트에 포함하지 않는다.
- 도메인 간 호출은 Spring Modulith 공개 인터페이스 또는 이벤트를 사용한다. 다른 모듈의 내부 구현 Bean 직접 주입은 금지한다.

## 공통 API 계약

### URI 규칙

- 외부 API 기본 경로는 `/api/v1`이다.
- 리소스는 복수 명사로 표현한다. 예: `/api/v1/products`, `/api/v1/orders`.
- 단일 리소스는 `/{resourceId}`를 사용한다.
- 상태 변경은 하위 동작 명사를 사용한다. 예: `/orders/{orderId}/cancel`.
- 관리자 전용 API는 `/api/v1/admin` 아래에 둔다.
- 로그인 사용자의 자기 리소스는 `/api/v1/me` 아래에 둔다.

### 성공 응답

모든 JSON 응답은 다음 봉투를 사용한다.

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-08-09T12:00:00Z"
  }
}
```

- 생성 성공은 `201 Created`, 조회·수정·삭제 성공은 기본 `200 OK`를 사용한다.
- 본문이 없는 성공은 `204 No Content`를 사용할 수 있다.
- 도메인 문서의 응답 예시는 `data` 내부 값이다.
- 서버는 모든 요청에 `requestId`를 부여하고 로그·이벤트에 같은 값을 기록한다.
- 모든 날짜·시간은 ISO-8601 UTC를 사용한다.
- 금액은 부동소수점이 아닌 `amount`와 ISO 4217 `currency` 조합으로 표현한다.

### 목록·페이지네이션 응답

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false
  },
  "error": null,
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-08-09T12:00:00Z"
  }
}
```

- 기본 `page=0`, `size=20`이다.
- 최대 `size=100`이다.
- 정렬 기준과 방향은 도메인 문서에서 명시한다.

### 커서 기반 페이지네이션

상품·리뷰·주문·포인트 원장처럼 데이터가 계속 누적되거나 목록이 자주 변경되는 조회는 커서 기반 조회를 사용한다.

- 첫 조회에서는 `cursor`를 생략하고, 다음 조회부터 응답의 `nextCursor`를 그대로 전달한다.
- `cursor`는 정렬 기준·필터 조건·마지막 항목의 위치를 포함하는 opaque 값이며 클라이언트가 해석하거나 수정하지 않는다.
- `cursor`와 `page`는 동시에 사용할 수 없다.
- `size`의 기본값은 20, 최대값은 100이다.
- 정렬은 반드시 고유한 보조 키를 포함하여 동일한 항목이 중복되거나 누락되지 않도록 한다.
- 커서 조회 응답에는 `totalElements`, `totalPages`를 포함하지 않고 `nextCursor`, `hasNext`를 포함한다.

```json
{
  "items": [],
  "nextCursor": "opaque-cursor",
  "hasNext": true
}
```

### 예외 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "요청 값이 올바르지 않습니다.",
    "details": {
      "fields": [
        {
          "field": "variant.sku",
          "reason": "required",
          "message": "필수 값입니다."
        }
      ]
    }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-08-09T12:00:00Z"
  }
}
```

### HTTP 상태 규칙

| HTTP | 의미 | 기본 코드 예시 |
|---:|---|---|
| 400 | JSON 형식·필드·값 검증 실패 | `VALIDATION_ERROR`, `INVALID_PRICE`, `INVALID_CURSOR`, `PAGINATION_PARAMETER_CONFLICT` |
| 401 | 인증 정보 없음·위조·만료 | `AUTHENTICATION_REQUIRED`, `INVALID_TOKEN` |
| 402 | 결제 승인이 거절됨 | `PAYMENT_DECLINED` |
| 403 | 인증은 됐지만 권한 없음 | `ACCESS_DENIED` |
| 404 | 요청 리소스 없음 | `PRODUCT_NOT_FOUND` |
| 409 | 중복·현재 상태와 충돌 | `SKU_ALREADY_EXISTS`, `OUT_OF_STOCK` |
| 423 | 계정 또는 리소스 잠금 | `ACCOUNT_LOCKED` |
| 429 | 요청 제한 초과 | `RATE_LIMIT_EXCEEDED` |
| 500 | 예상하지 못한 내부 오류 | `INTERNAL_SERVER_ERROR` |
| 503 | 외부 서비스·인프라 이용 불가 | `SERVICE_UNAVAILABLE` |

- 예외 코드는 대문자 `UPPER_SNAKE_CASE`다.
- 클라이언트는 `message` 문자열이 아니라 `code`를 기준으로 분기한다.
- 내부 stack trace, SQL, 토큰, 비밀번호는 `details`에 넣지 않는다.
- Bean Validation 실패는 필드별 `details.fields`를 반환한다.

### 공통 인증·권한

- 인증이 필요한 API는 HttpOnly Secure 쿠키의 Access Token을 사용한다.
- Access Token이 없거나 유효하지 않으면 `401 AUTHENTICATION_REQUIRED` 또는 `401 INVALID_TOKEN`이다.
- 로그인했지만 권한이 없으면 `403 ACCESS_DENIED`이다.
- 자기 소유 리소스가 아니면 리소스 존재 여부를 노출하지 않고 `403`으로 처리할 수 있다.

### 공통 트랜잭션·멱등성

- 하나의 API가 여러 애그리거트를 변경하면 도메인 문서에 원자성 범위를 명시한다.
- 결제·주문·재고·쿠폰 등 재시도 가능한 명령 API는 `Idempotency-Key`를 지원할 수 있으며, 심화사항에서 필수화한다.
- 이벤트 소비자는 `eventId`를 기준으로 중복 처리하지 않는다.

## 외부 참고

- Amazon 공개 화면은 상품명·가격·옵션·이미지·리뷰·판매자 표시 정보를 관찰하는 참고 자료로 사용한다.
- Amazon.com의 일반 고객 인증과 이 프로젝트의 소셜 회원가입은 동일한 개념으로 간주하지 않는다. 이 프로젝트의 `provider`는 애플리케이션이 정의한 OAuth 공급자 enum이다.
- Amazon은 제3자 웹사이트·앱이 Amazon 계정으로 로그인하게 하는 Login with Amazon을 제공한다. 이는 Amazon.com 자체가 Google·Naver·Kakao 회원가입을 제공한다는 의미가 아니다. ([공식 문서](https://developer.amazon.com/docs/login-with-amazon/documentation-overview.html))
- Amazon Catalog Items는 ASIN을 기준으로 카탈로그 상품·식별자·이미지·변형을 조회한다.
- Amazon Listings Items는 판매자별 등록을 `sellerId + seller SKU`로 관리한다.
- 상품 유형별 등록 필드는 Product Type Definitions API가 제공하는 스키마에 따라 달라진다.
