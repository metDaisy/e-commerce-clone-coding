# Java Testing Guide

This guide defines how to write unit, slice, and integration tests in this project. It supplements the local `java-junit` skill with project-specific rules for Spring Boot, Mockito, MockMvc, Spring Modulith, and Flyway.

## 1. Test principles

- Put tests under `src/test/java` and keep the package structure aligned with production code.
- Use a `Test` suffix: `UserServiceTest`, `UserControllerTest`.
- Name tests after observable behavior, for example `create_shouldRejectDuplicatePhoneNumber`.
- Each test must be independent, repeatable, and focused on one behavior.
- Every production behavior that is introduced must have at least one successful case and one failure or rejection case.
- Prefer AssertJ for readable assertions and `assertThrows` when an exception is the behavior under test.
- Do not disable a test without a concrete reason and a follow-up issue.

## 2. Given-When-Then

Every test should be structured as:

```java
// given: inputs, mocks, and preconditions
// when: invoke the single behavior under test
// then: assert the result and verify important interactions
```

Keep the three sections visually separate. Do not put production behavior in the `given` section, and do not configure mocks after the action has already been executed.

### Mockito stubbing rule

Use BDDMockito for stubbing:

```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

given(userRepository.findById(userId)).willReturn(Optional.of(user));
given(userRepository.existsByPhoneNumber(phone)).willReturn(false);
```

Do not use `when(...).thenReturn(...)` for stubbing in this project. The word `when` is easily confused with the When phase of Given-When-Then. Use `given(...).willReturn(...)`, `willThrow(...)`, or `willDoNothing()` instead.

Use `then(...).should()` for interaction verification:

```java
then(userRepository).should().save(any(User.class));
then(userRepository).should(never()).deleteById(any());
```

## 3. Unit tests

Unit tests execute one class without starting Spring. Use them for domain rules, application services, validators, mappers, handlers, and adapters with mocked collaborators.

### Setup

Use `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks`:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;
}
```

Do not use `@SpringBootTest` for a unit test. A unit test should not require a database, HTTP server, application context, or external service.

### Successful case

```java
@Test
void create_shouldSaveUser_whenPhoneNumberIsAvailable() {
  // given
  FormSignUpTask task = new FormSignUpTask(userId, "Alice", phone, address);
  User savedUser = mock(User.class);
  given(userRepository.existsByPhoneNumber(phone)).willReturn(false);
  given(userRepository.save(any(User.class))).willReturn(savedUser);

  // when
  User result = userService.create(task);

  // then
  assertThat(result).isSameAs(savedUser);
  then(userRepository).should().existsByPhoneNumber(phone);
  then(userRepository).should().save(any(User.class));
}
```

### Failure case

```java
@Test
void create_shouldRejectDuplicatePhoneNumber() {
  // given
  FormSignUpTask task = new FormSignUpTask(userId, "Alice", phone, address);
  given(userRepository.existsByPhoneNumber(phone)).willReturn(true);

  // when / then
  assertThatThrownBy(() -> userService.create(task))
      .isInstanceOf(UserException.class)
      .hasMessageContaining("phone");

  then(userRepository).should(never()).save(any(User.class));
}
```

If multiple inputs exercise the same rule, use `@ParameterizedTest` instead of copying nearly identical methods:

```java
@ParameterizedTest(name = "invalid password: {0}")
@ValueSource(strings = {"short", "onlyletters", "12345678"})
void validate_shouldRejectInvalidPassword(String password) {
  assertThat(passwordValidator.isValid(password, context)).isFalse();
}
```

Use `@MethodSource` when a case needs multiple named values or expected results. Use `@CsvSource` for short tabular cases.

## 4. Slice tests

Slice tests load only one Spring layer. They are useful when framework behavior matters but a full application is unnecessary.

Common choices:

| Test target | Annotation | Scope |
|---|---|---|
| REST controller | `@WebMvcTest` | MVC binding, validation, status, JSON, controller delegation |
| JPA repository | `@DataJpaTest` | Entity mapping, queries, constraints, transactions |
| JSON serialization | `@JsonTest` | Jackson request/response format |
| HTTP client | `@RestClientTest` | Client serialization and external HTTP contract |

Mock only collaborators outside the selected slice. A slice test must still assert at least one successful response and one error response, such as validation failure, not-found, or service exception mapping.

### REST controller test

Use `@WebMvcTest(Controller.class)`. Disable security filters only when authentication is not the behavior under test:

```java
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;
}
```

### Mandatory `@RequestBody` delegation check

When a controller accepts `@RequestBody`, verifying only the HTTP status is insufficient. The test must prove that the values decoded from JSON are exactly the values passed to the service.

Prefer an exact DTO verification when the request DTO has value equality:

```java
@Test
void create_shouldPassRequestBodyValuesToService() throws Exception {
  // given
  UserCreateRequest expected = new UserCreateRequest(
      "Alice", "alice@example.com", "010-1234-5678", "Seoul", "Password1!");

  // when
  mockMvc.perform(post("/api/v1/auth/signup")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(expected)))
      .andExpect(status().isCreated());

  // then
  then(userService).should().create(expected);
}
```

If the DTO does not implement value equality, capture and assert every relevant field:

```java
ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);
then(userService).should().create(captor.capture());

