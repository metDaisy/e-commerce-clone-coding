# P3 Cart Policy

이 문서는 장바구니 API와 독립적으로 유지되는 P3의 업무 정책을 정의한다. 데이터 모델과 API 계약은 [Cart API](p3-cart.md)를 따른다.

## 1. 범위와 책임

### 범위

- 회원·비회원 구매자의 임시 장바구니
- Cart Item의 추가·수량 변경·개별 삭제·전체 비우기
- Cart와 Cart Item의 소유권·수량·수명 주기
- 로그인 성공 시 게스트 Cart와 회원 Cart의 병합
- 결제 성공 후 주문에 포함된 Cart Item 정리

P3는 주문·결제·배송의 상태나 최종 주문 금액을 소유하지 않는다.

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| Cart·Cart Item 원본·상태·소유권 | P3 Cart | [Cart API](p3-cart.md) |
| 인증·로그인 성공 진입점 | P11 Auth | [P11 Index](../p11/p11-index.md), [Session API](../p11/p11-session.md) |
| 상품·Variant 표시 정보 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| Offer 상태·가격·구매 제한·재고 | P9 Offer & Marketplace | [P9 Offer](../p9/p9-offer.md) |
| 주문 생성·결제 완료·최종 재검증 | P5 Order | [P5 Policy](../p5/p5-policy.md) |
| 이벤트 재전달·정리 작업 운영 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `Cart` | 회원 또는 비회원 구매자가 주문 전에 Offer를 임시 보관하는 장바구니. 회원은 사용자당 하나만 가진다. |
| `Cart Item` | Cart에 담긴 하나의 Offer와 수량. 같은 Offer는 여러 행으로 중복 보관하지 않는다. |
| `Cart Owner` | Cart를 변경할 수 있는 주체. 회원 Cart는 User, 비회원 Cart는 `guest_cart_id` 쿠키를 가진 익명 브라우저 주체다. |
| `Guest Cart` | 로그인하지 않은 구매자의 임시 Cart. 주문을 소유하지 않으며 로그인 성공 시 회원 Cart와 병합된다. |
| `Offer Purchase Limit` | P9 Offer가 구매자 한 명에게 허용하는 최대 수량. 현재 재고와는 별개의 제한이다. |
| `선택 상품 주문` | Cart Item 일부만 선택해 하나의 주문으로 전환하는 방식. 분할 결제와는 다르다. |

용어의 기준은 [domain-glossary.md](../../domain-glossary.md)의 Cart 항목을 따른다.

## 3. 핵심 업무 규칙

- 회원 Cart는 사용자당 하나이며 논리 식별자를 인증된 `userId`로 사용한다.
- 비회원이 처음 상품을 추가하면 서버가 UUID Cart와 `guest_cart_id` 쿠키를 만든다. 쿠키에는 개인정보를 저장하지 않는다.
- 같은 Offer를 다시 추가하면 기존 수량에 합산한다. `quantity = 0` 변경은 해당 Cart Item 삭제다.
- 저장되는 수량은 1 이상이고 P9가 공개한 `maxPurchaseQuantity` 이하여야 한다. 장바구니는 Offer별 제한과 별도로 최대 50종, 전체 1,000개 제한을 적용한다.
- 모든 Cart 조회·변경·정리 요청은 Cart Owner를 확인한다. 클라이언트가 전달한 Cart ID만으로 소유권을 인정하지 않는다.
- 비회원은 상품 추가와 전체 비우기만 할 수 있다. 장바구니 화면 조회·수량 변경·개별 삭제·주문 생성은 로그인 사용자만 사용할 수 있다.
- Cart 조회는 현재 Offer 가격·판매 상태·재고를 표시하기 위한 1차 검증이다. P5 주문 생성과 결제 직전에도 같은 핵심 조건을 다시 검증한다.
- Cart는 가격·재고·상품 상태를 원본 값으로 저장하지 않는다. 가격과 표시 상태는 조회 시 P2·P9 공개 계약으로 계산한다.
- 일부 Cart Item만 선택해 주문할 수 있다. 결제 성공 시 주문에 포함된 항목만 삭제하고, 결제 실패 항목은 유지한다.

### 비회원 Cart 쿠키와 만료

- `guest_cart_id`는 `HttpOnly`, `SameSite=Lax`, `Path=/`를 사용하고 운영 환경에서는 `Secure`를 적용한다.
- 쿠키 `Max-Age`와 서버 `expiresAt`은 30일이다. 실제 상품 추가나 로그인 병합이 성공하면 만료 시각을 30일 연장한다. 단순 조회는 연장하지 않는다.
- 쿠키는 bearer 성격의 식별자이므로 URL·요청 본문·로그에 기록하지 않는다.
- 쿠키 만료만으로 서버 데이터가 삭제되지는 않는다. 정리 작업은 `expiresAt < 현재 시각`인 게스트 Cart를 배치로 멱등 삭제한다.
- 요청 처리 중 만료를 확인한 Cart는 즉시 폐기할 수 있으며, 과거 Cart의 존재나 상품 정보를 클라이언트에 노출하지 않는다.

