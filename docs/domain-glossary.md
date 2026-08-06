# 도메인 용어집

이 문서는 요구사항과 코드에서 같은 용어를 같은 의미로 사용하기 위한 기준이다. 상태값의 정확한 영문 상수는 구현 전 `docs/requirement.md`, 현재 코드, Flyway 제약조건의 충돌 여부를 확인한다.

## 아키텍처

| 용어 | 의미 |
|---|---|
| 모듈러 모놀리스 | 하나의 배포 단위와 DB를 사용하지만 도메인별 모듈의 공개 인터페이스와 의존성을 제한하는 구조 |
| 모듈 | 공개 인터페이스 뒤에 관련 비즈니스 규칙과 구현을 모은 단위. 현재 최상위 Java 패키지가 기본 모듈 seam이다. |
| 인터페이스 | 호출자가 모듈을 올바르게 사용하기 위해 알아야 하는 입력, 결과, 불변조건, 오류, 순서 제약 전체 |
| Named Interface | Spring Modulith의 `@NamedInterface`로 다른 모듈에 공개한 제한된 seam |
| 이벤트 | 이미 발생한 도메인 사실을 표현하는 불변 메시지. 소비자가 없어도 발행자의 사실은 성립해야 한다. |
| Outbox | 비즈니스 변경과 발행할 이벤트를 같은 DB 트랜잭션에 기록하여 이벤트 유실을 막는 패턴 |
| Saga | 여러 모듈의 로컬 트랜잭션을 이벤트로 연결하고 실패 시 보상 작업으로 일관성을 회복하는 흐름 |
| 보상 트랜잭션 | 완료된 이전 작업을 물리적으로 되돌리는 대신 재고 복원, 환불 같은 반대 효과를 새 트랜잭션으로 수행하는 작업 |
| 멱등성 | 같은 요청이나 이벤트를 여러 번 처리해도 최종 결과가 한 번 처리한 것과 같은 성질 |
| Adapter | DB, PG, OAuth 공급자, 캐시처럼 모듈 밖 기술을 도메인 인터페이스에 연결하는 구현 |

## 사용자와 인증

| 용어 | 의미 |
|---|---|
| User | 이름, 연락처, 역할, 활성 상태 등 고객 프로필의 주체. 비밀번호나 OAuth 공급자 세부정보를 소유하지 않는다. |
| UserCredential | 이메일과 비밀번호 해시로 구성된 로컬 로그인 수단. 현재 설계에서는 User에게 선택적으로 하나가 연결된다. |
| SocialCredential | OAuth 공급자와 공급자 사용자 ID로 식별하는 소셜 로그인 수단. 한 User가 여러 공급자 수단을 가질 수 있다. |
| 인증수단 | UserCredential 또는 SocialCredential처럼 사용자의 신원을 증명하는 방법 |
| Guest Token | 신규 소셜 사용자에게 잠시 발급하는 JWT. 공급자 인증 정보를 회원가입과 연결하며 목표 만료시간은 10분이다. |
| Access Token | API 요청 인증에 사용하는 단기 JWT. 목표 만료시간은 30분이며 HttpOnly 쿠키로 전달한다. |
| Refresh Token | 새 Access Token 발급에 사용하는 장기 JWT. 목표 만료시간은 7일이다. |
| Blacklist | 로그아웃, 자격 증명 변경, 계정 비활성화 후 기존 JWT를 즉시 거부하기 위한 토큰·사용자 무효화 기록 |
| Role | 접근 권한. `USER`, `PRODUCT_MANAGER`, `ADMIN`을 사용한다. |
| 계정 비활성화 | 주문 이력 보존을 위해 User를 삭제하지 않고 로그인과 사용만 막는 논리적 삭제 상태 |
| SignUpTask | 현재 auth가 user에 프로필 생성을 요청할 때 발행하는 동기 메시지. 이름과 달리 완료 이벤트가 아니라 명령 성격이 있다. |

## 고객 자산

| 용어 | 의미 |
|---|---|
| Address | 사용자가 관리하는 배송지. 주문은 이후 변경의 영향을 받지 않도록 주문 시점 주소 스냅샷을 보관한다. |
| 기본 배송지 | 사용자당 하나만 존재하는 대표 Address. 변경은 같은 트랜잭션에서 기존 기본값을 해제해야 한다. |
| Point | 결제에 사용할 수 있는 사용자 잔액. 음수가 될 수 없고 동일 사용자 차감은 동시성 제어가 필요하다. |
| Point History | 포인트 증감의 불변 원장. 수정·삭제하지 않고 취소도 반대 부호 레코드를 추가한다. |
| Wishlist | User와 Product 사이의 관심 관계. 상품이 품절·삭제되어도 이력은 유지하고 화면에서 상태를 구분한다. |

