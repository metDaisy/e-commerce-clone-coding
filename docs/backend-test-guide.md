# Backend Test Guide

## 목적과 기준

이 문서는 backend의 JUnit 5, Spring Boot Test, MockMvc, Spring Modulith Test, Testcontainers와 repository test convention을 정의한다. Test dependency와 task의 실제 구성은 `build.gradle`이 기준이며, 구현 구조는 [`backend-development-guide.md`](backend-development-guide.md)를 따른다.

테스트는 내부 구현이 아니라 public contract 또는 관찰 가능한 behavior를 증명한다. Private method, generated code의 호출 순서나 field 배치에 결합하지 않는다.

## 1. 공통 규칙

- Test는 `src/test/java`의 production package 구조에 두고 class 이름에 `Test` suffix를 사용한다.
- 모든 test class와 method에 behavior, scenario와 expected result를 설명하는 한국어 `@DisplayName`을 작성한다.
- 한 test는 하나의 behavior를 검증하고 다른 test의 실행 순서나 변경 가능한 공유 state에 의존하지 않는다.
- Contract에 rejection, failure 또는 boundary가 있으면 success와 함께 검증한다. 의미 있는 negative path가 없는 behavior에 형식적인 failure test를 만들지 않는다.
- AssertJ와 `assertThrows`를 우선하고 결과, 상태, 예외 code와 필요한 collaboration을 구체적으로 검증한다.
- 구현 완료를 위해 test를 삭제·약화하거나 `@Disabled`로 전환하지 않는다. Disabled test는 검증 evidence가 아니다.
- 일반 test에 `@TestMethodOrder`와 `@Order`를 사용하지 않는다. 순서가 필요한 업무 흐름은 하나의 독립 scenario 안에서 검증하거나 test마다 state를 다시 만든다.

## 2. Behavior별 RED–GREEN loop

1. 아직 검증되지 않은 관찰 가능한 behavior 하나를 선택하고 가장 작은 적절한 test level을 정한다.
2. Production code보다 먼저 해당 behavior를 재현하는 test를 작성한다.
3. 그 test만 `gradle-mcp`로 실행하여 compilation·fixture·환경 문제가 아니라 예상한 behavior gap으로 실패하는지 확인한다.
4. Behavior를 통과시키는 최소 production path를 구현하고 같은 test를 green으로 만든다.
5. 다음 behavior에 대해 RED–GREEN cycle을 반복한다. 모든 test를 먼저 쌓은 뒤 production code를 한꺼번에 작성하지 않는다.
6. 가장 가까운 regression test, card의 focused verification, 필요한 전체 backend verification 순서로 범위를 넓힌다.
7. Refactor는 green 상태에서만 수행하고 affected test를 다시 실행한다.

의미 있는 red test를 만들 수 없는 migration·configuration 등의 변경은 이유와 대체할 결정론적 verification을 명시한다.

## 3. Test 구조와 Mockito

Given, When, Then을 논리적으로 분리한다. Production behavior는 Given에서 실행하지 않고, mock은 When 이후에 설정하지 않는다. Exception assertion처럼 호출과 assertion이 하나의 표현식인 경우 `when & then`을 함께 사용할 수 있다.

```java
// given
given(userRepository.findById(userId)).willReturn(Optional.of(user));

// when
User result = userService.find(userId);

// then
assertThat(result).isSameAs(user);
then(userRepository).should().findById(userId);
```

- Stubbing과 일반 collaboration 검증에는 BDDMockito의 `given`, `willThrow`, `willDoNothing`, `then(...).should()`를 사용한다.
- `any(...)`만으로 중요한 request 전달을 통과시키지 않는다. Value equality가 없으면 captor로 관련 field를 모두 확인한다.
- Mock은 선택한 test subject 바깥의 collaborator에만 사용한다. Subject의 핵심 behavior를 mock으로 대체하지 않는다.
- 공통 fixture는 behavior를 숨기지 않는 작은 factory 또는 기존 support class로 재사용한다.

## 4. Parameterized test

같은 behavior와 assertion shape를 여러 입력으로 검증할 때만 parameterize한다. Success와 rejection처럼 의미가 다른 branch는 별도 test로 나눈다.

