# 현재 구현 상태

> 커밋된 코드에서 재현할 수 있는 파생 스냅샷이다. 구현 상태는 코드·테스트·설정·Flyway를 기준으로 판단하며, 요구사항 문서는 목표 범위를 확인하는 용도로만 사용한다.

## 스냅샷

- 확인일: 2026-08-18
- Git 브랜치: `p1/issue46`
- 구현 기준 Git SHA: `7ef5a39`
- 기준 상태: HEAD의 커밋된 코드·테스트·설정·Flyway를 반영했다. 현재 작업 트리에는 이 문서의 동기화 변경만 있다.
- 이번 문서 동기화일: 2026-08-18. P1 Address 목록·등록 API, User당 주소 최대 5개 제한, 비활성 User 오류 `USER-004`, Auth의 `UserQueryApi` 직접 참조와 `UserDto` 통합을 반영했다.
- 이번 확인에서 Auth 관련 테스트 85개와 Address 관련 테스트 24개가 통과했으며 `ModularityTest`, `checkstyleMain`, `checkstyleTest`도 통과했다. 전체 `check` 결과를 의미하지는 않는다.

## 전체 진행 요약

| 단계 | 상태 | 근거와 범위 |
|---|---|---|
| P1 User & Address | 부분 구현 | `user` 모듈의 프로필 조회·수정·비활성화 API와 `auth` 연계 회원가입, `address` 모듈의 주소 목록·등록 API가 존재한다. 역할이 단일 값에서 `Set<UserRole>`로 정규화되었고, 활성 User 간 이름·연락처 중복 검사와 비활성 User 식별자 재사용, User당 Address 최대 5개 제한이 구현되었다. 주소 수정·삭제·기본 배송지 지정과 P1 재인증은 아직 구현되지 않았다. |
| P2 Catalog | 부분 구현 | 관리자용 카탈로그 상품 생성·수정·archive·식별자 업데이트, 공개 카테고리 트리 조회, ADMIN 전용 카테고리 생성·수정이 구현되어 있다. CatalogProduct 상세, Variant와 상품용 Media 구현은 확인되지 않았다. |
| P3 Cart | 스키마만 존재 | 장바구니 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P4 Coupon | 스키마만 존재 | 쿠폰 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P5 Order & Payment | 스키마만 존재 | 주문·결제·배송 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P6 Infrastructure | 기반 부분 구현 | Spring Modulith 이벤트 발행, Outbox 설정·스케줄러와 관련 테이블이 존재한다. 실제 주문 Saga와 보상 흐름은 확인되지 않았다. |
| P7 Admin & Operations | 부분 구현 | `/admin/**` 전역 ADMIN 보호 아래 카탈로그 상품 생성·수정·archive·식별자 업데이트와 카테고리 생성·수정 진입점이 구현되어 있다. 전용 관리자 모듈과 나머지 운영 API는 확인되지 않았다. |
| P8 Seller | 부분 구현 | `Seller` 엔티티·저장소·`SellerQueryApi`가 존재한다. `ActiveSeller` 검증 어댑터·애스펙트도 남아 있지만 현재 관리자용 카탈로그 컨트롤러에는 연결되어 있지 않으며, 판매자 등록·관리 화면/API는 확인되지 않았다. |
| P9 Offer & Marketplace | 미구현 | Offer·Inventory, 가격·판매 상태, 고객용 Marketplace 검색·상세 API는 확인되지 않았다. |
| P10 Review | 스키마만 존재 | 리뷰 테이블은 V1에 있으나 Review 도메인·API·구매 자격 검증은 확인되지 않았다. |
| P11 Auth | 부분 구현 | `auth` 모듈의 로컬·소셜 Credential, 비밀번호 검증·변경, JWT, 로그아웃·블랙리스트와 관련 테스트가 존재한다. 민감 작업 재인증은 요구사항에 정의되었지만 구현되지 않았고, 인증수단 전체 관리 등 요구사항 전체는 구현되지 않았다. |
| P12 Media | 기반 미구현 | V1의 `images` 메타데이터 테이블은 있으나 `MediaStoragePort` 구현과 `MediaUpload` 세션·파일 검증·완료·첨부 API는 확인되지 않았다. |

## 백엔드 구조

현재 소스 모듈은 `auth`, `user`, `address`, `catalog`, `seller`, `common`, `global`이다. `offer` 디렉터리는 존재하지만 구현 파일은 확인되지 않았다.

