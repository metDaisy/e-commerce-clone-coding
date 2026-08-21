# E-Commerce Domain Requirements (기능 명세서)

이 문서는 도메인 요구사항의 인덱스와 모든 API에 공통으로 적용되는 계약을 정의한다.

## 도메인 요구사항 문서

구체적인 도메인 규칙은 다음 문서를 기준으로 한다.

| 도메인 | 문서 |
|---|---|
| P1 User | [P1 index](p1/p1-index.md), [Policy](p1/p1-policy.md), [User API](p1/p1-user.md), [Address API](p1/p1-address.md) |
| P2 Catalog | [P2 index](p2/p2-index.md), [Policy](p2/p2-policy.md), [Catalog](p2/p2-catalog.md), [Category](p2/p2-category.md), [CatalogProduct](p2/p2-catalog-product.md), [ProductVariant](p2/p2-product-variant.md) |
| P3 Cart | [P3 index](p3/p3-index.md), [Policy](p3/p3-policy.md), [Cart](p3/p3-cart.md) |
| P4 Coupon | [P4 index](p4/p4-index.md), [Policy](p4/p4-policy.md), [Coupon API](p4/p4-coupon.md), [Seller API](p4/p4-coupon-seller.md), [Customer API](p4/p4-coupon-customer.md), [Advanced](p4/p4-coupon-advanced.md) |
| P5 Order, Payment, Delivery | [P5 index](p5/p5-index.md), [Policy](p5/p5-policy.md), [Order](p5/p5-order.md), [Order Core](p5/p5-order-core.md), [Order Checkout](p5/p5-order-checkout.md), [Order Session](p5/p5-order-session.md), [Order History](p5/p5-order-history.md), [Payment](p5/p5-payment.md), [Payment Method](p5/p5-payment-method.md), [Payment Process](p5/p5-payment-process.md), [Delivery](p5/p5-delivery.md) |
| P6 Outbox & Saga | [P6 index](p6/p6-index.md), [Infrastructure](p6/p6-infrastructure.md) |
| P7 Admin & Operations | [P7 index](p7/p7-index.md), [Policy](p7/p7-policy.md), [Admin API](p7/p7-admin.md) |
| P8 Seller | [P8 index](p8/p8-index.md), [P8 Policy](p8/p8-policy.md), [SellerApplication API](p8/p8-seller-application.md), [Seller API](p8/p8-seller-profile.md), [CatalogRegistrationRequest API](p8/p8-catalog-requests.md), [SellerOrder API](p8/p8-seller-orders.md) |
| P9 Offer & Marketplace | [P9 index](p9/p9-index.md), [Policy](p9/p9-policy.md), [Offer API](p9/p9-offer.md), [Inventory API](p9/p9-inventory.md), [Seller Catalog API](p9/p9-seller-catalog.md), [Marketplace API](p9/p9-marketplace.md), [Exceptions](p9/p9-exceptions.md) |
| P10 Review | [P10 index](p10/p10-index.md), [Policy](p10/p10-policy.md), [Review](p10/p10-review.md) |
| P11 Auth | [P11 index](p11/p11-index.md), [Policy](p11/p11-policy.md), [Credential](p11/p11-credential.md), [Sign-up](p11/p11-signup.md), [Session](p11/p11-session.md) |
| P12 Media | [P12 index](p12/p12-index.md), [Policy](p12/p12-policy.md), [Media API](p12/p12-media.md) |

도메인 문서를 새로 작성할 때는 [요구사항 문서 템플릿](template/index.md)을 사용한다. 템플릿은 `index.md`, `policy.md`, `[resource].md`로 구성한다.

P2 상세 문서:

- [P2 Category](p2/p2-category.md)
- [P2 CatalogProduct](p2/p2-catalog-product.md)
- [P2 ProductVariant](p2/p2-product-variant.md)

P2 심화 문서:

