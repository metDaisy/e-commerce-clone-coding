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

- Extend `BaseIntegrationTest` for application/database tests; extend `BaseWebIntegrationTest` for
  HTTP tests. They provide the test profile, PostgreSQL Testcontainers, Flyway, rollback support,
  and the relevant JPA or MockMvc helpers.
- Use real internal beans and PostgreSQL; mock only an external boundary outside the scenario. Do
  not replace PostgreSQL with H2 or create the schema manually.
- Isolate fixtures and assert an observable HTTP response, persisted state, event, rollback, or
  security decision. A context-load test is not a feature scenario.
- Cover success and failure/rollback paths. For module structure, retain
  `ApplicationModules.of(Amaazon.class).verify()`; use `@ApplicationModuleTest` only for an
  independently bootstrappable module.

## 6. Repository tests

Repository tests run against PostgreSQL and prove queries declared in `domain/repository`.

- Do not test inherited `DomainRepository` operations: `save`, `saveAll`, `delete`, `findById`,
  `existsById`, and `getReferenceById`.
- Test each declared derived, `@Query`, locking, fetch-graph, pagination/scroll, or
  specification-backed query.
- Test inherited `delete` only for an entity with logical deletion such as `@SQLDelete`. Verify
  the stored archival/disabled state; `@SQLDelete` does not filter later reads automatically.

Every repository test extends `BaseRepositoryTest`; do not repeat its slice annotations or test
configuration.

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

Use inherited `em` only to create fixtures (`persistAndFlush`), force writes (`em.flush()`), clear
the persistence context, or inspect a logical-delete row with `em.createNativeQuery(...)`. Do not
assert a fetch query from already-managed entities.

`clear()` clears both `em` and `QueryInspector`: persist fixtures, call `clear()` immediately
before one repository method, access each association whose fetch behavior is under test, then
call `ensureQueryCount(expectedCount)` before any further SQL. The count must match exactly.
`queryInspector.getQueries()` and `logQueries()` diagnose a failed count; they do not replace it.

## 7. Choose the smallest test

| Behavior | Test type |
|---|---|
| Pure rule, service, mapper, validator | Unit |
| One Spring adapter or web layer | Slice |
| Database, transaction, module, security, external collaboration | Integration |

Before merging, confirm the DisplayName, Given-When-Then separation, success and failure paths,
deterministic fixtures, correct test scope, and—when applicable—request delegation or exact
repository query count.