- `auth`: 로컬·소셜 Credential, 회원가입, 비밀번호 검증·변경, Access/Refresh/Guest JWT, 로그아웃·토큰/사용자 블랙리스트, 로그인 실패 누적·잠금, 인증 이벤트와 보안 핸들러
- `user`: 사용자 프로필·역할 (도메인 `Set<UserRole>`, API 응답 `List<UserRole>`), 회원가입 이벤트 수신, 프로필 조회·수정, 활성 User 식별자 중복 검사, 계정 비활성화와 `UserDeactivatedEvent`, 공개 `UserQueryApi`
- `address`: User 소유 주소 목록·등록, 기본 배송지 정렬·변경, 활성 User 검증, User당 주소 최대 5개 제한
- `catalog`: `CatalogProduct`, `Category`, `Tag`, `CatalogProductTag`와 저장소·서비스, 관리자용 상품 생성·수정·archive·식별자 검증, 카테고리 명령·조회
- `seller`: `Seller`, `SellerStatus`, 저장소, `SellerQueryApi`, 카탈로그 모듈용 어댑터
- `common`: 공통 인증 주체·예외·DTO·JPA 저장소·MapStruct 설정
- `global`: Spring 설정, 보안 필터·JWT, 예외 응답, 캐시, Outbox 설정·스케줄러

Spring Modulith `package-info.java`의 `allowedDependencies`와 Named Interface로 모듈 경계를 관리한다.

## 확인된 HTTP 진입점

아래는 실제 외부 HTTP 경로 기준이다. Controller 선언 경로에는 `WebMvcConfig`를 통해 전역 `/api/v1` prefix가 적용되며, 이는 Spring `context-path`가 아니라 Controller 경로 prefix다.

| Method | Path | 상태 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 구현 |
| POST | `/api/v1/auth/refresh` | 구현 |
| POST | `/api/v1/auth/password/verify` | 구현 |
| POST | `/api/v1/auth/update` | 구현 |
| GET | `/api/v1/me` | 구현 |
| PATCH | `/api/v1/me` | 구현 |
| POST | `/api/v1/me/deactivate` | 구현 |
| GET | `/api/v1/me/addresses` | 구현 · User당 최대 5개 전체 목록 반환 |
| POST | `/api/v1/me/addresses` | 구현 · User당 최대 5개 제한 |
| POST | `/api/v1/admin/catalog-products` | 구현 · `ADMIN` 전용 |
| PATCH | `/api/v1/admin/catalog-products/{id}` | 구현 · `ADMIN` 전용 |
| PATCH | `/api/v1/admin/catalog-products/{id}/identifiers` | 구현 · `ADMIN` 전용, 상품 식별자(ASIN/GTIN/UPC/EAN/ISBN) 검증 업데이트 |
| POST | `/api/v1/admin/catalog-products/{id}/archive` | 구현 · `ADMIN` 전용 |
| GET | `/api/v1/categories` | 구현 |
| POST | `/api/v1/admin/categories` | 구현 · `ADMIN` 전용 |
| PATCH | `/api/v1/admin/categories/{categoryId}` | 구현 · `ADMIN` 전용 |

Form Login, OAuth2 callback, logout은 Spring Security filter·handler에서 처리되어 Controller 목록에 나타나지 않는다.

현재 P1 보호 API는 인증된 principal을 요구하지만 `__Host-REAUTH` 재인증 쿠키 검증은 구현되지 않았으며, 해당 책임은 P11 Auth에 남아 있다.

현재 확인되지 않은 카탈로그 API:

- `GET /api/v1/catalog-products` 목록·검색
- `GET /api/v1/catalog-products/{id}` 상세
- Variant 등록·조회 API
- Offer·Inventory 등록·조회 API와 고객용 Marketplace API

## 데이터베이스

