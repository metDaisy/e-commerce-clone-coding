# P5 Order Session (주문 화면 세션)

로그인 자체는 여러 기기에서 허용하지만, 하나의 `PENDING` 주문에 대한 주문 화면 조회·주문 갱신·최종 결제 요청은 한 기기에서만 허용한다. 결제 완료·취소·만료 주문의 일반 주문 상세와 주문 목록은 사용자 본인이라면 여러 기기에서 조회할 수 있다.

## 1. 책임과 범위

- `OrderSession`은 주문 화면의 일시적인 점유 상태만 관리한다.
- 주문의 소유권은 여전히 `Order.userId`로 검증한다.
- `checkoutKey`는 동일한 장바구니 선택에 대한 주문 재사용 식별자이고, `OrderSession`은 현재 주문 화면을 점유한 기기를 식별한다.
- 배송지·결제수단·결제 이력은 OrderSession에 저장하지 않는다.
- 현재 배포 범위는 단일 애플리케이션 인스턴스다. 활성 세션은 Caffeine만 사용하며, 애플리케이션 재시작 시 모든 주문 세션을 초기화한다.

## 2. OrderSession 데이터 모델

| 필드 | 타입 | 설명 |
|---|---|---|
| `orderId` | UUID | 대상 PENDING 주문 식별자. 주문당 활성 세션은 하나만 허용한다. |
| `userId` | UUID | 주문 소유자 식별자 |
| `tokenHash` | String | Cookie로 전달한 불투명 토큰의 해시. 원문 토큰은 저장하지 않는다. |
| `expiresAt` | Instant | 주문 관련 사용자 활동이 없을 때의 만료 시각 |
| `heartbeatExpiresAt` | Instant | 브라우저 하트비트가 끊겼을 때의 만료 시각 |

`OrderSession`은 Caffeine에 활성 세션만 보관한다. 엔트리가 존재하고 두 만료 시각이 모두 지나지 않았으면 활성 세션으로 판단한다. 세션이 만료되거나 해제되면 엔트리를 제거한다.

`OrderSession.expiresAt`은 기본 30분의 활동 만료 시각이고, `heartbeatExpiresAt`은 기본 3분의 생존 확인 만료 시각이다. 둘 다 주문 자체의 24시간 만료를 나타내는 `Order.expiresAt`과는 다르다.

## 3. Cookie와 요청 검증

주문서 생성 성공 시 서버는 랜덤한 불투명 `checkoutToken`을 발급하고 HttpOnly Cookie로 전달한다. 응답 JSON에는 원문 토큰을 포함하지 않는다.

```http
Set-Cookie: checkout_token=<opaque-token>; Path=/api/v1; HttpOnly; Secure; SameSite=Lax
```

`Path`는 브라우저 화면 경로가 아니라 Cookie를 전송할 서버 요청 URL의 경로에 적용한다. Checkout Cookie는 `/api/v1/orders/...`의 주문 조회·갱신·결제·하트비트 요청과 `/api/v1/cart`의 장바구니 진입 요청에 모두 필요하므로 현재 API 구조에서는 `Path=/api/v1`로 설정한다. `Path=/cart`로 설정하면 `/cart` 화면에서는 보이더라도 `/api/v1` API 요청에 Cookie가 전송되지 않는다. 향후 주문과 장바구니 API가 하나의 공통 경로 아래로 정리되면 그 공통 경로로 범위를 좁힌다.

Checkout Cookie에는 `Max-Age`와 `Expires`를 설정하지 않아 세션 Cookie로 발급한다. 브라우저가 정상적으로 종료되면 클라이언트 Cookie도 함께 삭제된다. 단, 서버의 `OrderSession`은 장바구니 진입 요청 같은 서버 요청으로도 종료해야 하며, 브라우저 종료 이벤트를 받지 못한 경우에는 `expiresAt` 또는 `heartbeatExpiresAt` 중 먼저 도달하는 시각까지 유지된다.