| 입력 | Source |
|---|---|
| 단일 String, number, boolean 또는 class | `@ValueSource` |
| Enum 전체·부분 집합 | `@EnumSource` |
| null·empty 경계 | `@NullSource`, `@EmptySource`, `@NullAndEmptySource` |
| 의미가 명확한 작은 scalar tuple | `@CsvSource` |
| DTO, entity, nested object 또는 복잡한 expected result | `@MethodSource` |

Parameterized test에는 `{index}`와 의미 있는 case label을 포함한 한국어 `name`을 지정한다. 값만으로 의미가 불명확한 `@MethodSource`는 첫 argument로 case 이름을 제공한다.

## 5. MapStruct 제외 범위

MapStruct가 생성한 assignment 자체는 독립적인 test 대상에서 제외한다.

- Generated `*MapperImpl`만을 검증하는 전용 `*MapperTest`를 작성하지 않는다.
- Service unit test에서 mapping이 관찰 가능한 service result 또는 entity update의 일부라면 실제 generated mapper를 collaborator로 사용할 수 있다. Assertion은 service behavior에 두고 generated method 내부나 호출 순서를 검증하지 않는다.
- MVC slice에서는 presentation mapper가 선택한 web slice 밖의 collaborator이면 mock하고 명시적인 변환 결과를 stub할 수 있다.
- Integration/module test는 Spring이 구성한 실제 mapper bean을 사용할 수 있지만 mapping 자체를 별도 acceptance로 세지 않는다.
- Validation, authorization, 상태 전이, normalization 또는 merge policy를 MapStruct hook·expression·`default` method에 새로 넣지 않는다. 그런 logic은 application/domain component로 옮기고 그 behavior를 해당 계층에서 test한다.

JaCoCo 대상 제외와 generated class 설정은 `build.gradle`이 소유한다. Test guide는 coverage 설정을 중복 정의하지 않는다.

## 6. Unit test

Domain rule, service, validator, handler와 adapter의 독립 behavior는 Spring 없이 한 class를 test한다.

- 필요한 경우 `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@Spy`, `@InjectMocks`를 사용한다.
- `@SpringBootTest`, database, HTTP server 또는 외부 service를 시작하지 않는다.
- Domain test는 상태 전이, invariant와 error code를 검증한다.
- Application service test는 use case 결과, repository/port collaboration, transaction 안에서 요구되는 decision을 검증한다.
- Adapter unit test는 외부 SDK/HTTP 경계의 request 변환과 error mapping을 검증한다.

## 7. Slice test

| 대상 | Annotation 또는 base | 검증 |
|---|---|---|
| REST controller | `@WebMvcTest`, 기존 `RestControllerTest` | binding, validation, status, JSON, delegation, error response |
| JPA repository | `@DataJpaTest`, 기존 `BaseRepositoryTest` | mapping, query, constraint, lock, transaction |
| JSON | `@JsonTest` | request/response serialization contract |
| HTTP client | `@RestClientTest` | serialization과 외부 contract |

선택한 slice 밖의 collaborator만 mock한다. Security가 behavior가 아닐 때만 filter 제외를 허용하며, 권한이 contract에 포함되면 실제 security 설정 또는 전용 security test를 사용한다.

### Web layer

- `MockMvc` URL을 작성하기 전에 `WebConstants.SERVLET_PREFIX`를 확인하고 `RestControllerTest.API_PREFIX` 또는 해당 상수를 사용한다.
- `@RequestBody`의 decoded 값이 service로 전달됐는지 검증한다.
- Valid response와 contract에 정의된 invalid/rejected response를 검증한다.
- Validation failure는 field와 code를, service failure는 exception-handler status와 error code를 확인한다.
- HTTP response만 맞고 application delegation이 틀린 test를 허용하지 않는다.

## 8. Integration test

Migration, persistence, transaction, security, cache, module seam 또는 HTTP-to-database collaboration 자체가 behavior일 때만 integration test를 사용한다.