- [P2 SearchKeyword](p2/p2-search-keyword.md)
- [P2 ProductType·ItemType](p2/p2-product-type.md)

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
- 리소스는 복수 명사로 표현한다. 예: `/api/v1/catalog-products`, `/api/v1/orders`.
- 단일 리소스는 `/{resourceId}`를 사용한다.
- 상태 변경은 하위 동작 명사를 사용한다. 예: `/orders/{orderId}/cancel`.
- 관리자 전용 API는 `/api/v1/admin` 아래에 둔다.
- 로그인 사용자의 자기 리소스는 `/api/v1/me` 아래에 둔다.

### 성공 응답

성공 응답은 공통 봉투를 사용하지 않는다. 각 도메인이 정의한 Response DTO를 HTTP 응답 본문으로 직접 반환한다. 목록 조회도 목록 데이터와 페이지네이션 필드를 포함한 도메인별 Response DTO를 직접 반환한다.

- 생성 성공은 `201 Created`, 조회·수정·삭제 성공은 기본 `200 OK`를 사용한다.
- 본문이 없는 성공은 `204 No Content`를 사용할 수 있다.
- 도메인 문서는 각 API가 반환하는 Response DTO 전체를 성공 응답 예시로 작성한다.
- 서버는 모든 요청에 `requestId`를 부여하고 `X-Request-Id` 응답 헤더와 로그·이벤트에 같은 값을 기록한다. `requestId`는 응답 본문에 포함하지 않는다.
- 모든 날짜·시간은 ISO-8601 UTC를 사용한다.
- 금액은 기본적으로 부동소수점이 아닌 `amount`와 ISO 4217 `currency` 조합으로 표현한다. 단, KRW로 고정한 P5 Order 금액은 숫자 필드만 사용하고 `currency`를 노출하지 않는다.

### 목록·페이지네이션 응답

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

- 기본 `page=0`, `size=20`이다.
- 최대 `size=100`이다.
- 다음 페이지 존재 여부는 `page + 1 < totalPages`로 판단한다.
- 정렬 기준과 방향은 도메인 문서에서 명시한다.

### 커서 기반 페이지네이션

상품·판매자 주문·포인트 히스토리처럼 데이터가 계속 누적되거나 목록이 자주 변경되는 조회는 커서 기반 조회를 사용한다. Review 목록과 구매자 주문 목록은 전체 건수와 페이지 이동이 필요하므로 페이지 기반 조회를 사용하며, 설명 키워드 검색은 심화사항으로 둔다.

- 첫 조회에서는 `cursor`를 생략하고, 다음 조회부터 응답의 `nextCursor`를 그대로 전달한다.
- `cursor`는 정렬 기준·필터 조건·마지막 항목의 위치를 포함하는 opaque 값이며 클라이언트가 해석하거나 수정하지 않는다.
- `cursor`와 `page`는 동시에 사용할 수 없다.
- `size`의 기본값은 20, 최대값은 100이다.
- 정렬은 반드시 고유한 보조 키를 포함하여 동일한 항목이 중복되거나 누락되지 않도록 한다.
- 커서 조회 응답에는 `totalElements`, `totalPages`를 포함하지 않고 `nextCursor`, `hasNext`를 포함한다.

커서 조회의 성공 응답은 다음 형식을 사용한다.

```json
{
  "data": [],
  "hasNext": true,
  "nextCursor": "opaque-cursor"
}
```

- `data`는 조회 결과 배열이다.
- 도메인별 요약값(예: 리뷰 통계, 포인트 잔액)은 `data`와 같은 최상위 필드로 추가할 수 있다.
- `hasNext`가 `false`이면 `nextCursor`는 `null`이다.
- `nextCursor`는 클라이언트가 해석하지 않는 opaque 값이다. `idAfter`를 별도로 반환하지 않는다.
- 서버는 정렬 기준·방향, 마지막 항목의 모든 정렬값, 필터 조건 식별값을 cursor payload에 포함한다.
- cursor payload는 Base64URL로 인코딩하고 서버 비밀키로 서명한다. Base64URL은 인코딩일 뿐 암호화가 아니므로 서명 검증에 실패한 cursor는 `400 INVALID_CURSOR`로 거부한다.
- 요청의 정렬·필터 조건이 cursor 생성 시점과 다르면 `400 INVALID_CURSOR`를 반환한다.
- 첫 조회에서는 `cursor`를 생략하고, 다음 조회부터 응답의 `nextCursor`를 그대로 전달한다.

