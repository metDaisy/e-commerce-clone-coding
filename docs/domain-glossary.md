# 도메인 용어집

이 문서는 요구사항과 코드에서 같은 용어를 같은 의미로 사용하기 위한 기준이다. 상태값의 정확한 영문 상수는 구현 전 `docs/requirement/index.md`, 현재 코드, Flyway 제약조건의 충돌 여부를 확인한다.

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
| User | 이름, 연락처, 역할, 활성 상태 등 고객 프로필의 주체. 이메일·비밀번호·OAuth 공급자 세부정보는 소유하지 않으며 각각 인증수단이 소유한다. |
| UserCredential | 이메일과 비밀번호 해시로 구성된 로컬 로그인 수단. 현재 설계에서는 User에게 선택적으로 하나가 연결된다. |
| SocialCredential | OAuth 공급자와 공급자 사용자 ID로 식별하는 소셜 로그인 수단. 신규 소셜 사용자는 OAuth 인증 후 회원가입 화면에서 추가 정보를 입력하고 가입을 완료하며, 이때 생성된다. `UserCredential`은 생성하지 않는다. 한 User가 여러 공급자 수단을 가질 수 있다. |
| provider | 이 프로젝트가 정의한 OAuth 공급자 식별 enum. 요구사항 값은 `GOOGLE`, `NAVER`, `KAKAO`, `GITHUB`이며 Amazon의 표준 회원 필드가 아니다. |
| providerId | OAuth 공급자가 반환하는 외부 사용자 식별자. 이메일이 아닌 `provider`별 불투명 식별자를 저장한다. |
| Login with Amazon | 제3자 웹사이트·앱이 Amazon 계정으로 인증받는 OAuth 2.0 서비스. Amazon.com 자체의 일반 회원가입 방식과 구분한다. |
| 인증수단 | UserCredential 또는 SocialCredential처럼 사용자의 신원을 증명하는 방법 |
| Guest Token | 신규 소셜 사용자에게 잠시 발급하는 JWT. 공급자 인증 정보를 회원가입과 연결하며 목표 만료시간은 10분이다. |
| Access Token | API 요청 인증에 사용하는 단기 JWT. 목표 만료시간은 30분이며 HttpOnly 쿠키로 전달한다. |
| Refresh Token | 새 Access Token 발급에 사용하는 장기 JWT. 목표 만료시간은 7일이다. |
| Blacklist | 로그아웃, 자격 증명 변경, 계정 비활성화 후 기존 JWT를 즉시 거부하기 위한 토큰·사용자 무효화 기록 |
| Role | 접근 권한. `USER`는 기본 구매자, `PRODUCT_MANAGER`는 `ACTIVE` Seller를 가진 User에게 부여되는 판매자 역할, `ADMIN`은 플랫폼 운영자다. `PRODUCT_MANAGER`도 구매자 API를 사용할 수 있다. |
| Seller | 기존 User에 연결된 판매자 신청·승인 프로필. `PENDING`, `ACTIVE`, `REJECTED`, `SUSPENDED` 상태를 가지며 `PRODUCT_MANAGER` 역할과 `ACTIVE` 상태가 모두 충족될 때 판매자 API를 사용할 수 있다. |
| 계정 비활성화 | 주문 이력 보존을 위해 User를 삭제하지 않고 로그인과 사용만 막는 논리적 삭제 상태 |
| SignUpTask | 현재 auth가 user에 프로필 생성을 요청할 때 발행하는 동기 메시지. 이름과 달리 완료 이벤트가 아니라 명령 성격이 있다. |

## 고객 자산

| 용어 | 의미 |
|---|---|
| Address | 사용자가 관리하는 배송지. 주문은 이후 변경의 영향을 받지 않도록 주문 시점 주소 스냅샷을 보관한다. |
| 기본 배송지 | 사용자당 하나만 존재하는 대표 Address. 변경은 같은 트랜잭션에서 기존 기본값을 해제해야 한다. |
| Point | 결제에 사용할 수 있는 사용자 잔액. 음수가 될 수 없고 동일 사용자 차감은 동시성 제어가 필요하다. |
| Point History | 포인트 증감의 불변 원장. 수정·삭제하지 않고 취소도 반대 부호 레코드를 추가한다. |
| Wishlist | User와 ProductVariant 사이의 관심 관계. Variant가 품절·보관되어도 관계는 유지하고 화면에서 상태를 구분한다. |

