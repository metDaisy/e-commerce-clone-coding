package io.github.metdaisy.amaazon.user.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.support.RestControllerTest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("사용자 컨트롤러 슬라이스 테스트")
class UserControllerTest extends RestControllerTest {

  private static final String USERS_URL = API_PREFIX + "/users";

  @MockitoBean
  private UserService userService;

  @Test
  @DisplayName("회원정보 수정 성공: 인증 사용자 ID와 수정 요청을 서비스에 그대로 전달한다")
  void update_success() throws Exception {
    UserUpdateRequest request = new UserUpdateRequest("updated", "01098765432");
    UserResponse profile = new UserResponse(
        request.name(), request.phoneNumber(), 0, Instant.now());
    given(userService.updateProfile(any(UUID.class), any(UserUpdateRequest.class)))
        .willReturn(profile);

    mockMvc.perform(postJson(USERS_URL + "/update", request))
        .andExpect(status().isOk());

    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UserUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserUpdateRequest.class);
    verify(userService).updateProfile(userIdCaptor.capture(), requestCaptor.capture());
    assertThat(userIdCaptor.getValue()).isEqualTo(USER_ID);
    assertThat(requestCaptor.getValue()).isEqualTo(request);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidUpdateRequests")
  @DisplayName("회원정보 수정 실패: 유효하지 않은 필드의 오류를 반환하고 서비스를 호출하지 않는다")
  void update_failure_whenRequestIsInvalid(
      String caseName, UserUpdateRequest request, String field, String expectedMessage)
      throws Exception {
    mockMvc.perform(postJson(USERS_URL + "/update", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details." + field, hasItem(expectedMessage)));

    verify(userService, never()).updateProfile(any(UUID.class), any(UserUpdateRequest.class));
  }

  @Test
  @DisplayName("내 정보 조회 성공: 인증 사용자 ID로 조회한 사용자 정보를 반환한다")
  void getMe_success() throws Exception {
    UserResponse profile = new UserResponse("tester", "01012345678", 0, Instant.now());
    given(userService.findProfile(USER_ID)).willReturn(profile);

    mockMvc.perform(get(USERS_URL + "/me"))
        .andExpect(status().isOk());

    verify(userService).findProfile(USER_ID);
  }

  @Test
  @DisplayName("내 정보 조회 실패: 사용자가 없으면 404와 오류 코드를 반환한다")
  void getMe_failure_whenUserNotFound() throws Exception {
    given(userService.findProfile(USER_ID))
        .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

    mockMvc.perform(get(USERS_URL + "/me"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionType").value("USER-001"))
        .andExpect(jsonPath("$.message").value("해당 사용자를 찾을 수 없습니다."));
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of("이름 길이 초과",
            new UserUpdateRequest("nameistoolong", null),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("전화번호 형식 오류",
            new UserUpdateRequest(null, "010-1234-5678"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."));
  }
}