### 예외 응답

예외 응답은 성공 응답과 마찬가지로 봉투를 사용하지 않는다. 모든 도메인은 다음 클라이언트용 오류 DTO를 사용한다.

```json
{
  "statusCode": 404,
  "exceptionCode": "USER-001",
  "message": "유저를 찾을 수 없습니다.",
  "details": {
    "field1": "value1",
    "field2": "value2"
  },
  "timestamp": "2026-08-16T12:31:33.333Z"
}
```

- `statusCode`는 HTTP 상태 코드와 동일하다.
- `exceptionCode`는 도메인 접두사와 일련번호를 사용하는 `USER-001` 형식이다.
- `message`는 클라이언트에 전달하는 안전한 안내 문구다.
- `details`는 선택 필드이며, 클라이언트가 입력을 수정하는 데 필요한 안전한 정보만 포함한다.
- `timestamp`는 ISO-8601 UTC 형식을 사용한다.

### 예외 응답 필드 규칙

- `statusCode`는 HTTP 표준 응답 상태 코드 체계를 따른다. 각 API 문서에는 가능한 HTTP 상태 코드를 명시한다.
- `exceptionCode`는 도메인이 정하는 백엔드·프론트엔드 간 약속된 값이다. 프론트엔드는 이 값을 기준으로 사용자에게 보여줄 예외를 결정한다.
- `exceptionCode`는 도메인과 일련번호의 조합으로 고유해야 하며, 하나의 코드에는 하나의 예외 의미만 부여한다.
- 도메인별 일련번호는 계속 증가시키고, 한 번 사용한 코드는 삭제·변경·재사용하지 않는다. 번호의 공백은 허용한다.
- `message`는 클라이언트에 전달하는 통상적이고 어느 정도 추상화된 안내 문구다. 예외 분기 기준으로 사용하지 않는다.
- `details`는 선택 필드이며, `exceptionCode`에 따라 구조와 포함 여부가 달라진다.
- `timestamp`는 서버가 예외를 생성한 시각을 `Instant.now()`로 기록한 ISO-8601 UTC 값이다. 프론트엔드는 이를 현재 사용자의 지역 시간대로 변환해 표시한다.
- 내부 기록용 메시지와 원인은 클라이언트의 `message`와 별도로 서버 로그에 기록하며, 예외 응답에 포함하지 않는다.
- API 문서는 다른 도메인이 소유한 예외도 응답 목록에 포함한다. 예외 코드·메시지·로그 규칙의 원본은 리소스 소유 도메인 문서가 정의하며, 호출 도메인은 원본 문서를 참조하고 재정의하지 않는다.

### 예측할 수 없는 내부 오류

예측하지 못한 서버 내부 오류는 모든 도메인에서 공통으로 다음 응답을 사용한다.

| HTTP | exceptionCode | client message | details |
|---:|---|---|---|
| 500 | `SYSTEM-001` | 요청을 처리하지 못했습니다. | 생략 |

실제 오류 원인과 stack trace는 `system message`와 서버 로그에만 기록하며 클라이언트 응답에는 포함하지 않는다.

### 공통 인증·권한

- 인증이 필요한 API는 HttpOnly Secure 쿠키의 Access Token을 사용한다.
- Access Token이 없거나 유효하지 않으면 `401 AUTH-001`이다.
- 비활성 User로 인증된 요청은 [P1 User의 `USER-004`](p1/p1-user.md#user-disabled-error)를 따른다. 관리자 권한 부족은 [P7 Admin](p7/p7-admin.md)의 `ADMIN-001`을 따른다.
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
- 상품 유형별 등록 필드는 Product Type Definitions API가 제공하는 스키마에 따라 달라진다.