UserCreateRequest actual = captor.getValue();
assertThat(actual.name()).isEqualTo("Alice");
assertThat(actual.email()).isEqualTo("alice@example.com");
assertThat(actual.phoneNumber()).isEqualTo("010-1234-5678");
assertThat(actual.address()).isEqualTo("Seoul");
assertThat(actual.password()).isEqualTo("Password1!");
```

This check is required for every controller test whose request contains a body. Do not use only `any(UserCreateRequest.class)`, because that would allow a controller to silently drop or change request values.

### REST success and error cases

At minimum, cover:

1. A valid request returning the required success status and response body.
2. An invalid or rejected request returning the required error status and error code.

For validation errors, assert the field and error code. For service failures, stub with BDDMockito and assert the controller's exception handler response:

```java
// given
UUID userId = UUID.randomUUID();
given(userService.find(userId)).willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

// when / then
mockMvc.perform(get("/api/v1/me"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
```

## 5. Integration tests

Integration tests load multiple real application components and verify their collaboration. Use them for database migrations, persistence behavior, module seams, transactions, security configuration, and end-to-end application flows.

Use `@SpringBootTest` for the application context. Use the project's Testcontainers configuration when PostgreSQL or another external service is required:

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserSignupIntegrationTest {
  // real Spring beans, real database, and Flyway migrations
}
```

Integration tests must:

- run Flyway migrations rather than manually executing `V1__...sql`;
- verify that the required schema and constraints are available;
- avoid mocking the internal service or repository being integrated;
- isolate data with transactions, cleanup, or unique test fixtures;
- cover at least one successful flow and one failure or rollback flow;
- verify observable boundaries such as HTTP response, persisted state, emitted event, or transaction outcome.

For Spring Modulith, use `ApplicationModules.of(Amaazon.class).verify()` to validate module dependencies. Cross-domain dependencies must be declared in the domain's top-level `package-info.java`; this does not permit cyclic module dependencies.

## 6. Choosing the test type

```text
Pure rule or service behavior       -> Unit test
One Spring adapter or web layer      -> Slice test
Database/module/transaction flow    -> Integration test
```

Prefer the smallest test that proves the behavior. Add a broader test only when the framework, persistence, module boundary, or external integration is part of the behavior.

## 7. Review checklist

- Does the test name describe one behavior and one scenario?
- Are `given`, `when`, and `then` clearly separated?
- Is every stub written with `BDDMockito.given(...).will...`?
- Is there at least one success and one failure case?
- Should repeated cases use `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, or `@MethodSource`?
- For a REST `@RequestBody`, are all service arguments verified against the request JSON values?
- Are important interactions verified without over-specifying implementation details?
- Is the test independent, deterministic, and free of unnecessary Spring context loading?
