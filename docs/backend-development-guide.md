# Backend Development Guide

## 목적과 문서 경계

이 문서는 Java 17, Spring Boot와 Spring Modulith backend를 구현할 때 적용하는 project-specific convention을 정의한다. 현재 동작은 checkout된 code·test·configuration·Flyway migration이, 목표 동작은 `requirement/`가 기준이다.

| 문서·파일 | 책임 |
|---|---|
| [`architecture.md`](architecture.md) | 안정적인 module 경계, 계층 의존 방향, 통신·data 구조 원칙 |
| 이 문서 | code 배치, Spring transaction, DTO·mapping, port·adapter, persistence 구현 convention |
| [`backend-test-guide.md`](backend-test-guide.md) | backend test 선택, 작성, assertion과 검증 규칙 |
| [`adr/`](adr/README.md) | 구조 결정의 이유, 대안과 예외 |
| [`requirement/`](requirement/index.md) | 목표 behavior와 API contract |
| 각 module의 `package-info.java` | 현재 허용 module dependency와 `@NamedInterface`의 executable contract |
| `build.gradle`, Checkstyle, CI | dependency, toolchain과 실제 quality gate |

구조 원칙을 이 문서에 다시 정의하지 않는다. 구현 중 구조 변경이 필요하면 `architecture.md`, 관련 ADR과 `package-info.java`를 먼저 확인한다.

## 기술 기준과 변경 원칙

- Java toolchain과 Spring dependency version은 `build.gradle`을 기준으로 한다.
- 기존 public API, transaction 의미, error contract와 module seam을 유지한다. 변경이 필요하면 요구사항 또는 승인된 설계 근거가 있어야 한다.
- 관련 symbol의 definition, caller, consumer와 가장 가까운 test를 읽은 뒤 가장 작은 올바른 seam을 수정한다.
- 기능 변경과 무관한 rename, formatting, abstraction 또는 cleanup을 섞지 않는다.
- 새 dependency나 framework pattern을 도입하기 전에 manifest와 repository의 기존 사용례를 확인한다.

## Module과 계층

Business capability별 최상위 package를 Spring Modulith application module로 사용한다.

```text
presentation -> application -> domain <- infra
```

`infra`는 application 또는 domain이 정의한 contract를 구현할 수 있지만, 안쪽 계층은 `infra` 구현을 알지 않는다.

| 계층 | 소유 책임 | 허용되는 의존 | 두지 않는 것 |
|---|---|---|---|
| `presentation` | HTTP binding, 인증 주체 추출, request validation 진입점, status/header/response mapping | `application`, presentation-local type | repository, JPA entity, 업무 상태 전이, `infra` 구현 |
| `application` | use case 조정, transaction boundary, application DTO, module 공개 API, event handler | `domain`, 다른 module의 공개 seam | HTTP type, 다른 module 내부 package, 구체적 `infra` 구현 |
| `domain` | entity, value, invariant, 상태 전이, domain error, repository·outbound port contract | domain 및 허용된 공통 기반 type | controller, Spring Data repository, 외부 SDK, 다른 module entity |
| `infra` | JPA/QueryDSL, 외부 client, security·storage adapter와 framework integration | 구현 대상인 `application`·`domain` contract | 새로운 business policy |

새 class는 책임을 기준으로 배치한다. 기존 package가 혼재되어 있다는 이유만으로 잘못된 방향을 복제하거나, 현재 작업에서 대규모 package migration을 함께 수행하지 않는다.

## Dependency injection과 Spring component

- 필수 dependency는 constructor injection으로 드러내고 field를 `final`로 유지한다. 기존 code와 일치할 때 `@RequiredArgsConstructor`를 사용한다.
- Controller는 request를 application 호출로 변환하고 응답을 구성한다. 업무 분기와 persistence 조정을 넣지 않는다.
- Application service는 use case 순서와 transaction을 소유한다. 단순 CRUD wrapper가 아니라 domain behavior와 port를 조정한다.
- Domain object는 자신의 불변조건과 상태 전이를 소유한다. Service가 entity field를 우회 변경하지 않는다.
- Framework callback·handler도 business behavior가 필요하면 application/domain contract에 위임한다.

## Transaction

- Use case의 transaction boundary는 application service에 둔다. Controller에서 transaction을 열지 않는다.
- 조회 중심 service는 class-level `@Transactional(readOnly = true)`를 사용할 수 있다.
- 쓰기 method는 `@Transactional`로 명시하고, 필요한 read·validation·write를 하나의 일관된 boundary에서 조정한다.
- Repository adapter의 lock 또는 복수 statement transaction은 persistence 원자성을 위한 세부 구현이며 application transaction을 대체하지 않는다.
- Event listener의 실행 시점, publisher transaction 결합, 실패 전파와 retry 의미를 확인한다. Command 성격 event나 새로운 결합은 ADR이 필요하다.

## DTO와 MapStruct

