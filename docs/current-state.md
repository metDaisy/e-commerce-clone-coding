# 현재 구현 상태

> 관리 방식: 커밋된 코드에서 다시 만들 수 있는 파생 스냅샷이다. 의미 있는 기능 커밋,
> 모듈·라우트·스키마 변경 후 Continue의 `/update-current-state` Prompt로 갱신한다.
> 작업 트리가 깨끗하지 않으면 자동 갱신하지 않는다. 사람의 고민과 작업 과정은 `dev-dairy.md`, 결정 이유는 `adr/`에 보존한다.

## 스냅샷

- 확인일: 2026-08-06
- Git 브랜치: `product/issue17`
- 기준 Git SHA: `b26bd01c9e1b94e189a9d762548af69f6087f77e`
- 코드 지식 그래프: 기준 SHA와 일치
- 검증 범위: 요구사항, 구현 계획, 개발 일지, 코드 그래프, 핵심 회원가입 코드, Flyway, 빌드 설정, 최근 커밋
- 이번 확인에서는 Gradle 테스트를 실행하지 않았다. 아래 내용은 코드 존재와 구조에 대한 상태이며 전체 테스트 통과 선언이 아니다.

현재 HEAD가 기준 SHA와 다르면 이 문서를 그대로 신뢰하지 말고 변경 영향을 먼저 확인한다.

## 전체 진행 요약

| 단계 | 상태 | 확인된 범위 |
|---|---|---|
| P1 User & Auth | 진행 중 | 회원가입, 프로필 조회·수정, 로컬·소셜 인증 기반, JWT, 토큰 블랙리스트, 계정 비활성화 기반, **계정 잠금, 로그인 시도 횟수 관리** |
| P2 Catalog & Inventory | 미구현 | Flyway 테이블과 프론트 mock 상품 UI만 존재; 백엔드 도메인 모듈 없음 |
| P3 Cart | 미구현 | Flyway 테이블만 존재 |
| P4 Coupon | 미구현 | Flyway 테이블만 존재 |
| P5 Order & Payment | 미구현 | Flyway 테이블만 존재 |
| P6 Outbox & Saga | 기반만 존재 | Spring Modulith event publication 테이블, Outbox 설정·스케줄러 골격; 주문 Saga 없음 |

## 백엔드 현황

### 구현된 모듈

- `auth`: 로컬·소셜 Credential, 회원가입 진입점, 비밀번호 정책, Form/OAuth2 로그인 지원 클래스, Access·Refresh·Guest JWT, 로그아웃과 사용자·토큰 블랙리스트, 로그인 정책(`LoginPolicyProperties`), 계정 잠금 로직(`UserCredential`의 `violationCount`/`untilLocked`), 로그인 실패/성공/로그아웃 이벤트(`IncorrectPasswordEvent`, `FormLoginSuccessEvent`, `JwtLogoutSuccessEvent`) 및 핸들러(`UserCredentialEventHandler`), **서비스 분리(`UserCredentialService`, `SocialCredentialService`에 Credential 생성/수정/삭제 위임)**, **FormLoginSuccessHandler에서 로그인 성공 이벤트 발행 및 게스트 → 회원 전환 이벤트(`SocialSignUpTask`) 발행**.
- `user`: 사용자 프로필, 역할, 활성 상태, 프로필 조회·수정, 회원가입 프로필 생성(`FormSignUpTask`, `SocialSignUpTask` 이벤트 수신).
- `common`: 공통 인증 주체, 예외, JPA 저장소, MapStruct 설정.
- `global`: Spring 설정, 보안 필터와 JWT, 캐시, 공통 예외 응답 전략, 웹 설정, Outbox 기반.

### 확인된 HTTP 진입점

- `POST /auth/signup`
- `POST /auth/refresh`
- `POST /auth/password/verify`
- `POST /auth/update`
- `GET /users/me`
- `POST /users/update`

Form Login, OAuth2 callback, logout의 일부 흐름은 Spring Security handler와 filter로 구성되어 컨트롤러 라우트 목록에 모두 나타나지 않는다.

### 현재 회원가입 구조

1. `AuthController`가 `SignUpRequest`를 받는다.
2. `auth`의 Bean Validation이 이메일·비밀번호 형식을 검사한다.
3. `AuthService`가 이메일 중복을 확인하고 비밀번호를 해시하여 `UserCredential`을 만든다.
4. 평문 비밀번호를 제외한 `FormSignUpTask` (또는 `SocialSignUpTask`) 를 발행한다.
5. `user`의 `UserEventHandler`가 동기 수신하여 프로필을 저장한다.
6. `AuthService`가 Credential을 저장한다.

**로그인 정책**: `LoginPolicyProperties`(시도 최대 횟수, 잠금 해제 시간) + `LoginPolicyConfig` 설정. `UserCredential`에 `violationCount`/`untilLocked` 필드 추가. `FormUserDetailsService`에서 계정 잠금 상태 검증. `UserCredentialJpaRepository`에 PESSIMISTIC_WRITE 락 적용.

**서비스 분리**: `AuthService`가 `UserCredentialService`, `SocialCredentialService`에 Credential 생성/수정/삭제 책임을 위임.