서버는 Cookie 원문을 해시해 `tokenHash`와 비교한다. `userId`, IP, User-Agent, 클라이언트가 전달한 `deviceId`만으로 기기를 식별하거나 접근을 허용하지 않는다.

### 3-1. tokenHash 생성

`checkoutToken`은 서버의 CSPRNG로 32바이트 난수를 생성해 Base64 URL-safe 문자열로 인코딩한다. `tokenHash`는 토큰 원문이 아니라 동일한 난수 바이트에 SHA-256을 적용한 소문자 hex 문자열로 저장한다.

```text
tokenBytes = SecureRandom 32 bytes
checkoutToken = Base64Url(tokenBytes, withoutPadding)
tokenHash = lowercaseHex(SHA-256(tokenBytes))
```

- `checkoutToken` 원문은 Cookie로만 전달하고 Caffeine·로그에 저장하지 않는다.
- 요청 Cookie를 Base64 URL-safe 디코딩한 뒤 같은 방식으로 SHA-256을 계산해 저장된 `tokenHash`와 비교한다.
- 비교는 constant-time 비교를 사용한다.
- `userId`, `orderId`, `deviceId`, UUID, 타임스탬프를 토큰으로 사용하지 않는다.

다음 요청은 유효한 Checkout Cookie를 요구한다. 따라서 다른 기기에서는 주문 화면을 볼 수 없을 뿐 아니라 주문 갱신이나 결제도 요청할 수 없다.

- `PENDING` 주문의 `GET /api/v1/orders/{orderId}`
- `PENDING` 주문의 `POST /api/v1/orders/{orderId}/pay`
- 동일 `cartItemIds`에 대한 기존 `PENDING` 주문 갱신

`PAID`, `CANCELED`, `EXPIRED` 주문의 주문 상세·주문 목록은 주문 소유자 인증만으로 조회한다.

## 4. 저장소 구조

### 4-1. Caffeine

Caffeine은 현재 실행 중인 단일 인스턴스의 활성 세션 저장소다.

```text
key: orderId
value: userId, tokenHash, expiresAt, heartbeatExpiresAt
```

- 세션 생성은 `putIfAbsent` 또는 원자적 `compute`로 처리한다.
- 같은 `orderId`에 다른 활성 토큰이 있으면 새 세션을 만들지 않고 거절한다.
- 장바구니 진입 시 Cookie만으로 세션을 찾을 수 있도록 `tokenHash -> orderId` 역색인을 함께 관리한다.
- 세션 갱신과 해제는 Caffeine에서 원자적으로 처리한다.
- `now >= expiresAt` 또는 `now >= heartbeatExpiresAt`이면 세션을 제거하고 만료로 처리한다.
- Cache TTL은 두 만료 시각 중 더 이른 시각보다 길지 않게 설정한다.

### 4-2. JPA 미사용

현재 범위에서는 `OrderSession`을 JPA 엔티티로 저장하지 않는다. 주문 세션은 주문의 업무 데이터가 아니라 현재 기기의 일시적인 점유 정보이기 때문이다.

- 애플리케이션 재시작 시 Caffeine이 비워지므로 모든 `OrderSession`은 초기화된다.
- `PENDING` Order와 주문의 만료 정보는 JPA에 계속 보존되며, 주문 세션 초기화와 무관하다.
- 재시작 전 Cookie를 가진 주문 화면은 유효한 서버 세션이 없으므로 장바구니로 이동하거나 새 주문 세션을 발급받아야 한다.

현재 구조는 다음과 같다.

```text
Order (JPA)                   ← 주문·PENDING 상태 영속화
OrderSessionStore (Caffeine)  ← 활성 주문 세션 정합성 판단
```

## 5. 처리 흐름

### 5-1. 최초 주문서 생성

1. 서버가 선택한 `cartItemIds`로 기존 `PENDING` Order를 찾는다.
2. 기존 주문에 유효한 `OrderSession`이 없으면 새 세션을 생성한다.
3. Caffeine에 세션을 원자적으로 등록한다.
4. `Set-Cookie`로 `checkoutToken`을 발급하고 주문 화면으로 이동시킨다.

