# P3 Cart 문서 안내

P3는 회원과 비회원 구매자가 선택한 Offer를 주문 전까지 임시 보관하는 장바구니를 정의한다. P3는 Cart와 Cart Item의 원본·소유권·수량 규칙을 소유하고, 주문·결제 상태는 소유하지 않는다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P3 Policy](p3-policy.md) | 정책 | 범위·책임, 행위자, 장바구니 규칙, 불변식, 수명 주기, 도메인 간 규칙 |
| [Cart API](p3-cart.md) | 데이터 모델·API | `carts`·`cart_items` 모델, 관계·제약, 장바구니 조회·변경 API와 예외 |

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| Cart·Cart Item의 원본, 소유권, 수량·수명 주기 | P3 Cart | [P3 Policy](p3-policy.md), [Cart API](p3-cart.md) |
| 사용자 인증과 로그인 성공 처리 | P11 Auth | [P11 Index](../p11/p11-index.md), [Session API](../p11/p11-session.md) |
| 상품 표시 정보·Variant 확인 | P2 Catalog | [P2 Catalog](../p2/p2-catalog.md) |
| Offer의 판매 상태·현재 가격·구매 제한·재고 확인 | P9 Offer & Marketplace | [P9 Offer](../p9/p9-offer.md), [P9 Exceptions](../p9/p9-exceptions.md) |
| 선택 Cart Item의 주문 전환과 결제 완료 후 정리 요청 | P5 Order | [P5 Policy](../p5/p5-policy.md), [Order Checkout](../p5/p5-order-checkout.md) |
| 이벤트 저장·재전달·운영 처리 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |

- P3는 `carts`와 `cart_items`의 원본 데이터와 장바구니 업무 규칙을 소유한다.
- P3는 P2·P9의 내부 모델이나 Repository를 소유하거나 직접 참조하지 않고 공개 조회 계약만 사용한다.
- P5는 주문 생성·결제 직전에 장바구니의 핵심 조건을 다시 검증한다. P3의 화면 조회 결과나 클라이언트 금액을 최종 검증 결과로 사용하지 않는다.
- 로그인 성공 시 비회원 장바구니 병합을 호출하는 진입점은 P11 Auth가 소유하고, 병합 규칙과 Cart 변경은 P3가 소유한다.
- 공통 URI, 성공 응답, 예외 응답, 인증, 트랜잭션 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 3. 처리 흐름

```text
상품 선택
  → Cart Item 추가·수량 변경
  → 장바구니 조회 및 구매할 Cart Item 선택
  → P5 주문 생성에 cartItemIds 전달
  → 결제 성공 후 P5 요청으로 선택 Cart Item 정리
```

비회원은 브라우저의 `guest_cart_id` 쿠키로 장바구니를 식별한다. 로그인하면 P11 Auth가 P3의 병합 계약을 호출하며, 병합이 완료된 뒤 기존 게스트 장바구니와 쿠키를 폐기한다.

## 4. 작성 원칙

- 이 문서는 문서 목록과 책임 경계만 정의하고, 정책·필드·API 계약을 중복해서 작성하지 않는다.
- 업무 규칙은 [P3 Policy](p3-policy.md)에, 필드·요청·응답·예외는 [Cart API](p3-cart.md)에 작성한다.
- Offer·재고·주문·인증의 원본 예외 코드는 해당 도메인 문서를 참조하고 P3에서 재정의하지 않는다.
