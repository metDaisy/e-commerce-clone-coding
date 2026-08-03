# 아키텍처

## 목적과 범위

하나의 Spring Boot 배포 단위 안에서 도메인별 변경과 검증을 지역화하는 모듈러 모놀리스를 구축한다. 모듈은 작은 공개 인터페이스 뒤에 비즈니스 규칙과 저장 세부사항을 숨기고, 향후 MSA 전환 시 모듈 seam을 서비스 seam으로 발전시킬 수 있어야 한다.

이 문서는 `docs/requirement.md`와 `docs/implmentation_plan.md`의 목표 구조와 장기간 유지할 구조 원칙을 설명한다. 브랜치, Git SHA, 구현 완료 범위처럼 자주 바뀌는 정보는 `docs/current-state.md`에서 관리한다.

구조, 모듈 seam, 허용 의존성이 바뀔 때만 이 문서를 갱신한다. 선택 이유는 이 문서에 누적하지 않고 ADR에 기록한다.

## 시스템 구성

```mermaid
flowchart LR
    Browser[Browser] --> Front[React + TypeScript\namaazon-front]
    Front -->|REST / HttpOnly Cookie| Web[Spring MVC + Security]
    Front -.->|목표: 재고 WebSocket| Web
    Web --> DB[(PostgreSQL)]
    Web -->|OAuth2| Providers[Google / Naver / Kakao / GitHub]

    subgraph Backend[Spring Boot Modular Monolith]
        Web
        Auth[auth]
        User[user]
        Common[common]
        Global[global]
        Future[목표: catalog / inventory / cart / coupon / order / payment / delivery]
    end
```

- 백엔드: Java 17, Spring Boot 3.5.16, Spring Modulith 1.4.12, Spring Data JPA, Flyway, Spring Security.
- 데이터베이스: PostgreSQL. 테스트 환경은 Testcontainers 사용을 전제로 한다.
- 프론트엔드: React 19, TypeScript, Vite, React Router, React Hook Form, Zod.
- 배포 모델: 현재 백엔드와 프론트엔드는 별도 프로젝트 디렉터리지만 백엔드는 하나의 프로세스와 하나의 DB를 사용하는 모듈러 모놀리스다.

## 저장소 구조

```text
src/main/java/io/github/metdaisy/amaazon/
  auth/       인증 수단, 로그인, JWT 연계
  user/       사용자 프로필과 역할
  common/     모듈 공통 타입과 기반 인터페이스
  global/     보안·웹·캐시·예외·Outbox 기반 설정
src/main/resources/
  db/migration/   Flyway 스키마
src/test/         백엔드 테스트
amaazon-front/    React 프론트엔드
docs/             요구사항, 설계, 상태 문서
```

## 모듈 내부 구조

도메인 모듈은 다음 책임을 따른다.

| 영역 | 책임 |
|---|---|
| `presentation` | HTTP 요청·응답, 인증 주체 추출, 입력 검증 진입점 |
| `application` | 유스케이스 조정, 트랜잭션, 공개 port와 이벤트 처리 |
| `domain` | 엔티티, 값과 상태 전이, 도메인 오류, 저장소 인터페이스 |
| `infra` | JPA, 외부 시스템, 보안 프레임워크 등의 adapter |

호출자는 모듈의 공개 인터페이스만 알아야 한다. JPA 엔티티나 다른 모듈의 `infra` 구현을 직접 참조하지 않는다.

## 현재 모듈과 공개 seam

`package-info.java`에 선언된 현재 허용 의존성은 다음과 같다.

| 모듈 | 책임 | 허용 의존성 |
|---|---|---|
| `common` | 공통 인증 주체, 저장소·매퍼·예외 기반 타입 | 없음 |
| `global` | Spring 설정, 공통 보안/JWT, 캐시, 웹 필터, Outbox 기반 | `common::*` |
| `auth` | 로컬·소셜 인증수단, 토큰, 블랙리스트, 회원가입 진입점 | `common::*`, `user::event`, `user::user-api`, `global::jwt`, `global::blacklist` |
| `user` | 프로필, 역할, 활성 상태 | `common::*`, `auth::signup`, `auth::password` |

현재 공개된 주요 `@NamedInterface`는 다음과 같다.