**로그인 성공 핸들러**: `FormLoginSuccessHandler`가 JWT 발급 후 `FormLoginSuccessEvent`를 발행하고, 게스트 쿠키가 있으면 `SocialSignUpTask` 이벤트를 발행하여 게스트 → 회원 전환을 처리한다.

개발 일지에서 제시한 "평문 비밀번호를 auth에 국한"하는 방향은 반영됐다. 다만 사용자 프로필 생성은 작은 동기 인터페이스 호출이 아니라 명령 성격의 `FormSignUpTask` 이벤트를 사용한다. 이 방식의 실패·트랜잭션 의미를 ADR로 확정할 필요가 있다.

### 아직 확인되지 않은 P1 요구사항

다음 항목은 현재 전용 모듈·진입점이 없거나 요구사항 전체 구현을 확인할 수 없다.

- 주소록 CRUD, 사용자당 5개 제한, 기본 배송지 원자적 전환
- 포인트 적립·사용·만료 원장과 동시성 제어
- 관심상품 토글과 조회
- **로그인 실패 5회에 따른 계정 잠금** (기능 구현됨, 테스트 범위 확정 필요)
- 인증수단 연결·해제와 마지막 수단 보호
- 관리자 역할 변경과 리소스 소유권 정책 전체
- 비활성화 90일 후 개인정보 마스킹 배치

## 데이터베이스 현황

- Flyway `V1__init_schema.sql`에 `users`, 인증 테이블뿐 아니라 상품, 장바구니, 쿠폰, 주문, 결제, 배송 등 P1~P6 목표 테이블이 정의되어 있다.
- `event_publication`은 Spring Modulith 이벤트 발행 저장 기반이다.
- `user_credentials` 테이블에 `violation_count`, `until_locked` 컬럼이 추가되었다.
- 계정 잠금 기능이 구현되었으며, 로그인 성공 시 `UserCredentialService.resetViolationOrNot`이 호출되어 잠금 해제 시간을 경과하면 위반 카운트가 초기화된다.
- DB 테이블 존재는 해당 도메인 로직, 상태 머신, 동시성 제어, API 구현 완료를 의미하지 않는다.
- 이후 스키마 변경은 V1을 수정하기보다 새 마이그레이션을 추가하는 것이 기본이다.

## 프론트엔드 현황

- React/TypeScript/Vite 프로젝트가 `amaazon-front/`에 있다.
- 홈 레이아웃, 내비게이션, 배너, 상품 그리드·스크롤 UI가 존재한다.
- 로그인, 회원가입, OAuth callback과 소셜 로그인 UI 코드가 존재한다.
- 상품 데이터는 현재 `src/data/products.ts`의 프론트 데이터가 포함되어 있으며 P2 백엔드 연동 완료로 간주하지 않는다.

## 테스트와 자동화 기반

- JUnit 5, Spring Boot Test, Spring Security Test, Spring Modulith Test, Testcontainers가 설정되어 있다.
- 코드 그래프에서 백엔드 테스트 파일 34개를 확인했다.
- JaCoCo 라인 커버리지 기준은 80%다.
- GitHub Actions, Codecov, CodeQL, OpenGrep, CodeRabbit 관련 설정이 존재한다.
- 실제 최신 테스트 결과는 Gradle MCP 또는 CI 결과로 별도 확인해야 한다.

## 해결해야 할 충돌과 결정

1. 요구사항은 도메인 간 직접 Bean 주입을 금지하고 이벤트 통신을 요구하지만 현재 `auth`는 `user::user-api` 동기 Named Interface도 사용한다. 허용할 조회 seam의 기준을 ADR로 정해야 한다.
2. `auth`와 `user`가 서로의 Named Interface를 허용한다. 신규 기능은 현재 순환 의존을 확대하지 않아야 한다.
3. `FormSignUpTask`/`SocialSignUpTask`는 도메인 사실보다 명령에 가깝다. 이벤트로 유지할지 동기 프로필 생성 인터페이스로 바꿀지 결정해야 한다.
4. Product, Delivery, Payment의 목표 상태값과 V1 DB CHECK 제약조건이 다르다. P2/P5 구현 전 정렬해야 한다.
5. 요구사항의 UserCredential 1:N과 현재 설계·구현의 0..1 관계가 다르다.
6. 구현 계획은 Spring Boot 3.5.15를 적었지만 현재 빌드는 3.5.16이다. 현재 빌드 파일을 실행 기준으로 사용한다.
7. 구현 계획은 PostgreSQL 16, README 배지는 PostgreSQL 17을 가리킨다. 지원 버전을 확정해야 한다.

## 다음 진행 순서

구현 계획을 따르되 다음 순서로 불확실성을 먼저 제거한다.

1. 회원가입 seam과 이벤트 의미를 결정하고 첫 ADR로 기록한다.
2. P1의 누락 기능과 요구사항 대비 테스트 범위를 확정한다.
3. DB 상태값 충돌을 구현 전에 정리한다.
4. P1을 검증한 뒤 P2 `catalog`·`inventory` 모듈 seam을 설계한다.
5. 모듈 추가 시 `architecture.md`, 이 문서의 날짜와 Git SHA를 함께 갱신한다.
