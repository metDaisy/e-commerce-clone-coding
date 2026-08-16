# E-commerce-clone-coding

[![CI Pipeline](https://github.com/metDaisy/e-commerce-clone-coding/actions/workflows/ci.yml/badge.svg)](https://github.com/metDaisy/e-commerce-clone-coding/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/metDaisy/e-commerce-clone-coding/graph/badge.svg?token=qUz12GJ8C4)](https://codecov.io/gh/metDaisy/e-commerce-clone-coding)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)

## 프로젝트 소개

Amazon과 유사한 구매·판매 흐름을 학습하기 위한 이커머스 클론 코딩 프로젝트입니다.

실제 서비스의 용어와 흐름을 참고하되, 구현 난이도를 관리하기 위해 기본 요구사항과 심화사항을 분리합니다. 현재 목표는 기본 요구사항을 먼저 구현하고, 이후 구조를 크게 바꾸지 않고 심화사항으로 확장하는 것입니다.

주요 도메인은 다음과 같습니다.

- 회원가입·로그인·소셜 인증 및 사용자 프로필
- CatalogProduct, ProductVariant, Offer, Inventory 기반 상품·재고 관리
- 장바구니·쿠폰·주문·결제·배송
- Review·Media Upload·도메인별 Media attachment
- Outbox·Saga 기반 이벤트 처리
- 관리자 운영 기능 및 판매자 마켓플레이스 기능

구매자와 판매자는 별도 로그인 계정이 아닙니다. 하나의 `User`가 구매자 역할을 가지면서 `Seller`을 통해 판매자로 활성화될 수 있습니다. 플랫폼 운영 권한은 `ADMIN` 역할로 구분합니다.

## 기술 스택

- Backend: Java 17, Spring Boot, Spring Modulith
- Database: PostgreSQL, Flyway
- Frontend: React, TypeScript, Vite
- Test: JUnit 5, Spring Boot Test, Testcontainers
- Quality: Checkstyle, JaCoCo, CodeQL, CodeRabbit

## 프로젝트 구조

```text
src/main/java/io/github/metdaisy/amaazon/
├── auth/       # 로컬·소셜 인증
├── user/       # 사용자·프로필·주소·역할·활성 상태
├── catalog/    # CatalogProduct 중심 상품 기능
├── seller/     # 판매자
├── common/     # 공통 타입·예외·영속화 기반
└── global/     # 보안·웹·애플리케이션 설정

amaazon-front/ # React/TypeScript/Vite 프론트엔드
docs/          # 요구사항·아키텍처·ADR·개발 가이드
```

백엔드는 Spring Modulith 기반 모듈러 모놀리스 구조를 사용합니다. 모듈 내부는 `presentation → application → domain ← infra` 방향을 유지하고, 모듈 간 결합은 공개 인터페이스나 이벤트를 통해 제한합니다.

## 문서

문서는 [docs/index.md](./docs/index.md)에서 목적별로 탐색할 수 있습니다.

| 문서 | 설명 |
|---|---|
| [요구사항 인덱스](./docs/requirement/index.md) | 공통 규칙과 P1~P12 도메인 요구사항 |
| [Domain ERD](./docs/domain-erd.md) | SQL과 독립적인 도메인 객체·업무 관계 |
| [Architecture](./docs/architecture.md) | 모듈 경계와 의존성 규칙 |
| [Domain Glossary](./docs/domain-glossary.md) | 도메인 용어와 상태값 기준 |
| [ADR](./docs/adr/) | 주요 설계 결정과 선택 이유 |
| [Testing Guide](./docs/testing-guide.md) | 단위·슬라이스·통합 테스트 작성 규칙 |
| [Current State](./docs/current-state.md) | 특정 Git SHA 기준 구현 상태 스냅샷 |
| [Skills Index](./docs/skills/index.md) | 작업별 에이전트 스킬 선택 가이드 |
| [Frontend README](./amaazon-front/README.md) | 프론트엔드 실행·검증 방법 |

## 데이터베이스

기본 PostgreSQL 스키마는 [V1__init_schema.sql](./src/main/resources/db/migration/V1__init_schema.sql)에 정의되어 있습니다.

- SQL 스키마는 테이블·컬럼·제약 조건·외래 키를 설명합니다.
- Domain ERD는 도메인 객체와 업무 관계를 설명하며 SQL 테이블과 일대일 대응하지 않을 수 있습니다.
- 모듈 간 식별자는 애플리케이션 계약으로 검증하고, 모듈 경계를 넘는 DB 외래 키는 만들지 않습니다.

## 구현 기준

현재 동작은 코드·테스트·Flyway 마이그레이션을 기준으로 판단합니다. 구현 목표와 API 계약은 [요구사항 문서](./docs/requirement/index.md)를 기준으로 하며, 두 문서가 다르면 차이를 확인한 뒤 수정합니다.
