# 현재 구현 상태

> 커밋된 코드·테스트·설정·Flyway를 기준으로 정리한 파생 스냅샷이다. 요구사항 문서는 목표 범위를 확인하는 용도로만 사용한다.

## 스냅샷

- 확인일: 2026-08-23
- Git 브랜치: `p2/issue18`
- 확인 기준 Git SHA: `2f4d0b1c660d02a6df5d5a6cd589b3a0e762dd82`
- 기준 상태: 위 SHA의 커밋된 코드·테스트·설정·Flyway·프런트엔드를 확인했다. 이 문서를 갱신하는 후속 문서 커밋은 기준 SHA에 포함하지 않는다.
- 이전 스냅샷 기준 SHA `82cb76b12df590777893e0ea644dbc73fa70d19f`는 현재 확인 기준 SHA의 조상이다.
- 검증 실행: 이번 갱신에서는 Gradle `check`·테스트·JaCoCo와 프런트엔드 `lint`·`build`를 실행하지 않았다. 테스트 통과나 커버리지는 주장하지 않는다. Codebase Memory 인덱스는 확인 기준 SHA와 일치하는 `ready` 상태였다.

## 전체 진행 요약

| 단계 | 상태 | 확인된 범위 |
|---|---|---|
| P1 User & Address | 부분 구현 | User 프로필 조회·수정·비활성화, 회원가입 연계, Address 목록·등록·수정·삭제·기본 배송지 지정, 다중 역할, 활성 User 검사와 주소 정렬·잠금이 구현되어 있다. 요구사항의 재인증(`__Host-REAUTH`)과 주문 연계는 확인되지 않았다. |
| P2 Catalog | 부분 구현 | 공개 Category 트리 조회와 ADMIN 전용 Category 생성·수정, CatalogProduct 관리자 생성·수정·식별자 검증·갱신·archive, ProductVariant 생성·조회·수정·archive와 공개 application API가 구현되어 있다. CatalogProduct 공개 조회·상품 Media는 확인되지 않았다. 식별자는 요청에서 객체형 `identifiers` 맵으로 받고 DB에는 개별 컬럼으로 저장한다. |
| P3 Cart | 스키마만 존재 | V1의 `carts`·`cart_items` 테이블은 있으나 Cart 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P4 Coupon | 스키마만 존재 | V1의 쿠폰 테이블은 있으나 Coupon 도메인·API는 확인되지 않았다. |
| P5 Order, Payment, Delivery | 스키마만 존재 | V1의 주문·결제·배송 테이블은 있으나 해당 도메인 구현·API는 확인되지 않았다. |
| P6 Outbox & Saga | 기반 부분 구현 | Spring Modulith 이벤트 발행 저장소, Outbox 설정·스케줄러와 관련 테이블이 존재한다. 주문 Saga·보상 흐름은 확인되지 않았다. |
| P7 Admin & Operations | 부분 구현 | `/api/v1/admin/**`에 ADMIN 권한 보호가 적용되고 Category·CatalogProduct·ProductVariant 관리자 진입점이 구현되어 있다. 사용자·판매자 심사 등 나머지 운영 API는 확인되지 않았다. |
| P8 Seller | 부분 구현 | `Seller`, 저장소, `SellerQueryApi`, Catalog의 판매자 조회 어댑터와 활성 판매자 검증 기반이 존재한다. 판매자 등록·프로필·주문 관리 API와 실제 `@ActiveSeller` 적용 지점은 확인되지 않았다. |
| P9 Offer & Marketplace | 미구현 | V1의 Offer·Inventory 테이블은 있으나 도메인 구현과 고객용 Marketplace API는 확인되지 않았다. |
| P10 Review | 스키마만 존재 | V1의 `reviews` 테이블은 있으나 Review 도메인·API·구매 자격 검증은 확인되지 않았다. |
| P11 Auth | 부분 구현 | 로컬·소셜 Credential, 회원가입, Form Login과 OAuth2 서비스·핸들러, JWT·Refresh·Guest 토큰, 로그아웃·블랙리스트와 관련 테스트가 존재한다. 재인증과 인증수단 전체 관리 등 요구사항 전체는 구현되지 않았다. |
| P12 Media | 스키마만 존재 | V1의 `images` 메타데이터 테이블은 있으나 Media 저장소·업로드 세션·파일 검증·완료·첨부 API는 확인되지 않았다. |