## 상품과 전시

| 용어 | 의미 |
|---|---|
| Category | 최대 3단계까지 부모를 가질 수 있는 상품 분류 |
| Product | 판매 대상의 이름, 설명, 정상가, 브랜드, 카테고리와 판매 상태를 소유하는 도메인 객체 |
| Inventory | 판매 가능한 Product 수량과 동시 차감·복원 규칙 |
| Time Sale | 지정 기간에 정상가 대신 할인가를 적용하는 정책 |
| 적용가격 | 현재 시간이 타임세일 범위 안이면 할인가, 아니면 정상가 |
| Image | `entity_type`과 `entity_id`로 Product, Review 등에 연결하는 공통 이미지 메타데이터 |
| Review | 구매와 배송 완료가 확인된 사용자가 상품당 하나 작성할 수 있는 평점·텍스트·이미지 평가 |

## 구매와 혜택

| 용어 | 의미 |
|---|---|
| Cart | 사용자당 하나인 활성 장바구니 |
| Cart Item | Cart에 담긴 Product와 수량. 동일 상품은 중복 행 대신 수량을 합친다. |
| Coupon | 할인 타입, 값, 기간, 최소 주문액, 발급 한도를 정의하는 쿠폰 원본 |
| User Coupon | 특정 User에게 발급된 Coupon 인스턴스. `AVAILABLE`, `USED`, `EXPIRED` 생명주기를 가진다. |
| 정률 할인 | 상품 금액의 일정 비율을 할인하되 최대 할인 한도를 적용하는 방식 |
| 정액 할인 | 고정 금액을 할인하는 방식 |

## 주문, 결제, 배송

| 용어 | 의미 |
|---|---|
| Order | 결제 대상 품목, 가격·할인·포인트 계산 결과, 주소 스냅샷과 상태를 보존하는 구매 기록 |
| Order Item | 주문 시점 Product 이름, 단가, 수량을 보존하는 항목. 이후 상품 변경과 분리된다. |
| 상품 총액 | 각 Order Item의 적용가격과 수량을 곱한 값의 합 |
| 최종 결제 금액 | 상품 총액에서 쿠폰 할인액과 포인트 사용액을 차감한 값. 0보다 작을 수 없다. |
| Payment Method | 카드, 간편결제, 계좌이체 같은 결제 전략의 종류 |
| Payment | PG 승인·실패·환불 결과와 거래번호, 금액, 수단을 기록한 결제 트랜잭션 |
| Delivery | Order와 1:1로 연결된 배송 추적 정보와 상태 |
| 주소 스냅샷 | 주문 생성 시 Address 값을 복사한 불변 배송 정보 |

## 목표 상태 전이

- Order: `PENDING → PAID 또는 CANCELED`, `PAID → CANCELED`.
- Delivery: `PREPARING → SHIPPED → IN_TRANSIT → DELIVERED`.
- User Coupon: `AVAILABLE → USED` 또는 `AVAILABLE → EXPIRED`; 주문 취소 시 기간이 남아 있으면 `USED → AVAILABLE`.
- Payment: 승인 성공, 실패, 환불을 구분하며 환불은 성공 결제에만 적용한다.

## 현재 확인이 필요한 용어 충돌

요구사항과 `V1__init_schema.sql`의 상수가 일치하지 않는다. 관련 도메인을 구현하기 전에 하나를 선택해 요구사항, 코드, DB 제약조건을 함께 정렬하고 ADR이 필요한지 판단한다.

| 대상 | 요구사항 | 현재 V1 스키마 |
|---|---|---|
| Product 상태 | `ACTIVE`, `INACTIVE`, `SOLD_OUT` | `ON_SALE`, `SOLD_OUT`, `RESTOCK_SCHEDULED` |
| Delivery 상태 | `PREPARING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED` | `PREPARING`, `SHIPPED`, `DELIVERED` |
| Payment Method | `CREDIT_CARD`, `KAKAO_PAY`, `BANK_TRANSFER` | `CARD`, `KAKAOPAY`, `NAVERPAY`, `TRANSFER` |
| Payment 상태 | `SUCCESS`, `FAILED`, `REFUNDED` | `COMPLETED`, `FAILED`, `REFUNDED` |
| 로컬 Credential 관계 | 요구사항은 User와 1:N | 현재 설계 메모와 구현은 User당 0..1 |