- HTTP 또는 module seam으로 JPA entity를 노출하지 않는다.
- 불변 payload에는 Java `record`를 우선한다.
- HTTP/Jackson concern이 있는 type은 `presentation`에 둔다.
- Transport-neutral command, query와 result는 `application`에 둔다. HTTP concern이 없는 application result를 presentation에서 그대로 사용하는 것은 허용하지만, 이후 HTTP annotation을 application DTO에 추가하지 않는다.
- Domain과 application 사이 mapping은 application mapper, application result와 HTTP response 사이는 presentation mapper가 담당한다.
- MapStruct는 기계적인 변환에만 사용한다. validation, authorization, 상태 전이, merge policy 같은 business rule을 mapper의 expression·hook·`default` method에 새로 넣지 않고 application 또는 domain의 이름 있는 component로 옮긴다.
- `GlobalMapperConfig`와 기존 mapper 구성을 재사용한다. Generated `*MapperImpl` 내부 구조를 production code에서 참조하지 않는다.
- MapStruct test 범위는 [`backend-test-guide.md`](backend-test-guide.md)의 규칙을 따른다.

## Port, adapter와 module seam

- Persistence contract는 module 안쪽의 repository interface로 정의하고 JPA/QueryDSL 구현은 `infra`에 둔다.
- 외부 시스템 contract는 domain/application의 outbound port로 정의하고 SDK·HTTP·storage 구현은 `infra` adapter에 둔다.
- 다른 module을 동기 호출해야 하면 상대 module이 공개한 작은 `@NamedInterface`만 사용한다. Entity, repository, service 구현이나 `infra` package를 직접 참조하지 않는다.
- Cross-module adapter는 호출 module의 port를 구현하고 상대 module의 공개 API를 호출한다.
- 즉시 결과가 필요하지 않은 사실 통지는 immutable event를 우선한다. Event payload는 작고 안정적이어야 하며 entity를 포함하지 않는다.
- 최상위 `package-info.java`의 `allowedDependencies`가 실제 허용 dependency의 기준이다. 구조 문서와 다르면 source를 우선하고 불일치를 보고한다.

## Persistence와 Flyway

- Domain repository contract와 Spring Data/JPA adapter를 분리한다.
- Association은 use case가 명시적으로 함께 읽어야 하는 경우가 아니라면 lazy loading을 기본으로 한다. 필요한 fetch는 repository query contract에서 드러낸다.
- 중요한 invariant는 domain/application validation과 PostgreSQL constraint 중 필요한 계층에 함께 둔다.
- 적용된 Flyway migration은 수정하지 않는다. Schema 변경은 다음 순번의 새 `V<number>__<description>.sql`로 추가한다.
- 기존 data가 있는 non-null column은 nullable 추가, backfill, constraint 강화 순서를 검토한다.
- Foreign key는 같은 owning domain 내부 관계에 사용한다. Module을 넘는 식별자는 공개 port 또는 event contract로 검증한다.
- Query, lock, pagination과 constraint의 test 규칙은 `backend-test-guide.md`를 따른다.

## API, error와 security

- Request DTO에서 형식·필수 값 같은 boundary validation을 수행하고, 업무 가능 여부는 application/domain에서 판정한다.
- Controller는 project의 공통 servlet prefix, authentication annotation과 exception flow를 재사용한다.
- Error code와 외부 message는 기존 exception contract를 유지한다. 내부 detail, stack trace, token, password와 credential은 response에 포함하지 않는다.
- Authorization은 HTTP path만이 아니라 use case가 요구하는 principal·role·resource ownership까지 검증한다.
- 새로운 endpoint는 success뿐 아니라 validation, authorization, not-found와 domain rejection contract를 함께 확인한다.

## Java style와 자동 검증

- Source encoding은 UTF-8이다.
- 인접 code의 two-space indentation, import와 annotation 배치를 유지한다. 요청 범위 밖의 file을 reformat하지 않는다.
- Type은 import해 사용하고 source에 불필요한 fully qualified type을 쓰지 않는다.
- 의미 없는 comment보다 이름과 작은 method로 의도를 표현한다. Comment는 제약의 이유나 외부 contract처럼 code만으로 드러나지 않는 사실에 사용한다.
- 현재 Checkstyle은 일반 formatter가 아니라 project-specific encoding 및 type-name 규칙을 검사한다. Google Java Format이나 별도 formatter가 설정되어 있다고 가정하지 않는다.
- Checkstyle whitelist는 의도적인 문자열만 최소 범위로 등록한다.

## 변경별 확인 기준

| 변경 | 구현 시 추가 확인 |
|---|---|
| module package, 공개 seam, event | `architecture.md`, 양쪽 `package-info.java`, 관련 ADR, Modulith 구조 검증 |
| transaction, lock, consistency | application boundary, repository query/lock, rollback scenario |
| JPA mapping, query, constraint | entity·repository contract, migration, PostgreSQL 기반 repository/integration test |
| REST API, validation, security | controller·DTO·application contract, exception/security 설정과 web/integration test |
| 외부 system 연동 | outbound port, adapter, timeout/error mapping과 test double 경계 |

구체적인 test level, fixture, assertion과 실행 순서는 `backend-test-guide.md`가 소유한다. Gradle 실행 경로와 완료 보고의 강제 규칙은 `AGENTS.md`를 따른다.