## 백엔드 구조

현재 소스 모듈은 `auth`, `user`, `address`, `catalog`, `seller`, `common`, `global`이다. `product`, `cart`, `coupon`, `order`, `payment`, `review`, `media`, `offer`, `inventory` 모듈은 확인되지 않았다.

- `auth`: 로컬·소셜 Credential, 회원가입, 비밀번호 검증·변경, Form Login·OAuth2 처리, Access/Refresh/Guest JWT, 로그아웃·블랙리스트와 인증 이벤트
- `user`: 프로필·다중 역할, 활성 User 식별자 중복 검사, 비활성화 이벤트, 공개 `UserQueryApi`
- `address`: User 소유 주소의 페이지 목록·등록·수정·삭제·기본 배송지 지정. `alias`·`lastUsedAt`, 중복 주소 검사, 기본 배송지 행 잠금과 삭제 후 승격이 구현되어 있다.
- `catalog`: Category·CatalogProduct·ProductVariant·Tag와 저장소·서비스, Category 캐시, 관리자 CatalogProduct·ProductVariant 명령, 식별자 검증기, Category 조회·명령, `CatalogVariantQueryApi`. Seller 공개 API는 `CatalogSellerAdapter`를 통해 참조한다.
- `seller`: Seller 상태·저장소·`SellerQueryApi`
- `common`: 공통 인증 주체·예외·DTO·JPA 저장소·MapStruct 설정
- `global`: Spring 설정, `/api/v1` 경로 prefix, 보안 필터·JWT, 활성 User 인터셉터, 예외 응답, 캐시·Outbox 설정

Spring Modulith `package-info.java`의 `allowedDependencies`와 `@NamedInterface`로 모듈 경계를 관리한다. Catalog는 `seller::api`를 통해 Seller를 참조하고 내부 구현에는 직접 의존하지 않는다.

## 확인된 HTTP 진입점

Controller 경로에는 `WebMvcConfig`가 전역 `/api/v1` prefix를 적용한다. `/api/v1/admin/**`는 `SecurityConfig`에서 ADMIN 역할을 요구한다. Category GET·로그인·회원가입·토큰 갱신·로그아웃 공개 여부는 Security 설정에서 별도로 관리된다.

| Method | Path | 구현 상태 |
|---|---|---|
| POST | `/api/v1/auth/login` | Spring Security Form Login 처리 |
| POST | `/api/v1/auth/logout` | Spring Security Logout·JWT 정리 처리 |
| POST | `/api/v1/auth/signup` | 구현 · 204 |
| POST | `/api/v1/auth/refresh` | 구현 · 200 |
| POST | `/api/v1/auth/password/verify` | 구현 · 활성 User 보호 · 204 |
| POST | `/api/v1/auth/update` | 구현 · 활성 User 보호 · 204 |
| GET | `/api/v1/me` | 구현 · 활성 User 보호 |
| PATCH | `/api/v1/me` | 구현 · 활성 User 보호 |
| POST | `/api/v1/me/deactivate` | 구현 · 204 |
| GET | `/api/v1/me/addresses` | 구현 · 기본 page=0, size=20, 최대 size=100 |
| POST | `/api/v1/me/addresses` | 구현 · 201 |
| PATCH | `/api/v1/me/addresses/{addressId}` | 구현 · 200 |
| DELETE | `/api/v1/me/addresses/{addressId}` | 구현 · 204 |
| POST | `/api/v1/me/addresses/{addressId}/default` | 구현 · 200 |
| GET | `/api/v1/categories` | 구현 · 공개 Category 트리 |
| POST | `/api/v1/admin/categories` | 구현 · ADMIN 전용 · 201 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | 구현 · ADMIN 전용 · 200 |
| POST | `/api/v1/admin/catalog-products` | 구현 · ADMIN 전용 · 201 |
| PATCH | `/api/v1/admin/catalog-products/{id}` | 구현 · ADMIN 전용 · 200 |
| PATCH | `/api/v1/admin/catalog-products/{id}/identifiers` | 구현 · ADMIN 전용 · 200 |
| POST | `/api/v1/admin/catalog-products/{id}/archive` | 구현 · ADMIN 전용 · 200 |
| POST | `/api/v1/admin/catalog-products/{catalogProductId}/variants` | 구현 · ADMIN 전용 · 201 |
| GET | `/api/v1/admin/product-variants/{id}` | 구현 · ADMIN 전용 · 200 |
| PATCH | `/api/v1/admin/product-variants/{id}` | 구현 · ADMIN 전용 · 200 |
| POST | `/api/v1/admin/product-variants/{id}/archive` | 구현 · ADMIN 전용 · 200 |
| GET | `/api/v1/product-variants/{id}` | 구현 · 인증된 활성 User · 공개 필드만 반환 |