### 로그인 병합

- P11 Auth가 로그인 성공 시 P3 병합 계약을 호출한다.
- 회원 Cart가 없으면 먼저 준비하고, 같은 Offer는 수량을 합산한다.
- 병합 결과가 Offer별·종류·전체 수량 제한을 넘거나 병합 대상 Offer가 유효하지 않으면 병합은 원자적으로 실패한다. 회원 Cart는 변경하지 않고 게스트 Cart와 쿠키를 유지하며 문제 목록을 반환한다.
- 병합 성공 시 게스트 Cart와 항목을 삭제하고 쿠키를 폐기한다.

## 4. 불변식과 상태 전이

### 불변식

- 회원당 활성 Cart는 하나다. 회원 Cart의 `expiresAt`은 `NULL`이다.
- 게스트 Cart는 `guest_cart_id`로만 접근할 수 있고 `userId`·`deviceId`·개인정보를 저장하지 않는다.
- `(cartId, offerId)`는 유일하며 Cart Item 수량은 항상 1 이상이다.
- Cart의 Offer 종류 수는 50 이하, 전체 수량은 1,000 이하이며 모든 합산·병합에도 적용한다.
- P3는 P9 Offer의 현재 가격·재고·판매 상태를 저장하지 않고, P5는 P3 조회 결과를 최종 검증으로 신뢰하지 않는다.
- 주문·결제 상태는 P5가 소유한다. P3는 결제 성공 후 전달된 Cart Item 정리만 수행한다.
- 게스트 Cart 만료 정리와 결제 완료 정리는 반복 실행해도 같은 결과가 되어야 한다.

### 상태 전이

Cart의 수명 상태는 업무상 파생 상태이며 `status` 컬럼으로 저장하지 않는다.

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| `ACTIVE` 게스트 Cart | `expiresAt` 도달 또는 만료 확인 | `EXPIRED` 후 삭제 대상 | P3 정리 작업·요청 처리 |
| `ACTIVE` 게스트 Cart | 상품 추가·로그인 병합 성공 | `ACTIVE` 및 `expiresAt` 연장 | P3 |
| `ACTIVE` Cart | 전체 비우기 | `EMPTY` 또는 게스트 Cart 삭제 | Cart API |
| `ACTIVE` Cart | 결제 성공 이벤트에서 선택 항목 전달 | 선택 Cart Item 삭제 | P3 |
| `ACTIVE` Cart | 로그인 병합 성공 | 회원 Cart로 병합, 게스트 Cart 삭제 | P11 Auth·P3 |

`EMPTY`는 저장된 상태가 아니라 Cart Item이 없는 결과를 뜻한다. 회원 Cart는 비워져도 유지된다.

## 5. 도메인 간 규칙과 예외 소유권

- P2 상품 표시 정보와 P9 Offer·재고 정보는 각 도메인의 공개 조회 계약으로만 사용한다.
- P9가 반환한 Offer 없음·보관·판매 중지 예외의 원본은 [P9 Offer](../p9/p9-offer.md)가 소유한다. P3 API는 필요한 경우 [OFFER-001](../p9/p9-offer.md) 원본을 참조하거나 P3 소유의 `CART-006`으로 변환하며, 변환 이유는 Cart API에 명시한다.
- P11 Auth는 인증과 로그인 성공 흐름을 소유하고 P3는 병합 규칙과 Cart 변경을 소유한다.
- P5는 선택 Cart Item을 주문으로 전환하고 결제 직전 조건을 재검증한다. P3는 주문 생성이나 결제 완료 여부를 판단하지 않는다.
- P6는 이벤트 저장·재전달·정리 작업의 운영 정책을 소유한다. P3의 Cart Item 정리 계약은 중복 이벤트에 멱등적이어야 한다.

| 외부 도메인 | 사용 목적 | 공개 계약·정책 참조 |
|---|---|---|
| P2 Catalog | 상품명·썸네일·Variant 표시 정보 | [P2 Catalog](../p2/p2-catalog.md) |
| P9 Offer & Marketplace | Offer 존재·판매 상태·현재 가격·구매 제한·재고 확인 | [P9 Offer](../p9/p9-offer.md), [P9 Exceptions](../p9/p9-exceptions.md) |
| P11 Auth | 로그인 성공 후 게스트 Cart 병합 호출 | [Session API](../p11/p11-session.md) |
| P5 Order | 선택 Cart Item 전달·결제 성공 후 정리 요청 | [P5 Policy](../p5/p5-policy.md), [Payment Process](../p5/p5-payment-process.md) |

## 6. API 문서와의 관계

- 요청·성공 응답·HTTP 상태·예외 매트릭스는 [Cart API](p3-cart.md)에서 정의한다.
- 이 정책과 API 문서가 충돌하면 이 정책을 기준으로 Cart API를 수정한다.
- 공통 오류 응답 필드는 [공통 API 계약](../index.md#공통-api-계약)을 따르고, P3 특화 코드는 Cart API에 등록한다.
