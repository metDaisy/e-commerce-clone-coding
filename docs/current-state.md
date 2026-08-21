# 현재 구현 상태

> 커밋된 코드·테스트 소스·설정·Flyway를 기준으로 정리한 파생 스냅샷이다. 요구사항 문서는 목표 범위를 확인하는 용도로만 사용한다.

## 스냅샷

- 확인일: 2026-08-21
- Git 브랜치: `p2/issue14`
- 구현 기준 Git SHA: `43be16c83c0eac3225bf9b9fc825730d525a1ad4`
- 기준 상태: 위 커밋의 구현만 기술하며, 문서 자체의 미커밋 변경은 구현 완료로 기록하지 않는다.
- 이번 확인: 작업 트리에는 `docs/current-state.md`만 미커밋 상태로 남아 있었다.
- 검증 실행: 이번 문서 확인에서는 Gradle 테스트·`check`·JaCoCo와 프런트엔드 `lint`·`build`를 실행하지 않았다. 테스트 통과나 커버리지는 주장하지 않는다.

## 전체 진행 요약

| 단계 | 상태 | 확인된 범위 |
|---|---|---|
| P1 User & Address | 부분 구현 | User 프로필 조회·수정·비활성화, 회원가입 연계, Address 목록·등록·수정·삭제·기본 배송지 지정, 다중 역할, 활성 User 검사와 주소 정렬·잠금이 구현되어 있다. 요구사항의 재인증(`__Host-REAUTH`)과 주문 연계는 확인되지 않았다. |
| P2 Catalog | 부분 구현 | Category 트리 조회·생성·수정과 CatalogProduct 관리자 생성·수정·식별자 업데이트·archive가 구현되어 있다. 공개 CatalogProduct 조회, ProductVariant, 상품 Media는 확인되지 않았다. |
| P3 Cart | 스키마만 존재 | V1의 `carts`·`cart_items` 테이블은 있으나 해당 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P4 Coupon | 스키마만 존재 | V1의 쿠폰 테이블은 있으나 Coupon 도메인·API는 확인되지 않았다. |
| P5 Order, Payment, Delivery | 스키마만 존재 | V1의 주문·결제·배송 테이블은 있으나 해당 도메인 구현·API는 확인되지 않았다. |
| P6 Outbox & Saga | 기반 부분 구현 | Spring Modulith 이벤트 발행 저장소, Outbox 설정·스케줄러와 관련 테이블이 존재한다. 주문 Saga·보상 흐름은 확인되지 않았다. |
| P7 Admin & Operations | 부분 구현 | `/api/v1/admin/**`에 대한 ADMIN 권한 보호와 Category·CatalogProduct 관리자 진입점이 구현되어 있다. 사용자·판매자 심사 등 나머지 운영 API는 확인되지 않았다. |
| P8 Seller | 부분 구현 | `Seller`, 저장소, `SellerQueryApi`, Catalog의 판매자 검증 어댑터·애스펙트가 존재한다. 판매자 등록·프로필·주문 관리 API는 확인되지 않았다. |
| P9 Offer & Marketplace | 미구현 | V1의 Offer·Inventory 테이블은 있으나 도메인 구현과 고객용 Marketplace API는 확인되지 않았다. |
| P10 Review | 스키마만 존재 | V1의 `reviews` 테이블은 있으나 Review 도메인·API·구매 자격 검증은 확인되지 않았다. |
| P11 Auth | 부분 구현 | 로컬·소셜 Credential, 회원가입, Form Login과 OAuth2 관련 서비스·핸들러, JWT·Refresh·Guest 토큰, 로그아웃·블랙리스트와 관련 테스트가 존재한다. 재인증과 인증수단 전체 관리 등 요구사항 전체는 구현되지 않았다. |
| P12 Media | 스키마만 존재 | V1의 `images` 메타데이터 테이블은 있으나 Media 저장소·업로드 세션·파일 검증·완료·첨부 API는 확인되지 않았다. |

## 백엔드 구조

현재 소스 모듈은 `auth`, `user`, `address`, `catalog`, `seller`, `common`, `global`이다. `product`, `cart`, `coupon`, `order`, `payment`, `review`, `media`, `offer`, `inventory` 모듈은 확인되지 않았다.

- `auth`: Credential, 회원가입, 비밀번호 검증·변경, Form Login과 OAuth2 관련 서비스·핸들러, JWT·Refresh·Guest 토큰, 로그아웃·블랙리스트와 인증 이벤트
- `user`: 프로필·역할, 활성 User 식별자 중복 검사, 비활성화 이벤트, 공개 `UserQueryApi`
- `address`: User 소유 주소의 페이지 목록·등록·수정·삭제·기본 배송지 지정. `alias`·`lastUsedAt`, 중복 주소 검사, 기본 배송지 행 잠금과 삭제 후 승격이 구현되어 있다.
- `catalog`: Category·CatalogProduct·Tag와 저장소·서비스, Category 캐시, 관리자 CatalogProduct 명령과 Category 조회·명령
- `seller`: Seller 상태·저장소·`SellerQueryApi`
- `common`: 공통 인증 주체·예외·DTO·JPA 저장소·MapStruct 설정
- `global`: Spring 설정, `/api/v1` 경로 prefix, 보안 필터·JWT, 활성 User 인터셉터, 예외 응답, 캐시·Outbox 설정