현재 확인된 CatalogProduct API에는 공개 목록·상세와 관리자 GET 목록·상세가 없다. ProductVariant 관리자 응답은 내부 식별자·CatalogProduct 식별자·상태를 포함하고, 공개 응답은 `displayName`과 `attributes`만 반환한다. CatalogProduct 생성·식별자 갱신 요청의 `identifiers`는 `ASIN`, `GTIN`, `UPC`, `EAN`, `ISBN` 키를 갖는 맵이다.

## 주요 구현 규칙

- Category는 `parent`·`children` 트리이며 depth 1~3, 순환, 전역 이름 중복, 빈 이름을 검증한다. 조회는 `categories` 캐시를 사용하고 생성·수정 시 무효화한다.
- CatalogProduct은 Category에 소속된 상품군 엔티티이며 `name`, 설명, 브랜드, `identifiers`, attributes, tags, publication status를 가진다. 요청 식별자는 서비스 검증기와 DB 제약으로 검증하며, 하나 이상의 식별자가 필요하다.
- V6는 CatalogProduct에 하나 이상의 식별자 존재 제약을 추가하고, V7은 ASIN·GTIN·UPC·EAN·ISBN 형식 제약을 추가한다. ISBN은 하이픈·공백을 제거한 값으로 형식을 검증한다.
- CatalogProduct은 활성 상태 검증 후 수정·식별자 갱신이 가능하고, archive 시 `ARCHIVED` 상태·시각을 기록한다. 식별자 검증 실패는 형식 오류·중복·ISBN 외부 검증 실패를 필드별 사유로 묶어 반환한다.
- ProductVariant는 하나의 활성 CatalogProduct에만 생성할 수 있고 `displayName`·JSON object `attributes`를 검증한다. `ACTIVE`·`ARCHIVED`와 `archivedAt`의 일관성을 도메인·DB에서 보장하며, 보관된 Variant는 공개 조회·수정·재보관을 거부한다. Offer·Inventory·Media는 ProductVariant가 소유하지 않는다.
- Address 목록 정렬은 `isPrimary DESC, lastUsedAt DESC NULLS LAST, createdAt DESC, id DESC`이다. 기본 배송지 변경·삭제는 사용자 주소 행 잠금으로 처리한다.
- `@RequireEnabledUser`가 붙은 Controller는 `EnabledUserInterceptor`를 통해 `UserQueryApi.requireEnabled()`를 호출한다. `__Host-REAUTH` 검증 구현은 확인되지 않았다.
- Checkstyle은 UTF-8을 명시하고 `NoReplacementCharacterCheck`로 유니코드 대체 문자를, `SuspiciousKoreanEncodingCheck`로 의심스러운 한글·CJK 혼합 토큰을 탐지한다. 예외는 `config/checkstyle/whitelist.yml`의 명시적 목록으로 관리한다.

## 데이터베이스

Flyway 마이그레이션은 `V1__init_schema.sql`부터 `V8__align_product_variant_with_p2.sql`까지 8개다.

- V1: 사용자·Credential·주소·포인트·위시리스트, Category·CatalogProduct·Variant·Offer·Inventory·이미지·태그·리뷰, Cart·Coupon, Order·Payment·Delivery, Outbox·Saga·이벤트 발행, Seller 테이블
- V2: `users.role`을 `user_roles` 다중 역할 테이블로 정규화
- V3: `event_publication.completion_attempts` 기본값 추가
- V4: 활성 User에만 이름·전화번호 고유 제약을 적용하여 비활성 User 식별자 재사용 허용
- V5: Address의 `alias`·`last_used_at`과 User별 `(postal_code, address_line)` 중복 방지 인덱스 추가
- V6: CatalogProduct의 ASIN·GTIN·UPC·EAN·ISBN 중 하나 이상 존재 제약 추가
- V7: CatalogProduct 각 식별자의 형식 제약 추가
- V8: ProductVariant에서 SKU·고정 무게·크기 컬럼을 제거하고 JSON attributes, publication status, archivedAt과 상태·JSON object 제약을 추가