### 5-2. 같은 기기의 재요청

같은 Cookie의 토큰이 유효하면 같은 기기의 요청으로 판단한다.

- 주문 수량·쿠폰 매핑·포인트·금액을 최신 요청으로 갱신할 수 있다.
- `orderId`는 유지한다.
- 6절의 Checkout API allowlist에 포함된 요청은 6절의 규칙에 따라 세션을 갱신한다.
- 주문 조회와 최종 결제를 허용한다.

### 5-3. 다른 기기의 요청

같은 사용자라도 Cookie 토큰이 다르면 다른 기기로 판단한다.

- 기존 활성 세션이 있으면 `409 ORDER_CHECKOUT_IN_USE`를 반환한다.
- 기존 `PENDING` 주문을 다른 기기의 요청으로 덮어쓰지 않는다.
- 다른 기기의 결제 요청도 동일하게 `409 ORDER_CHECKOUT_IN_USE`를 반환하고 결제를 시작하지 않는다.
- 사용자는 기존 기기에서 계속하거나 세션 만료 후 새 기기에서 다시 시도한다.

### 5-4. 세션 만료

- 세션 만료 시각의 의미와 기본값은 2절, API별 갱신 규칙은 6절을 따른다.
- `expiresAt` 또는 `heartbeatExpiresAt` 중 하나라도 지나면 세션을 제거하고 다른 기기가 새 세션을 획득할 수 있다.
- 세션이 만료되어도 Order는 즉시 만료되지 않는다. Order 자체는 생성·갱신 시점부터 24시간 동안 `PENDING`으로 유지한다.
- Order가 `PAID`, `CANCELED`, `EXPIRED`로 전이되면 Checkout Session을 Caffeine에서 제거한다.

## 6. Checkout API allowlist와 활동 갱신 정책

서버는 HTTP method와 path 기준으로 주문 화면에서 사용하는 API allowlist를 관리한다. allowlist API는 유효한 Checkout Cookie를 검증한 뒤 요청을 처리하고 세션 만료 시각을 연장한다.

| API 범위 | 세션 처리 |
|---|---|
| `POST /api/v1/orders` | 신규 주문서 생성 또는 기존 `PENDING` 주문 갱신. `expiresAt`, `heartbeatExpiresAt` 모두 연장 |
| `GET /api/v1/orders/{orderId}` | `PENDING` 주문 조회. `expiresAt`, `heartbeatExpiresAt` 모두 연장 |
| `POST /api/v1/orders/{orderId}/pay` | 최종 결제 요청. `expiresAt`, `heartbeatExpiresAt` 모두 연장한 뒤 결제 처리 |
| `POST /api/v1/orders/{orderId}/checkout-session/heartbeat` | 브라우저 생존 확인. `heartbeatExpiresAt`만 연장 |
| `GET /api/v1/payments/{paymentId}` | 주문 화면의 결제 상태 조회. `expiresAt`, `heartbeatExpiresAt` 모두 연장 |
| 주문 화면에서 사용하는 주소 조회·등록 API | `expiresAt`, `heartbeatExpiresAt` 모두 연장 |
| 주문 화면에서 사용하는 결제수단 조회·등록·삭제 API | `expiresAt`, `heartbeatExpiresAt` 모두 연장 |

```text
expiresAt = now + 30분
heartbeatExpiresAt = now + 3분
```

하트비트는 브라우저가 아직 살아 있음을 확인하기 위한 보조 요청이다. 하트비트 API는 유효한 Checkout Cookie를 검증한 뒤 `heartbeatExpiresAt`만 `now + 3분`으로 연장한다. 하트비트만 계속되고 주문 관련 사용자 활동이 없으면 `expiresAt`은 연장되지 않아 30분 후 세션이 만료된다.

```http
POST /api/v1/orders/{orderId}/checkout-session/heartbeat
Cookie: checkout_token=<opaque-token>
```

