# 현재 구현 상태

> 커밋된 코드에서 재현할 수 있는 파생 스냅샷이다. 구현 상태는 코드·테스트·설정·Flyway를 기준으로 판단하며, 요구사항 문서는 목표 범위를 확인하는 용도로만 사용한다.

## 스냅샷

- 확인일: 2026-08-12
- Git 브랜치: `p2-product/issue18`
- 구현 기준 Git SHA: `6596f778826415b808f2d55e97bfbe93d1251e62`
- 기준 상태: 구현 기준 SHA 이후의 uncommitted 구현 변경은 포함하지 않았다.
- 이번 확인에서는 Gradle 테스트를 실행하지 않았다. 아래 내용은 정적 코드·설정·마이그레이션 확인 결과이며 테스트 통과를 의미하지 않는다.

## 전체 진행 요약

| 단계 | 상태 | 근거와 범위 |
|---|---|---|
| P1 User & Auth | 부분 구현 | `auth`·`user` 모듈의 회원가입, Credential, 로컬·소셜 인증 지원, JWT, 로그아웃/블랙리스트, 사용자 프로필 API와 관련 테스트가 존재한다. 주소록·포인트·관심상품·인증수단 전체 관리 등 요구사항 전체는 구현되지 않았다. |
| P2 Catalog & Inventory | 부분 구현 | 카탈로그 상품 생성·수정과 카테고리 목록 조회가 구현되어 있다. 상품 목록·상세 조회, Variant·Offer·Inventory 구현과 관련 테스트는 확인되지 않았다. |
| P3 Cart | 스키마만 존재 | 장바구니 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P4 Coupon | 스키마만 존재 | 쿠폰 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P5 Order & Payment | 스키마만 존재 | 주문·결제·배송 테이블은 V1에 있으나 도메인 모듈·API·구현 테스트는 확인되지 않았다. |
| P6 Infrastructure | 기반 부분 구현 | Spring Modulith 이벤트 발행, Outbox 설정·스케줄러와 관련 테이블이 존재한다. 실제 주문 Saga와 보상 흐름은 확인되지 않았다. |
| P7 Admin & Operations | 미구현 | 전용 관리자 모듈·관리 API·운영 기능은 확인되지 않았다. |
| P8 Seller & Marketplace | 부분 구현 | `Seller` 엔티티·저장소·조회 API와 카탈로그 생성 시 `PRODUCT_MANAGER`의 활성 판매자 검증이 존재한다(`ADMIN`은 검증을 우회한다). 판매자 등록·관리 화면/API와 Offer 기반 판매 흐름은 확인되지 않았다. |

## 백엔드 구조

현재 소스 모듈은 `auth`, `user`, `catalog`, `seller`, `common`, `global`이다. `offer` 디렉터리는 존재하지만 구현 파일은 확인되지 않았다.

- `auth`: 로컬·소셜 Credential, 회원가입, 비밀번호 검증·변경, Access/Refresh/Guest JWT, 로그아웃·토큰/사용자 블랙리스트, 로그인 실패 누적·잠금, 인증 이벤트와 보안 핸들러
- `user`: 사용자 프로필·역할, 회원가입 이벤트 수신, 프로필 조회·수정
- `catalog`: `CatalogProduct`, `Category`, `Tag`, `CatalogProductTag`와 저장소·서비스, 카탈로그 상품 생성 및 판매자 검증
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
| GET | `/api/v1/users/me` | 구현 |
| POST | `/api/v1/users/update` | 구현 |
| POST | `/api/v1/catalog-products` | 구현·`PRODUCT_MANAGER`는 활성 판매자 검증, `ADMIN`은 우회 |
| PATCH | `/api/v1/catalog-products/{id}` | 구현·`PRODUCT_MANAGER`는 활성 판매자 및 소유자 검증, `ADMIN`은 우회 |
| GET | `/api/v1/categories` | 구현 |

Form Login, OAuth2 callback, logout은 Spring Security filter·handler에서 처리되어 Controller 목록에 나타나지 않는다.

현재 확인되지 않은 카탈로그 API:

- `GET /api/v1/catalog-products` 목록·검색
- `GET /api/v1/catalog-products/{id}` 상세
- `PATCH /api/v1/catalog-products/{id}/codes` 상품 코드 업데이트 (DTO는 존재하나 API 미구현)
- Variant·Offer·Inventory 등록·조회 API
- 카테고리 생성·수정·삭제 관리자 API

## 데이터베이스

- Flyway 마이그레이션은 `V1__init_schema.sql` 하나이며 `application.yml`에서 활성화되어 있다.
- V1에는 사용자·인증·주소·포인트·관심상품, 카탈로그·Variant·Offer·Inventory·이미지·태그·리뷰, 장바구니, 쿠폰, 주문·결제·배송, Outbox·Saga·이벤트 발행, 판매자 테이블이 정의되어 있다.
- 현재 Java 엔티티가 확인되는 영역은 사용자·인증·카탈로그·판매자이며, 나머지 테이블은 스키마만 존재한다.
- `catalog_products`와 `CatalogProduct`는 현재 구현 대상이며, `product_variants`, `offers`, `inventories`는 현재 구현되지 않았다.
- 테이블 존재만으로 도메인 로직·상태 전이·동시성 제어·API 완료를 의미하지 않는다.

## 프론트엔드

- `amaazon-front/`에 React/TypeScript/Vite 애플리케이션이 있다.
- 홈·내비게이션·배너·상품 그리드와 로그인·회원가입·OAuth UI가 존재한다.
- 상품 화면은 프론트 정적 데이터와 UI를 사용하며 P2 백엔드 연동 완료로 확인되지 않았다.

## 테스트와 자동화

- JUnit 5, Spring Boot Test, Spring Security Test, Spring Modulith Test, Testcontainers 의존성이 설정되어 있다.
- `src/test`에서 Java 테스트 파일 41개를 확인했다. 이번 갱신에서는 실행 결과를 확인하지 않았다.
- JaCoCo와 80% 라인 커버리지 검증 설정, Checkstyle 및 custom `checkstyle-rules` 모듈이 빌드에 포함되어 있다.
- CI에는 build, JaCoCo 리포트, Codecov, CodeQL, OpenGrep, CodeRabbit 관련 설정이 있다.

## 현재 주요 불일치와 다음 작업

1. 요구사항에 정의된 P2 목록·상세 조회와 Variant·Offer·Inventory 기능이 현재 코드에 없다.
2. 상품 코드 업데이트 DTO(`CatalogProductCodeUpdateRequest`, `CatalogProductCodeResponse`)는 존재하나 API 엔드포인트로 연결되지 않았다.
3. V1 스키마의 P3~P8 테이블 대부분에 대응하는 도메인 모듈과 API가 없다.
4. 카테고리 관리 API는 P7 관리자 기능으로 분리되어야 하며 현재 구현되지 않았다.
5. P2 API와 Catalog 테스트를 추가하고, 이후 P8 판매자·Offer 흐름을 구현한다.
6. 구현 범위가 확장되면 해당 모듈의 테스트와 Modulith 경계 검증을 함께 추가한다.