- `user::user-api`: 인증 모듈이 사용자 존재 여부, 역할, 활성 상태를 조회하는 동기 seam.
- `auth::signup`: `SignUpTask`를 통해 프로필 생성을 요청하는 현재 회원가입 seam.
- `auth::password`: 회원가입 요청의 비밀번호 검증 규칙.
- `global::jwt`: JWT 설정과 생성·검증 기능.
- `global::blacklist`: 토큰 무효화 이벤트.

## 현재 회원가입 흐름

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant UserEventHandler
    participant UserService
    participant DB

    Client->>AuthController: POST /auth/signup
    AuthController->>AuthService: SignUpRequest
    AuthService->>AuthService: 이메일·비밀번호 검증 및 해시
    AuthService->>UserEventHandler: SignUpTask 발행
    UserEventHandler->>UserService: 프로필 생성
    UserService->>DB: users 저장
    AuthService->>DB: user_credentials 저장
```

- 평문 비밀번호는 `auth` 요청과 검증·해시 범위에 머물며 `SignUpTask`에는 포함되지 않는다.
- Spring의 기본 `@EventListener`는 동기 실행이므로 현재 프로필과 인증수단 생성은 발행자 트랜잭션에 결합되어 있다.
- `SignUpTask`는 완료 사실보다 프로필 생성을 지시하는 명령 성격이 강하다. 이를 유지할지 동기 인터페이스 seam으로 바꿀지는 ADR로 확정해야 한다.
- `auth`와 `user`가 서로의 Named Interface에 의존하므로 새 의존성을 추가하기 전에 순환을 더 키우지 않는지 확인해야 한다.

## 목표 도메인 모듈

| 단계 | 목표 모듈 | 핵심 책임 |
|---|---|---|
| P1 | `user`, `auth` | 프로필, 인증수단, 권한, 주소, 포인트, 관심상품 |
| P2 | `catalog`, `inventory`, `review` | 카테고리, 상품, 이미지, 검색, 타임세일, 재고, 리뷰 |
| P3 | `cart` | 활성 장바구니, 항목과 수량, 결제 전 재검증 |
| P4 | `coupon` | 쿠폰 발행·보유·사용·만료 |
| P5 | `order`, `payment`, `delivery` | 금액 산출, 상태 머신, 결제와 환불, 배송 추적 |
| P6 | `outbox`와 Saga 참여 모듈 | 이벤트 유실 방지, 재시도, 멱등 소비, 보상 트랜잭션 |

모듈 이름과 분리 수준은 구현 전 ADR로 확정한다. 요구사항의 P 번호가 반드시 하나의 코드 모듈을 뜻하지는 않는다.

## 데이터와 트랜잭션

- 모든 모듈은 현재 하나의 PostgreSQL 스키마를 공유한다.
- `V1__init_schema.sql`에는 P1~P6 목표 테이블이 미리 정의되어 있지만 테이블 존재는 도메인 구현 완료를 의미하지 않는다.
- 스키마 변경은 새 Flyway 마이그레이션으로 적용한다.
- 모듈 내부 원자성은 로컬 DB 트랜잭션으로 보장한다.
- 목표 Outbox는 비즈니스 변경과 이벤트 레코드를 같은 트랜잭션에 기록한다.
- 목표 Saga는 각 단계와 보상을 독립 트랜잭션, 재시도 가능, 멱등하게 처리한다.

## 통신 원칙

- 외부 클라이언트: REST, 목표 재고 알림은 WebSocket.
- 인증: Spring Security Form Login, OAuth2, JWT HttpOnly 쿠키.
- 모듈 간 사실 통지: Spring Application Event.
- 모듈 간 동기 조회: 필요성이 명확할 때만 작은 Named Interface seam.
- 외부 결제·스토리지: 도메인 인터페이스 뒤의 adapter로 격리한다.
- 이벤트 소비자는 동일 이벤트가 여러 번 전달되어도 결과가 중복되지 않아야 한다.

## 검증

- Spring Modulith 구조 검증으로 허용하지 않은 의존성을 탐지한다.
- 모듈 테스트는 공개 인터페이스와 이벤트를 테스트 표면으로 사용한다.
- 저장소·외부 연동은 adapter별 통합 테스트를 둔다.
- 모든 Gradle 검증은 `gradle-mcp`로만 실행한다.