allowlist에 포함된 주소·결제수단 API도 주문 화면의 정상적인 활동으로 취급한다. allowlist에 포함되지 않은 API를 호출하면 [주문 화면 이탈](#7-주문-화면-이탈과-새로고침) 규칙에 따라 세션을 정리한다.

## 7. 주문 화면 이탈과 새로고침

주문 화면에서 새로고침·뒤로 가기 등으로 주문 화면 밖으로 이동하면, 주문 화면에서 사용하는 API를 제외한 다음 API 응답을 통해 Checkout Session을 정리한다. 별도의 `/enter` endpoint나 `DELETE` 요청을 만들지 않고, 현재 화면의 기존 API 요청에 Checkout Cookie를 포함한 뒤 서버 응답에서 해당 Cookie를 삭제한다.

```http
GET /api/v1/cart
Cookie: checkout_token=<opaque-token>
```

6절의 allowlist에 포함되지 않은 API의 응답에서는 서버가 요청 Cookie를 해시해 일치하는 `tokenHash`의 `OrderSession`만 Caffeine에서 제거한다. `userId` 기준으로 사용자의 모든 세션을 제거하지 않는다. Cookie가 없거나 일치하는 세션이 이미 없으면 서버 세션을 변경하지 않고 성공 응답을 반환한다.

allowlist 외 API 응답은 현재 브라우저의 Checkout Cookie를 삭제한다.

```http
Set-Cookie: checkout_token=; Max-Age=0; Path=/api/v1; HttpOnly; Secure; SameSite=Lax
```

Cookie를 삭제할 때는 발급할 때와 같은 이름·`Path`·`Domain`을 사용해야 한다. 따라서 Checkout Cookie의 삭제 응답도 반드시 `Path=/api/v1`을 사용한다.

`Set-Cookie`는 요청을 보낸 브라우저에만 적용된다. 따라서 A 디바이스가 주문 화면에 있는 동안 B 디바이스가 장바구니에 진입해도 A 디바이스의 Cookie나 `OrderSession`은 삭제되지 않는다. A 디바이스가 장바구니에 진입했을 때만 A의 `tokenHash`에 해당하는 세션이 제거된다.

브라우저 새로고침으로 주문 화면이 다시 로드되면 주문 화면을 복구하지 않고 장바구니 화면(`/cart`)으로 이동한다. 장바구니 화면의 기존 `GET /api/v1/cart` 응답이 서버 세션과 Cookie를 정리한 뒤 주문 버튼을 활성화한다. 메인·상품 등 다른 화면으로 이동하는 경우에도 해당 화면의 기존 API 응답이 같은 정리 규칙을 따른다. SPA 라우팅 자체는 서버 응답을 만들지 않으므로, 이동한 화면의 기존 데이터 조회 API가 반드시 호출되어야 한다.

브라우저의 강제 종료, 프로세스 종료, 전원 차단, 네트워크 단절처럼 allowlist 외 API 요청을 보내지 못하는 경우에는 서버의 `OrderSession.heartbeatExpiresAt`이 최종 안전장치가 된다. 다시 주문하려면 장바구니에서 주문 버튼을 눌러 주문서 생성과 새 Checkout Session 발급을 다시 수행한다.

## 8. 상태와 예외

| 상황 | 처리 |
|---|---|
| 유효한 세션 없음 | 새 세션 발급 |
| 같은 토큰 | 조회·갱신·결제 허용 |
| 다른 토큰의 활성 세션 존재 | `409 ORDER_CHECKOUT_IN_USE` |
| Cookie 없음으로 PENDING 주문 조회 | `401 ORDER_CHECKOUT_SESSION_REQUIRED` |
| 세션 만료 토큰 사용 | `409 ORDER_CHECKOUT_SESSION_EXPIRED` |
| Order가 이미 PAID·CANCELED·EXPIRED | Order 상태 규칙에 따라 처리하고 Checkout Session 검증은 생략 |