도메인 간 식별자는 Modulith 경계를 따르기 위해 DB 외래 키로 연결하지 않는 정책이며, `addresses.user_id`와 `users.id` 사이의 외래 키는 확인되지 않는다. 현재 Java 엔티티가 확인되는 영역은 사용자·인증·주소·카탈로그·판매자이며, 나머지 테이블은 스키마만 존재한다.

## 프런트엔드

- `amaazon-front/`에 React·TypeScript·Vite 앱이 있고 `npm run lint`, `npm run build` 스크립트가 정의되어 있다.
- 홈·내비게이션·상품 그리드·정적 상품 상세와 로그인·회원가입·OAuth UI가 존재한다.
- 현재 프런트 소스에서 Backend Address·Catalog·Marketplace API 호출은 확인되지 않았다. Address 화면과 상품 데이터는 정적 UI 범위로 남아 있다.

## 테스트·자동화

- JUnit 5, Spring Boot Test, Spring Security Test, Spring Modulith Test, Testcontainers 의존성이 설정되어 있다.
- Auth·User·Address·Catalog·Seller의 단위·Controller·Repository·일부 통합 테스트와 `ModularityTest`가 존재한다. CatalogProduct 식별자 검증과 ProductVariant 도메인·서비스·Repository·Controller·통합·공개 application API 테스트가 포함되어 있다.
- Checkstyle, custom `checkstyle-rules`, JaCoCo와 80% 라인 커버리지 검증 설정이 빌드에 포함되어 있다.
- `.github/workflows/ci.yml`은 backend build·JaCoCo 리포트·Codecov 업로드를 수행한다. CodeQL·Dependabot workflow는 현재 확인되지 않았다.
- 이번 갱신에서는 Gradle과 프런트엔드 명령을 실행하지 않았으므로 현재 통과 여부와 커버리지는 미검증이다.

## 알려진 차이와 다음 작업

1. P1 민감 작업 재인증(`__Host-REAUTH`)과 주문·배송 연계를 구현한다.
2. Address의 `lastUsedAt`은 필드·정렬·삭제 승격 기준만 구현되어 있다. 주문 도메인에서 실제 배송지 사용 시 갱신하는 연계가 필요하다.
3. Address 화면과 API 연동은 프런트에서 확인되지 않았다.
4. `addresses.user_id`와 `users.id`의 DB 외래 키가 확인되지 않았다. 소유 관계를 DB 제약으로 보장할지 결정하고 필요한 Flyway migration을 추가한다.
5. CatalogProduct 공개 목록·상세, 관리자 조회와 상품 Media를 구현한다.
6. P8 Seller 등록·프로필·운영 API와 실제 active-seller 적용 지점을 구현한다.
7. P9 Offer·Inventory·Marketplace, P10 Review, P12 MediaUpload 흐름을 구현한다.
8. 구현 범위가 확장되면 해당 모듈의 API·테스트와 Modulith 경계 검증을 함께 추가한다.

현재 알려진 ProductVariant 차이:

- P2 ProductVariant 문서의 관리자 응답 예시는 `variantId`를 사용하지만 현재 DTO는 Java 필드 `productVariantId`를 JSON `id`로 노출한다. 문서와 응답 계약 중 하나로 통일해야 한다.
- `CATALOG-030`, `CATALOG-032`, `CATALOG-034`, `CATALOG-036` 오류 코드는 정의되어 있으나 생성·조회·수정·보관 저장소 예외를 해당 코드로 변환하는 서비스 흐름은 확인되지 않았다.
- 공개 Variant API는 `@RequireEnabledUser`를 통해 인증된 활성 User를 요구하며, 문서의 구매자·Seller 역할별 허용을 별도로 제한하지 않는다.
