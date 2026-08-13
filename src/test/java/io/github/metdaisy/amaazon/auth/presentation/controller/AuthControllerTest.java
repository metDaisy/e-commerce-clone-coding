package io.github.metdaisy.amaazon.auth.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.dto.request.SignUpRequest;
import io.github.metdaisy.amaazon.auth.application.dto.request.UserCredentialUpdateRequest;
import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.controller.dto.request.PasswordValidationRequest;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("인증 컨트롤러 슬라이스 테스트")
class AuthControllerTest extends RestControllerTest {

  private static final String AUTH_URL = API_PREFIX + "/auth";

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private AuthTokenService authTokenService;

  @MockitoBean
  private AuthCookieProvider authCookieProvider;

  @Test
  @DisplayName("토큰 재발급 성공: 새 토큰과 refresh token 쿠키를 반환한다")
  void refresh_success() throws Exception {
    String refreshToken = "valid-refresh-token";
    String newRefreshToken = "new-refresh-token";
    String accessToken = "new-access-token";
    JwtLoginDto loginDto = new JwtLoginDto(USER_ID, accessToken, newRefreshToken);
    given(authTokenService.reissue(refreshToken)).willReturn(loginDto);
    given(authCookieProvider.createRefreshTokenCookie(newRefreshToken)).willReturn(
        ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, newRefreshToken)
            .path("/")
            .maxAge(Duration.ofSeconds(3600))
            .httpOnly(true)
            .build());