- HTTP-to-database test는 `BaseIntegrationTest`를 재사용한다. Test profile, PostgreSQL Testcontainers, Flyway, rollback, JPA helper, MockMvc와 ObjectMapper 설정을 중복하지 않는다.
- Internal bean과 PostgreSQL은 실제 구현을 사용하고 scenario 밖의 외부 boundary만 mock한다. H2로 바꾸거나 schema를 수동 생성하지 않는다.
- Observable HTTP response, persisted state, event, rollback, cache 또는 security decision을 assertion한다.
- 단순 context-load test는 infrastructure smoke test일 뿐 feature acceptance를 대신하지 않는다.
- Success와 contract에 존재하는 failure/rollback path를 함께 검증한다.

## 9. Repository test

Repository test는 특정 구현 class가 아니라 domain repository contract와 PostgreSQL에서의 adapter behavior를 검증한다.

- Test 대상은 `infra/repository`의 JPA adapter이고 contract는 `domain/repository`가 소유한다.
- 각 repository contract의 실제 사용 operation을 검증한다. 공통 `save`, `findById`, `delete`와 사용되는 bulk/query operation을 포함한다.
- `save`는 valid entity와 실제 contract에 존재하는 null, length, format, enum, uniqueness 또는 relationship constraint를 검증하고 rejection 관찰을 위해 flush한다.
- Derived query, `@Query`, QueryDSL, lock, fetch graph, pagination/scroll과 specification을 실제 사용 범위대로 검증한다.
- Page/cursor query는 서로 다른 두 요청의 결과가 다르고 겹치지 않는지 확인한다.
- Fresh query behavior는 fixture를 `persistAndFlush`한 뒤 `clear()`하고 새로 조회한 entity에서 검증한다.
- 필요한 field와 association을 순회한 뒤 각 repository operation의 query count를 바로 확인한다. Validation이 DB 접근 전에 실패해야 하면 zero-query도 검증한다.
- Vendor-specific SQL, dialect, schema 또는 column type을 일반 repository contract test에 넣지 않는다. Native query test는 별도로 둔다.
- Logical deletion은 persisted disabled/archived state를 확인한다.

`BaseRepositoryTest`의 annotation과 configuration을 test class에서 반복하지 않는다. `clear()`는 persistence context와 `QueryInspector`를 reset하므로 query-count assertion 이후 추가 persistence operation을 유발하지 않도록 한다.

## 10. Spring Modulith와 구조 test

- `ApplicationModules.of(Amaazon.class).verify()`는 module cycle, `allowedDependencies`와 공개 seam 위반을 검증한다.
- ArchUnit layer 검증은 `presentation -> application -> domain <- infra` 방향을 확인한다. 현재
  `ModularityTest`에 열거된 module만 검증하므로 새 module·계층 package를 추가할 때 대상 목록도 갱신한다.
- `package-info.java`, `@NamedInterface`, cross-module import, event 또는 layer package가 바뀌면 `ModularityTest`를 실행한다.
- `@ApplicationModuleTest`는 선택한 module이 독립적으로 bootstrap되고 공개 API·event collaboration을 검증할 필요가 있을 때만 사용한다.
- 구조 검증은 behavior test를, module context load는 unit/slice/integration scenario를 대체하지 않는다.

## 11. Test level 선택과 검증

| 변경 behavior | 가장 작은 test | 추가 검증 |
|---|---|---|
| Pure rule, validator, domain state | Unit | success와 contract의 rejection/boundary |
| Application service/use case | Unit | repository·port collaboration |
| Controller/JSON/validation | Web/JSON slice | delegation, error/security response |
| JPA query/constraint/lock | Repository slice | fresh entity, query count, migration 영향 |
| Transaction/security/cache/external collaboration | Integration | rollback, event, 권한 또는 cache 경계 |
| Module seam/layer/event contract | `ModularityTest` | 관련 module/integration behavior |
| Flyway/schema | PostgreSQL integration | migration과 기존 data compatibility |

모든 Gradle 작업은 `gradle-mcp`로 실행한다. Terminal, shell 또는 IDE에서 `gradle`, `gradlew`, `gradlew.bat`을 실행하지 않는다. `gradle-mcp`가 없으면 우회하지 않고 Gradle-dependent verification을 block하고 원인과 다음 조치를 보고한다.

완료 전에는 모든 한국어 `@DisplayName`, logical Given–When–Then, 독립 fixture, 적절한 test scope, success/negative contract, request delegation, persistence state와 필요한 query count를 확인한다. 실행하지 않았거나 실패한 verification을 통과했다고 보고하지 않는다.