- Flyway 마이그레이션은 `V1__init_schema.sql`, `V2__normalize_user_roles.sql`, `V3__fix_event_publication_completion_attempts.sql`, `V4__allow_reuse_of_disabled_user_identifiers.sql` 네 파일이며 `application.yml`에서 활성화되어 있다.
- V1에는 사용자·인증·주소·포인트·관심상품, 카탈로그·Variant·Offer·Inventory·이미지·태그·리뷰, 장바구니, 쿠폰, 주문·결제·배송, Outbox·Saga·이벤트 발행, 판매자 테이블이 정의되어 있다. 이는 P11 인증 지원과 P12 공통 이미지 메타데이터의 저장 기반을 포함하지만, P11·P12 요구사항 API 전체를 의미하지 않는다.
- V2는 `users.role` 단일 열을 `user_roles` 다중 역할 테이블로 정규화하고 기존 데이터를 마이그레이션한다.
- V3는 `event_publication.completion_attempts` 기본값을 0으로 명시한다.
- V4는 `users` 테이블의 `name`·`phone_number` 고유 인덱스를 `is_enabled = TRUE` 조건으로 변경하여 비활성화 사용자의 식별자 재사용을 허용한다.
- 현재 Java 엔티티가 확인되는 영역은 사용자·인증·주소·카탈로그·판매자이며, 나머지 테이블은 스키마만 존재한다.
- `catalog_products`와 `CatalogProduct`는 현재 구현 대상이며, `product_variants`, `offers`, `inventories`는 현재 구현되지 않았다.
- 테이블 존재만으로 도메인 로직·상태 전이·동시성 제어·API 완료를 의미하지 않는다.

## 프론트엔드

- `amaazon-front/`에 React/TypeScript/Vite 애플리케이션이 있다.
- 홈·내비게이션·배너·상품 그리드와 로그인·회원가입·OAuth UI가 존재한다.
- 상품 화면은 프론트 정적 데이터와 UI를 사용하며 P2 Catalog·P9 Marketplace 백엔드 연동 완료로 확인되지 않았다.

## 테스트와 자동화

- JUnit 5, Spring Boot Test, Spring Security Test, Spring Modulith Test, Testcontainers 의존성이 설정되어 있다.
- 카테고리·카탈로그 관리자 컨트롤러와 서비스·리포지토리 테스트 소스가 존재한다. P1 User·Address의 서비스·컨트롤러·Repository·HTTP 통합 테스트가 강화되었으며, 활성 User 중복·비활성 User 식별자 재사용·저장 제약조건·역할 Fetch 조회·주소 최대 개수·비활성 User 접근을 검증한다. 이번 확인에서 Auth 테스트 85개와 Address 테스트 24개, Modulith 경계 테스트가 통과했다.
- JaCoCo와 80% 라인 커버리지 검증 설정, Checkstyle 및 custom `checkstyle-rules` 모듈이 빌드에 포함되어 있다.
- CI에는 build, JaCoCo 리포트, Codecov, CodeQL, OpenGrep, CodeRabbit 관련 설정이 있다.

## 현재 주요 불일치와 다음 작업

1. 요구사항에 정의된 P2 Catalog 상세·Variant 기능과 P9 Offer·Inventory·Marketplace 기능이 현재 코드에 없다.
2. 관리자 상품 생성·수정·archive·식별자 업데이트 API가 구현되었으며 ASIN/GTIN/UPC/EAN/ISBN 검증 어댑터가 추가되었다.
3. P1 사용자 역할이 `Set<UserRole>`로 정규화되었으며, 프로필 수정 API(`PATCH /api/v1/me`), 계정 비활성화 API(`POST /api/v1/me/deactivate`)와 `UserDeactivatedEvent`가 구현되었다. 프로필 조회 경로가 `/api/v1/users/me`에서 `/api/v1/me`로 변경되었다. 활성 User 간 이름·연락처 중복 방지와 비활성 User 식별자 재사용이 V4 마이그레이션 및 Repository·HTTP 통합 테스트로 확인되었다.
4. V1 스키마의 P3~P10 테이블 대부분에 대응하는 도메인 모듈과 API가 없고, P12 `MediaUpload` 세션·검증 API도 없다.
5. 카테고리 생성·수정 API는 `/api/v1/admin/categories`에 구현되었으며, 기본 요구사항에는 카테고리 삭제 API를 두지 않는다.
6. P11 민감 작업 재인증은 요구사항·정책만 정의된 상태이며 구현은 남아 있다.
7. 예외 응답 구조에 `AmaazonExceptionContext`와 시스템 메시지, `FORBIDDEN` 응답 타입이 추가되었다.
8. P1 Address 수정·삭제·기본 배송지 지정 API가 아직 구현되지 않았다.
9. P2 Catalog 상세·Variant와 이후 P9 Offer·Marketplace, P10 Review, P12 Media 흐름을 구현한다.
10. 구현 범위가 확장되면 해당 모듈의 테스트와 Modulith 경계 검증을 함께 추가한다.