    mockMvc.perform(post(AUTH_URL + "/refresh")
            .cookie(new Cookie(AuthWebConstants.REFRESH_TOKEN, refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.accessToken").value(accessToken))
        .andExpect(header().string(HttpHeaders.SET_COOKIE,
            containsString(AuthWebConstants.REFRESH_TOKEN + "=" + newRefreshToken)));

    verify(authTokenService).reissue(refreshToken);
    verify(authCookieProvider).createRefreshTokenCookie(newRefreshToken);
  }

  @Test
  @DisplayName("토큰 재발급 실패: refresh token 쿠키가 없으면 401을 반환하고 서비스를 호출하지 않는다")
  void refresh_failure_whenRefreshTokenIsMissing() throws Exception {
    mockMvc.perform(post(AUTH_URL + "/refresh"))
        .andExpect(status().isUnauthorized());

    verify(authTokenService, never()).reissue(any());
    verify(authCookieProvider, never()).createRefreshTokenCookie(any());
  }

  @Test
  @DisplayName("토큰 재발급 실패: 저장된 refresh token이 없으면 404와 오류 코드를 반환한다")
  void refresh_failure_whenRefreshTokenIsNotFound() throws Exception {
    String refreshToken = "unknown-refresh-token";
    given(authTokenService.reissue(refreshToken))
        .willThrow(new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

    mockMvc.perform(post(AUTH_URL + "/refresh")
            .cookie(new Cookie(AuthWebConstants.REFRESH_TOKEN, refreshToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionType").value("AUTH-003"))
        .andExpect(jsonPath("$.message").value("refreshToken DB 에서 해당 토큰을 찾을 수 없습니다."));

    verify(authCookieProvider, never()).createRefreshTokenCookie(any());
  }

  @Test
  @DisplayName("비밀번호 확인 성공: 인증 사용자 ID와 비밀번호를 서비스에 전달한다")
  void verifyPassword_success() throws Exception {
    PasswordValidationRequest request = new PasswordValidationRequest("Password1!");

    mockMvc.perform(postJson(AUTH_URL + "/password/verify", request))
        .andExpect(status().isNoContent());

    verify(authService).verifyPassword(USER_ID, request.password());
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidPasswords")
  @DisplayName("비밀번호 확인 실패: 유효하지 않은 비밀번호면 필드 오류를 반환하고 서비스를 호출하지 않는다")
  void verifyPassword_failure_whenPasswordIsInvalid(
      String caseName, String password, String expectedMessage) throws Exception {
    PasswordValidationRequest request = new PasswordValidationRequest(password);

    mockMvc.perform(postJson(AUTH_URL + "/password/verify", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.password", hasItem(expectedMessage)));

    verify(authService, never()).verifyPassword(any(), any());
  }

  @Test
  @DisplayName("비밀번호 확인 실패: 비밀번호가 일치하지 않으면 400과 오류 코드를 반환한다")
  void verifyPassword_failure_whenPasswordIsIncorrect() throws Exception {
    PasswordValidationRequest request = new PasswordValidationRequest("Password1!");
    willThrow(new AuthException(AuthErrorCode.INCORRECT_PASSWORD))
        .given(authService).verifyPassword(USER_ID, request.password());

    mockMvc.perform(postJson(AUTH_URL + "/password/verify", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("AUTH-009"))
        .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
  }

  @Test
  @DisplayName("인증정보 수정 성공: 인증 사용자 ID와 수정 요청을 서비스에 전달한다")
  void update_success() throws Exception {
    UserCredentialUpdateRequest request =
        new UserCredentialUpdateRequest("new@example.com", "NewPassword1!");

    mockMvc.perform(postJson(AUTH_URL + "/update", request))
        .andExpect(status().isNoContent());

    verify(authService).update(USER_ID, request);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidCredentialUpdateRequests")
  @DisplayName("인증정보 수정 실패: 유효하지 않은 필드는 구체적인 오류를 반환하고 서비스를 호출하지 않는다")
  void update_failure_whenRequestIsInvalid(
      String caseName, UserCredentialUpdateRequest request, String field, String expectedMessage)
      throws Exception {
    mockMvc.perform(postJson(AUTH_URL + "/update", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details." + field, hasItem(expectedMessage)));

    verify(authService, never()).update(any(), any());
  }

  @Test
  @DisplayName("회원가입 성공: 검증된 가입 요청을 서비스에 전달한다")
  void signup_success() throws Exception {
    SignUpRequest request = validSignUpRequest();

    mockMvc.perform(postJson(AUTH_URL + "/signup", request))
        .andExpect(status().isNoContent());

    verify(authService).create(request);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidSignUpRequests")
  @DisplayName("회원가입 실패: 유효하지 않은 필드는 구체적인 오류를 반환하고 서비스를 호출하지 않는다")
  void signup_failure_whenRequestIsInvalid(
      String caseName, SignUpRequest request, String field, String expectedMessage)
      throws Exception {
    mockMvc.perform(postJson(AUTH_URL + "/signup", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details." + field, hasItem(expectedMessage)));

    verify(authService, never()).create(any());
  }

  @Test
  @DisplayName("회원가입 실패: 이미 가입된 이메일이면 409와 오류 코드를 반환한다")
  void signup_failure_whenEmailAlreadyExists() throws Exception {
    SignUpRequest request = validSignUpRequest();
    willThrow(new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS))
        .given(authService).create(request);

    mockMvc.perform(postJson(AUTH_URL + "/signup", request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionType").value("AUTH-004"))
        .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
  }

  private static Stream<Arguments> invalidPasswords() {
    return Stream.of(
        Arguments.of("비밀번호 누락", null,
            "비밀번호는 영문 대소문자, 숫자, 특수문자 중 최소 2가지 이상 포함해야 합니다."),
        Arguments.of("8자 미만", "Pass1!",
            "비밀번호는 최소 8자 이상이어야 합니다."),
        Arguments.of("공백 포함", "Password 1!",
            "비밀번호에 공백이 포함되어있습니다."),
        Arguments.of("문자 종류 조합 부족", "abcdefgh",
            "비밀번호는 영문 대소문자, 숫자, 특수문자 중 최소 2가지 이상 포함해야 합니다."));
  }

  private static Stream<Arguments> invalidCredentialUpdateRequests() {
    return Stream.of(
        Arguments.of("이메일 형식 오류",
            new UserCredentialUpdateRequest("invalid-email", null),
            "email", "올바른 이메일 형식이 아닙니다."),
        Arguments.of("비밀번호 길이 부족",
            new UserCredentialUpdateRequest(null, "Pass1!"),
            "password", "비밀번호는 최소 8자 이상이어야 합니다."),
        Arguments.of("비밀번호 공백 포함",
            new UserCredentialUpdateRequest(null, "Password 1!"),
            "password", "비밀번호에 공백이 포함되어있습니다."));
  }

  private static Stream<Arguments> invalidSignUpRequests() {
    return Stream.of(
        Arguments.of("이름 누락",
            new SignUpRequest("", "test@example.com", "Password1!", "01012345678"),
            "name", "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요."),
        Arguments.of("이메일 형식 오류",
            new SignUpRequest("tester", "invalid-email", "Password1!", "01012345678"),
            "email", "올바른 이메일 형식이 아닙니다."),
        Arguments.of("비밀번호 길이 부족",
            new SignUpRequest("tester", "test@example.com", "Pass1!", "01012345678"),
            "password", "비밀번호는 최소 8자 이상이어야 합니다."),
        Arguments.of("전화번호 형식 오류",
            new SignUpRequest("tester", "test@example.com", "Password1!", "010-1234-5678"),
            "phoneNumber", "전화번호는 숫자만 11자리로 입력해주세요."));
  }

  private static SignUpRequest validSignUpRequest() {
    return new SignUpRequest(
        "tester", "test@example.com", "Password1!", "01012345678");
  }
}
