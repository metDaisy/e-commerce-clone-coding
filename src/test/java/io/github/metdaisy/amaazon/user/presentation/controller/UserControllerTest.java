package io.github.metdaisy.amaazon.user.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.support.RestControllerTest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("사용자 컨트롤러 슬라이스 테스트")
class UserControllerTest extends RestControllerTest {

  private static final String PROFILE_URL = API_PREFIX + "/me";

  @MockitoBean
  private UserService userService;

  @Test
  @DisplayName("프로필 조회 성공: 인증 주체의 User 프로필을 반환한다")
  void getMe_success() throws Exception {
    // given
    Instant createdAt = Instant.parse("2026-08-16T12:00:00Z");
    Instant updatedAt = Instant.parse("2026-08-16T12:30:00Z");
    UserResponse profile = new UserResponse(
        USER_ID,
        "tester",
        "01012345678",
        List.of(UserRole.USER.name(), UserRole.PRODUCT_MANAGER.name()),
        true,
        createdAt,
        updatedAt);
    given(userService.findProfile(USER_ID)).willReturn(profile);

    // when
    ResultActions result = mockMvc.perform(get(PROFILE_URL));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.name").value("tester"))
        .andExpect(jsonPath("$.phoneNumber").value("01012345678"))
        .andExpect(jsonPath("$.loginEmail").doesNotExist())
        .andExpect(jsonPath("$.roles").isArray())
        .andExpect(jsonPath("$.roles[0]").value("USER"))
        .andExpect(jsonPath("$.roles[1]").value("PRODUCT_MANAGER"))
        .andExpect(jsonPath("$.isEnabled").value(true))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
        .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

    then(userService).should().findProfile(USER_ID);
  }

  @Test
  @DisplayName("프로필 조회 실패: User가 존재하지 않으면 404 오류를 반환한다")
  void getMe_failure_whenUserNotFound() throws Exception {
    // given
    given(userService.findProfile(USER_ID))
        .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

    // when
    ResultActions result = mockMvc.perform(get(PROFILE_URL));

    // then
    result
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("USER-001"));
  }

  @Test
  @DisplayName("사용자 정보 수정 성공: 인증 주체 ID와 수정 요청을 서비스에 전달한다")
  void update_success() throws Exception {
    // given
    UserUpdateRequest request = new UserUpdateRequest("updated", "01098765432");
    given(userService.update(any(UUID.class), any(UserUpdateRequest.class)))
        .willReturn(profileResponse());

    // when
    mockMvc.perform(patch(PROFILE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // then
    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UserUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserUpdateRequest.class);
    then(userService).should().update(userIdCaptor.capture(), requestCaptor.capture());
    assertThat(userIdCaptor.getValue()).isEqualTo(USER_ID);
    assertThat(requestCaptor.getValue()).isEqualTo(request);
  }

  @ParameterizedTest(name = "[{index}] 이름={0}")
  @MethodSource("validUpdateNames")
  @DisplayName("사용자 정보 수정 성공: 정규식에 맞는 이름과 11자리 전화번호를 허용한다")
  void update_success_whenRequestMatchesPatterns(String name) throws Exception {
    // given
    UserUpdateRequest request = new UserUpdateRequest(name, "01012345678");
    given(userService.update(any(UUID.class), any(UserUpdateRequest.class)))
        .willReturn(profileResponse());

    // when
    mockMvc.perform(patch(PROFILE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // then
    then(userService).should().update(USER_ID, request);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidUpdateRequests")
  @DisplayName("사용자 정보 수정 실패: 유효하지 않은 요청이면 서비스 호출 없이 400 오류를 반환한다")
  void update_failure_whenRequestIsInvalid(
      String caseName, UserUpdateRequest request, String field, String expectedMessage)
      throws Exception {
    // when
    mockMvc.perform(patch(PROFILE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details." + field, hasItem(expectedMessage)));

    // then
    then(userService).should(never()).update(any(UUID.class), any(UserUpdateRequest.class));
  }

  @Test
  @DisplayName("계정 비활성화 성공: 인증 주체의 User를 비활성화하고 204를 반환한다")
  void deactivate_success() throws Exception {
    // when
    mockMvc.perform(post(PROFILE_URL + "/deactivate"))
        .andExpect(status().isNoContent());

    // then
    then(userService).should().deactivate(USER_ID);
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of("이름 빈 문자열",
            new UserUpdateRequest("", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("이름 길이 초과",
            new UserUpdateRequest("nameistoolong", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("이름에 숫자 포함",
            new UserUpdateRequest("user123", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("이름에 공백 포함",
            new UserUpdateRequest("user name", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("이름에 특수문자 포함",
            new UserUpdateRequest("user!", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("전화번호 형식 오류",
            new UserUpdateRequest(null, "010-1234-5678"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."),
        Arguments.of("전화번호 길이 부족",
            new UserUpdateRequest(null, "0101234567"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."),
        Arguments.of("전화번호 길이 초과",
            new UserUpdateRequest(null, "010123456789"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."),
        Arguments.of("전화번호에 문자 포함",
            new UserUpdateRequest(null, "0101234567a"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."));
  }

  private static Stream<Arguments> validUpdateNames() {
    return Stream.of(
        Arguments.of("a"),
        Arguments.of("가"),
        Arguments.of("abcdefghij"),
        Arguments.of("가나다라마바사아차카"));
  }

  private static UserResponse profileResponse() {
    Instant now = Instant.parse("2026-08-16T12:00:00Z");
    return new UserResponse(
        USER_ID, "tester", "01012345678", List.of(UserRole.USER.name()), true, now, now);
  }
}
