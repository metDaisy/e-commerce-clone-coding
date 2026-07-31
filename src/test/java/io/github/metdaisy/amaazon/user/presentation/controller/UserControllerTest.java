package io.github.metdaisy.amaazon.user.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionResponseStrategy;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.presentation.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.presentation.mapper.UserMapper;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("사용자 컨트롤러 슬라이스 테스트")
class UserControllerTest {

  private static final String USERS_URL = "/api/users";
  private static final UUID USER_ID = UUID.fromString("2bb8df7f-9478-4d51-b055-496016dd421f");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private ExceptionStrategyFactory exceptionStrategyFactory;

  @BeforeEach
  void setUp() {
    AmaazonPrincipal principal = mock(AmaazonPrincipal.class);
    given(principal.getId()).willReturn(USER_ID);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null));

    @SuppressWarnings("unchecked")
    ExceptionResponseStrategy<MethodArgumentNotValidException> validationStrategy =
        mock(ExceptionResponseStrategy.class);
    given(exceptionStrategyFactory.getStrategy(MethodArgumentNotValidException.class))
        .willReturn(validationStrategy);
    given(validationStrategy.buildResponse(any(MethodArgumentNotValidException.class)))
        .willReturn(ResponseEntity.badRequest().build());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("회원가입 성공: 검증된 생성 요청을 서비스에 그대로 전달한다")
  void create_success() throws Exception {
    // given
    UserCreateRequest request = validCreateRequest();
    User user = User.createUser(request.name(), request.phoneNumber(), request.address());
    given(userService.create(any(UserCreateRequest.class))).willReturn(user);
    given(userMapper.toDto(user)).willReturn(new UserResponse());

    // when
    mockMvc.perform(post(USERS_URL + "/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // then
    ArgumentCaptor<UserCreateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserCreateRequest.class);
    verify(userService).create(requestCaptor.capture());
    assertThat(requestCaptor.getValue()).isEqualTo(request);
    verify(userMapper).toDto(user);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidCreateRequests")
  @DisplayName("회원가입 실패: 유효하지 않은 생성 요청이면 서비스 호출을 차단한다")
  void create_failure_whenRequestIsInvalid(String caseName, UserCreateRequest request)
      throws Exception {
    // when & then
    mockMvc.perform(post(USERS_URL + "/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).create(any(UserCreateRequest.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("회원정보 수정 성공: 인증 사용자 ID와 수정 요청을 서비스에 그대로 전달한다")
  void update_success() throws Exception {
    // given
    UserUpdateRequest request = new UserUpdateRequest(
        "updated", "updated@example.com", "Password1!", "01098765432", "Busan");
    User user = User.createUser(request.name(), request.phoneNumber(), request.address());
    given(userService.update(any(UUID.class), any(UserUpdateRequest.class))).willReturn(user);
    given(userMapper.toDto(user)).willReturn(new UserResponse());

    // when
    mockMvc.perform(post(USERS_URL + "/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // then
    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UserUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserUpdateRequest.class);
    verify(userService).update(userIdCaptor.capture(), requestCaptor.capture());
    assertThat(userIdCaptor.getValue()).isEqualTo(USER_ID);
    assertThat(requestCaptor.getValue()).isEqualTo(request);
    verify(userMapper).toDto(user);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidUpdateRequests")
  @DisplayName("회원정보 수정 실패: 유효하지 않은 수정 요청이면 서비스 호출을 차단한다")
  void update_failure_whenRequestIsInvalid(String caseName, UserUpdateRequest request)
      throws Exception {
    // when & then
    mockMvc.perform(post(USERS_URL + "/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).update(any(UUID.class), any(UserUpdateRequest.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  private static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of("이름 누락", new UserCreateRequest(
            "", "user@example.com", "Password1!", "01012345678", "Seoul")),
        Arguments.of("이름 길이 초과", new UserCreateRequest(
            "nameistoolong", "user@example.com", "Password1!", "01012345678", "Seoul")),
        Arguments.of("이메일 형식 오류", new UserCreateRequest(
            "tester", "invalid-email", "Password1!", "01012345678", "Seoul")),
        Arguments.of("비밀번호 길이 부족", new UserCreateRequest(
            "tester", "user@example.com", "Pass1!", "01012345678", "Seoul")),
        Arguments.of("비밀번호 조합 부족", new UserCreateRequest(
            "tester", "user@example.com", "abcdefgh", "01012345678", "Seoul")),
        Arguments.of("전화번호 형식 오류", new UserCreateRequest(
            "tester", "user@example.com", "Password1!", "010-1234-5678", "Seoul")));
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of("이름 길이 초과", new UserUpdateRequest(
            "nameistoolong", null, null, null, null)),
        Arguments.of("이메일 형식 오류", new UserUpdateRequest(
            null, "invalid-email", null, null, null)),
        Arguments.of("비밀번호 길이 부족", new UserUpdateRequest(
            null, null, "Pass1!", null, null)),
        Arguments.of("비밀번호 조합 부족", new UserUpdateRequest(
            null, null, "abcdefgh", null, null)),
        Arguments.of("전화번호 형식 오류", new UserUpdateRequest(
            null, null, null, "010-1234-5678", null)));
  }

  private static UserCreateRequest validCreateRequest() {
    return new UserCreateRequest(
        "tester", "user@example.com", "Password1!", "01012345678", "Seoul");
  }
}