## 상품과 전시

문서의 도메인 용어와 기준 SQL 테이블은 다음처럼 대응한다. Java entity 이름은 이 매핑을
기준으로 이후 구현한다.

| Domain concept | Database table |
|---|---|
| CatalogProduct | `catalog_products` |
| ProductVariant | `product_variants` |
| Offer | `offers` |
| Inventory | `inventories` |

다음 개념은 별도 테이블이 없는 것이 정상이다.

- `Point`는 `users.point_balance`에 저장되는 사용자 잔액이다.
- `Access Token`과 `Guest Token`은 JWT로 발급하며 DB에 저장하지 않는다. `Refresh Token`은 `refresh_tokens`에 저장한다.
- `SignUpTask`는 사용자 프로필 생성 요청 메시지이며 영속 엔티티가 아니다.
- `구매 가능 상태`는 `offers.status`와 `inventories.quantity`로 계산한다.

| 용어 | 의미 |
|---|---|
| Category | 최대 3단계까지 부모를 가질 수 있는 상품 분류 |
| CatalogProduct | 상품명·설명·브랜드·카테고리·상품 속성 등 상품군의 공통 메타데이터와 전시 정보를 소유하는 도메인 객체. `ADMIN`만 생성·수정·보관하며 관리자 소유자나 `managerId`를 저장하지 않는다. 실제 가격과 재고의 소유자가 아니다. |
| ProductVariant | CatalogProduct의 메타데이터를 바탕으로 구성된 판매 대상이다. 고객이 실제로 선택·주문하고 판매자가 판매하는 하나의 옵션 조합이자 SKU 단위다. `ADMIN`만 생성·수정·보관하며 하나의 ProductVariant는 정확히 하나의 CatalogProduct에만 속한다. |
| SKU(Stock Keeping Unit) | ProductVariant를 식별하는 판매 단위 코드. 요구사항에서는 시스템 전체에서 UNIQUE다. |
| ASIN / GTIN / UPC / EAN / ISBN | 전 세계적으로 동일한 상품을 식별하기 위한 표준 바코드 및 식별자(아마존 식별자, 국제/북미/유럽 표준, 국제 도서 번호 등). `CatalogProduct` 간 중복될 수 없는 고유 값이다. |
| Offer | 승인된 Seller가 특정 ProductVariant를 어떤 가격·판매 상태·판매자 조건으로 판매하는지 나타내는 판매 제안. Seller별로 같은 ProductVariant에 하나만 가질 수 있으며, 생성 시 Inventory가 함께 만들어진다. |
| Inventory | 특정 Offer의 구매 가능 수량과 차감·복원 규칙을 소유하는 재고 정보. Offer에 종속되고 Offer 생성 시 함께 생성되며, 재고는 CatalogProduct 전체가 아니라 실제 판매 조건 단위로 관리한다. |
| 구매 가능 상태 | Inventory 수량을 바탕으로 계산한 `IN_STOCK`, `OUT_OF_STOCK` 등의 표시 상태. `SOLD_OUT`을 상품의 영구 상태로 저장하지 않는다. |
| 기간성 할인 | Offer에 연결된 기본 가격과 시작·종료 시각이 있는 할인 가격. 심화사항에서는 쿠폰·회원가·복수 프로모션으로 확장한다. |
| 적용가격 | 현재 시각과 적용 가능한 가격 정책을 기준으로 계산한 고객 표시 가격 |
| Image/Media | 상품·ProductVariant·Review에 연결되는 이미지와 미디어 메타데이터. CatalogProduct·ProductVariant Media의 등록·수정·보관은 `ADMIN`만 수행하고 Review Media는 Review가 소유하며, 표시 순서와 URL을 관리한다. 실제 파일 저장은 infra adapter가 담당한다. |
| Review | 구매와 배송 완료가 확인된 사용자가 ProductVariant당 하나 작성할 수 있는 평점·텍스트·이미지 평가 |

