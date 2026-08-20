# Java Testing Guide

Project rules for JUnit, Spring Boot, MockMvc, Testcontainers, and repository queries. The local
`java-junit` skill covers general JUnit usage.

## 1. Core rules

- Place tests under `src/test/java` in the production package structure, with a `Test` suffix.
- Every test class and method must have a Korean `@DisplayName` that states the behavior,
  scenario, and expected result. A reader must understand the feature from the test report alone.
- Keep each test independent and focused on one behavior. New production behavior needs at least
  one success case and one rejection or failure case.
- Prefer AssertJ and `assertThrows`; do not disable tests without a reason and follow-up issue.

## 2. Structure and Mockito

Separate every test into Given, When, and Then. Do not execute production behavior in Given or
configure mocks after When.

```java
// given
given(userRepository.findById(userId)).willReturn(Optional.of(user));

// when
User result = userService.find(userId);

// then
assertThat(result).isSameAs(user);
then(userRepository).should().findById(userId);
```

Use BDDMockito only: `given(...).willReturn(...)`, `willThrow(...)`, `willDoNothing()`, and
`then(...).should()`. Do not use `when(...).thenReturn(...)`; it is ambiguous with the When phase.

## 3. Unit tests

Test one class without Spring for domain rules, services, validators, mappers, handlers, and
adapters. Use `@ExtendWith(MockitoExtension.class)`, `@Mock`, and `@InjectMocks`; do not use
`@SpringBootTest`, a database, HTTP server, or external service. Use `@ParameterizedTest` with
`@ValueSource`, `@CsvSource`, or `@MethodSource` when inputs exercise the same rule.

## 4. Slice tests

| Target | Annotation | Verify |
|---|---|---|
| REST controller | `@WebMvcTest` | binding, validation, status, JSON, delegation |
| JPA repository | `@DataJpaTest` | mapping, query, constraint, transaction |
| JSON | `@JsonTest` | request/response format |
| HTTP client | `@RestClientTest` | serialization and external contract |

Mock only collaborators outside the selected slice. A controller slice uses
`@WebMvcTest(Controller.class)`; disable filters only when security is not the behavior under test.

### Web-layer rules

- Before writing a `MockMvc` URL, check `WebConstants.SERVLET_PREFIX`. `WebMvcConfig` applies it
  to every `@RestController`; currently it is `/api/v1`. Use `RestControllerTest.API_PREFIX` or
  `WebConstants.SERVLET_PREFIX`, never a bare controller path.
- For `@RequestBody`, verify that decoded JSON values reach the service. Verify the DTO directly
  when it has value equality; otherwise capture it and assert every relevant field. `any(...)`
  alone is insufficient.
- Verify a valid response and an invalid or rejected response. For validation errors, assert the
  field and code; for service failures, assert the exception-handler status and error code.

```java
@Test
@DisplayName("사용자 생성: 요청 값이 서비스에 그대로 전달되고 201을 반환한다")
void create_passesRequestToService() throws Exception {
  UserCreateRequest request = validRequest();

  mockMvc.perform(post(API_PREFIX + "/users")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isCreated());

  then(userService).should().create(request);
}
```

## 5. Integration tests

Use an integration test only when collaboration is the behavior: migrations, persistence,
transactions, security, module seams, or an end-to-end flow.

- Extend `BaseIntegrationTest` for HTTP-to-database integration tests. It provides the test
  profile, PostgreSQL Testcontainers, Flyway, rollback support, JPA helpers, MockMvc, and
  ObjectMapper.
- Use real internal beans and PostgreSQL; mock only an external boundary outside the scenario. Do
  not replace PostgreSQL with H2 or create the schema manually.
- Isolate fixtures and assert an observable HTTP response, persisted state, event, rollback, or
  security decision. A context-load test is not a feature scenario.
- Cover success and failure/rollback paths. For module structure, retain
  `ApplicationModules.of(Amaazon.class).verify()`; use `@ApplicationModuleTest` only for an
  independently bootstrappable module.

## 6. Repository tests

Repository tests verify the JPA Repository contract, not a specific database. The test database and
dialect are infrastructure details, so test scenarios must remain database-independent.

- The test target is the JPA adapter under `infra/repository` (for example, `UserJpaRepository`),
  while `domain/repository` defines the contract.
- Every `DomainRepository` implementation must test `save`, `findById`, and `delete`; test `saveAll`
  when bulk persistence is used or part of the contract.
- `save(...)` tests must consider the entity's validation and persistence constraints. Cover both a
  valid entity and invalid cases such as null, length, format, enum, or relationship violations;
  flush the persistence context so the rejection is actually observed.
- Test declared derived, `@Query`, locking, fetch-graph, pagination/scroll, and specification
  queries. For page- or cursor-based queries, make at least two requests with different pages or
  cursors in the same test and verify that each result is different and does not overlap the others;
  check the query count for each request separately.
- Assert persisted/query results, and verify the expected query count for every Repository operation
  that is executed. This includes expected zero-query validation failures. For a fresh entity read,
  traverse and assert every entity field and association before checking the count; a `LAZY`
  association may produce an additional query. Use an explicit fetch query when the association
  must be loaded together with the entity. Do not assert vendor-specific SQL, dialect, schema,
  column types, or native-query syntax.
- Keep database-specific native-query tests separate. For logical deletion such as `@SQLDelete`,
  verify the persisted disabled/archived state.

Place the test beside the adapter under `src/test/.../infra/repository`, name it after the adapter
(for example, `UserJpaRepositoryTest`), and extend `BaseRepositoryTest`. Do not repeat its slice
annotations or test configuration. `BaseRepositoryTest` may change its test database
implementation without changing the repository test scenarios.

```java
@DisplayName("카테고리 저장소")
class CategoryJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CategoryJpaRepository repository;

  @Test
  @DisplayName("전체 조회: 부모와 자식을 한 번의 쿼리로 조회한다")
  void findAll_fetchesHierarchyInOneQuery() {
    Category parent = persistAndFlush(Category.of("전자기기", null));
    persistAndFlush(Category.of("노트북", parent));
    clear();

    List<Category> categories = repository.findAll();
    categories.forEach(Category::getChildren);

    ensureQueryCount(1);
  }
}
```

Create fixtures with `persistAndFlush`, call `clear()` before the Repository method, and assert the
result from a fresh JPA query. Fetch behavior must be checked on the freshly queried entity, not on
an entity already managed by the persistence context. `BaseRepositoryTest` also calls `clear()` before
each test as a safety net. `clear()` also resets `QueryInspector`; call
`ensureQueryCount(expectedCount)` immediately after the relevant fields and associations have been
traversed. It does not need to be the final assertion in the test; later assertions must not trigger
additional persistence operations. Query-inspector logs are diagnostic only.

## 7. Choose the smallest test

| Behavior | Test type |
|---|---|
| Pure rule, service, mapper, validator | Unit |
| One Spring adapter or web layer | Slice |
| Database, transaction, module, security, external collaboration | Integration |

Before merging, confirm the DisplayName, Given-When-Then separation, success and failure paths,
deterministic fixtures, correct test scope, and—when applicable—request delegation or exact
repository query count.