Spring Modulith `package-info.java`의 `allowedDependencies`와 `@NamedInterface`로 모듈 경계를 관리한다. Catalog는 Seller의 공개 API를 어댑터를 통해 참조한다.

## 확인된 HTTP 진입점

Controller 경로에는 `WebMvcConfig`가 전역 `/api/v1` prefix를 적용한다. `/api/v1/admin/**`는 `SecurityConfig`에서 ADMIN 역할을 요구한다.

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
| GET | `/api/v1/categories` | 구현 · Category 트리 |
| POST | `/api/v1/admin/categories` | 구현 · 201 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | 구현 · 200 |
| POST | `/api/v1/admin/catalog-products` | 구현 · 201 |
| PATCH | `/api/v1/admin/catalog-products/{id}` | 구현 · 200 |
| PATCH | `/api/v1/admin/catalog-products/{id}/identifiers` | 구현 · 200 |
| POST | `/api/v1/admin/catalog-products/{id}/archive` | 구현 · 200 |

현재 확인된 CatalogProduct API에는 관리자 GET 목록·상세, 공개 목록·상세, Variant API가 없다.

## 주요 구현 규칙

- Category는 `parent`·`children` 트리이며 depth 1~3을 검증하고, 순환·전역 이름 중복·빈 이름을 거부한다.
- Category 조회는 `categories` 캐시를 사용하고 생성·수정 시 캐시를 무효화한다. 관련 단위·통합 테스트 소스가 존재한다.
- CatalogProduct은 Category에 소속된 상품군 엔티티이며 `name`, 설명, 브랜드, ASIN/GTIN/UPC/EAN/ISBN, attributes, tags, publication status를 가진다. Category처럼 parent/children 트리나 depth 제약을 가진다는 근거는 없다.
- CatalogProduct은 활성 상태 검증 후 수정·식별자 업데이트가 가능하고, archive 시 `ARCHIVED` 상태·시각을 기록한다.
- Address 목록 정렬은 `isPrimary DESC, lastUsedAt DESC NULLS LAST, createdAt DESC, id DESC`이다. 기본 배송지 변경·삭제는 사용자 주소 행 잠금으로 처리한다.
- `@RequireEnabledUser`가 붙은 Controller는 `EnabledUserInterceptor`를 통해 `UserQueryApi.requireEnabled()`를 호출한다. `__Host-REAUTH` 검증 구현은 확인되지 않았다.

## 데이터베이스

Flyway 마이그레이션은 `V1__init_schema.sql`부터 `V5__extend_address_book.sql`까지 5개다.

- V1: 사용자·Credential·주소·포인트·위시리스트, Category·CatalogProduct·Variant·Offer·Inventory·이미지·태그·리뷰, Cart·Coupon, Order·Payment·Delivery, Outbox·Saga·이벤트 발행, Seller 테이블
- V2: `users.role`을 `user_roles` 다중 역할 테이블로 정규화
- V3: `event_publication.completion_attempts` 기본값 추가
- V4: 활성 User에만 이름·전화번호 고유 제약을 적용하여 비활성 User 식별자 재사용 허용
- V5: Address의 `alias`·`last_used_at`과 User별 `(postal_code, address_line)` 중복 방지 인덱스 추가

도메인 간 식별자는 Modulith 경계를 따르기 위해 DB 외래 키로 연결하지 않는 정책이며, `addresses.user_id`와 `users.id` 사이의 외래 키는 확인되지 않는다.

## 프런트엔드

- `amaazon-front/`에 React·TypeScript·Vite 앱이 있고 `npm run lint`, `npm run build` 스크립트가 정의되어 있다.
- 홈·내비게이션·상품 그리드·정적 상품 상세와 로그인·회원가입·OAuth UI가 존재한다.
- Backend Address API 연동과 Address 화면은 확인되지 않았다.
- 상품 화면은 정적 데이터·UI 중심이며 Catalog·Marketplace Backend API 연동은 확인되지 않았다.

## 테스트·자동화

- JUnit 5, Spring Boot Test, Spring Security Test, Spring Modulith Test, Testcontainers 의존성이 설정되어 있다.
- Auth·User·Address·Catalog·Seller의 단위·Controller·Repository·일부 통합 테스트 소스와 `ModularityTest`가 존재한다.
- Checkstyle, JaCoCo와 80% 라인 커버리지 검증 설정이 `build.gradle`에 있다.
- 이번 확인에서는 Gradle과 프런트엔드 명령을 실행하지 않았으므로 현재 통과 여부는 미검증이다.

## 알려진 차이와 다음 작업

1. 이전 문서의 브랜치·구현 SHA(`p1/issue7`, `a78c6c8e`)는 현재 저장소 상태와 달라 `p2/issue14`, `43be16c83c0eac3225bf9b9fc825730d525a1ad4`로 수정했다.
2. CatalogProduct을 Category처럼 트리·2단계 depth로 기술하던 내용과 존재하지 않는 관리자 GET API를 제거했다.
3. `auth`, `user`, `address`를 미구현으로 기술하던 내용과 존재하지 않는 `product` 모듈 경로를 현재 코드에 맞게 수정했다.
4. P1 재인증·주문 연계, P2 공개 Catalog/Variant, P3~P5 도메인 API, P8 Seller API, P9 Marketplace, P10 Review, P12 Media 흐름이 다음 구현 범위다.