`CatalogProduct`와 `ProductVariant`는 `1 : N` 관계다. 하나의 CatalogProduct가 여러 ProductVariant를 가지며, 각 ProductVariant는 하나의 CatalogProduct에만 속한다. 따라서 `(catalogProductId, variantId)` 조합은 유일하고, `variantId`가 전역 PK이므로 별도의 복합 식별자를 만들지 않는다. 판매자는 ProductVariant에 `Offer`를 등록해 가격·판매 상태·판매 조건을 관리하고, 재고는 Offer 단위로 관리한다.

예를 들어 `GIGABYTE AMD R9700`을 등록하면 다음과 같이 표현한다.

```text
CatalogProduct: GIGABYTE AMD R9700
├─ ProductVariant: White / SKU-GIGA-R9700-W
│  ├─ Offer: 판매자 A / 500,000원 / Inventory 10개
│  └─ Offer: 판매자 B / 510,000원 / Inventory 4개
└─ ProductVariant: Black / SKU-GIGA-R9700-B
   └─ Offer: 판매자 A / 505,000원 / Inventory 7개
```

이 예시에서 상품명·브랜드는 CatalogProduct의 공통 메타데이터이고, 색상과 SKU는 ProductVariant가 표현한다. 판매자·가격·판매 상태·재고는 각각 Offer와 Inventory가 표현한다.

## 구매와 혜택

| 용어 | 의미 |
|---|---|
| Cart | 사용자당 하나인 활성 장바구니 |
| Cart Item | Cart에 담긴 Offer와 수량. 동일 Offer는 중복 행 대신 수량을 합친다. |
| Coupon | 할인 타입, 값, 통화, 기간, 최소 주문액, 발급 한도를 정의하는 쿠폰 원본. 저장 모델의 `discountValue`는 `PERCENTAGE`에서는 할인율, `FIXED_AMOUNT`에서는 할인 금액이며 API는 각각 `discountRate`, `discountAmount`로 구분한다. 카테고리·상품 등 적용 대상 제한은 심화사항이다. |
| User Coupon | 특정 User에게 발급된 Coupon 인스턴스. `AVAILABLE`, `USED`, `EXPIRED` 생명주기를 가진다. |
| 정률 할인 | 상품 금액의 일정 비율을 할인하되 최대 할인 한도를 적용하는 방식 |
| 정액 할인 | 고정 금액을 할인하는 방식 |

## 주문, 결제, 배송

| 용어 | 의미 |
|---|---|
| Order | 결제 대상 품목, 가격·할인·포인트 계산 결과, 주소 스냅샷과 상태를 보존하는 구매 기록 |
| Order Item | 주문 시점의 CatalogProduct·ProductVariant·Offer를 식별하고 상품명·SKU·단가·수량을 스냅샷으로 보존하는 항목. 이후 상품 변경과 분리된다. |
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

## 상태 및 관계 기준

- `CatalogProduct.publicationStatus`는 `ACTIVE`, `ARCHIVED`를 사용한다.
- `ProductVariant.publicationStatus`는 `ACTIVE`, `ARCHIVED`를 사용한다.
- `Offer.status`는 `ACTIVE`, `INACTIVE`, `ARCHIVED`를 사용한다. `ARCHIVED`는 논리 삭제이며 다시 활성화할 수 없다.
- CatalogProduct 또는 ProductVariant가 `ARCHIVED`가 되면 하위 Offer는 `INACTIVE`가 되고 공개 검색·구매 대상에서 제외된다.
- `Inventory`의 구매 가능 상태는 수량을 기준으로 `IN_STOCK` 또는 `OUT_OF_STOCK`으로 계산한다.
- `Delivery.status`는 `PREPARING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`를 사용한다.
- `PaymentMethod.methodType`는 `CREDIT_CARD`, `KAKAO_PAY`, `BANK_TRANSFER`를 사용한다.
- `Payment.status`는 `SUCCESS`, `FAILED`, `REFUNDED`를 사용한다.
- `UserCredential`은 User에 선택적으로 연결되며, 소셜 회원가입만으로는 생성하지 않는다.
- `User.isEnabled`는 `UserDetails.isEnabled()`와 같은 의미로, 인증을 허용할지 나타낸다. User 테이블에는 `is_enabled`만 저장하며 `is_active`는 사용하지 않는다.
- `UserCredential.isLocked`는 저장 컬럼이 아니다. `violationCount`와 `untilLocked`를 기준으로 계산되는 로그인 잠금 파생 상태다.
