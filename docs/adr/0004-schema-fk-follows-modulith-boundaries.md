# ADR-0004: 스키마 외래 키는 Modulith 도메인 경계를 따른다

- Status: Accepted
- Date: 2026-08-09
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

이 프로젝트는 하나의 PostgreSQL 스키마를 사용하는 Spring Modulith 구조다. 데이터베이스가 모든 도메인 간 UUID에 외래 키를 만들면 모듈 간 결합이 생기고, 이벤트 기반 협력과 독립적인 도메인 변경을 어렵게 만든다.

반면 같은 도메인의 엔티티 관계는 데이터 무결성을 위해 DB 외래 키가 필요하다. 기본 요구사항 스키마는 `V1__init_schema.sql`에서 관리한다.

## Decision Drivers

- Modulith 모듈 간 결합을 낮게 유지한다.
- 같은 도메인 내부 데이터 무결성을 DB에서 보장한다.
- 기존 구현과 소셜 로그인 테이블을 보존한다.
- 요구사항 기준 스키마를 새 Flyway migration으로 점진적으로 추가한다.

## Considered Options

### Option A: 모든 ID 관계에 외래 키 생성

무결성은 강해지지만 `auth → user`, `order → payment`, `review → catalog` 같은 모듈 간 결합이 DB에 고정된다.

### Option B: 같은 도메인 내부 관계만 외래 키 생성

도메인 간 ID는 애플리케이션 이벤트·공개 port로 검증하고, 내부 관계만 DB가 강제한다. 모듈 경계는 유지되지만 도메인 간 참조 검증 책임이 애플리케이션에 있다.

## Decision

Option B를 선택한다.

- 같은 도메인 내부 엔티티에는 FK를 생성한다. 예: `product_tags → products/tags`, `cart_items → carts`, `order_items → orders`, `saga_steps → saga_instances`.
- 다른 도메인의 ID는 UUID 컬럼으로 저장하되 FK를 생성하지 않는다. 예: 인증수단의 `user_id`, 리뷰의 `product_id`, 결제·배송의 `order_id`.
- `V1__init_schema.sql`에 요구사항 기준 컬럼·구매 모델·내부 FK를 정의한다.
- 심화사항은 이후 버전의 별도 Flyway migration으로 추가한다.
- 소셜 회원가입은 `users`와 `social_credentials`를 생성하고, 로컬 이메일·비밀번호용 `user_credentials`는 생성하지 않는다.

## Consequences

### Positive

- DB 스키마가 Modulith 모듈 경계를 침범하지 않는다.
- 같은 도메인 내부 삭제·고아 데이터 문제를 DB에서 방지할 수 있다.
- 기존 인증 구현과 migration 이력을 보존하면서 요구사항 스키마를 확장할 수 있다.

### Negative

- 도메인 간 ID의 존재·소유권 검증은 애플리케이션에서 수행해야 한다.
- 이벤트 소비와 데이터 정합성 테스트가 필요하다.
- 같은 물리 스키마 안에 FK가 있는 관계와 없는 관계가 함께 존재한다.

### Follow-up

- 새 모듈 관계를 추가할 때 FK가 Modulith 경계를 넘지 않는지 검토한다.
- 도메인 간 참조는 공개 port 또는 이벤트 계약으로 문서화한다.
- migration 실행 후 내부 FK와 cross-domain FK 부재를 integration test로 검증한다.

## Evidence

- [V1 요구사항 스키마 migration](../../src/main/resources/db/migration/V1__init_schema.sql)
- [아키텍처 문서](../architecture.md)
- [요구사항 인덱스](../requirement/index.md)
