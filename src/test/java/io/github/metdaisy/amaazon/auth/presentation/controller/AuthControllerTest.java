package io.github.metdaisy.amaazon.auth.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import io.github.metdaisy.amaazon.global.security.constant.SecurityConstants;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import io.github.metdaisy.amaazon.global.exception.GlobalExceptionHandler;
import io.github.metdaisy.amaazon.global.security.config.SecurityConfig;
import io.github.metdaisy.amaazon.global.security.filter.JwtAuthenticationFilter;
import io.github.metdaisy.amaazon.global.web.config.WebMvcConfig;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class, WebMvcConfig.class})
@DisplayName("AuthController 테스트")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean(name = "formLoginSuccessHandler")
  private AuthenticationSuccessHandler formLoginSuccessHandler;

  @MockitoBean
  private AuthenticationFailureHandler loginFailureHandler;

  @MockitoBean
  private LogoutHandler logoutHandler;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private BlacklistRegistry blacklistRegistry;

  @MockitoBean
  private ExceptionStrategyFactory exceptionStrategyFactory;

  @MockitoBean
  private AuthTokenService authTokenService;

  @MockitoBean
  private AuthCookieProvider authCookieProvider;

  @Test
  @DisplayName("refresh_success: 유효한 refresh token으로 재발급 성공")
  void refresh_success() throws Exception {
    // given
    String refreshToken = "valid-refresh-token";
    String newRefreshToken = "new-refresh-token";
    String accessToken = "new-access-token";
    UUID userId = UUID.randomUUID();

    JwtLoginDto loginDto = new JwtLoginDto(userId, accessToken, newRefreshToken);
    given(authTokenService.reissue(refreshToken)).willReturn(loginDto);
    given(authCookieProvider.createRefreshTokenCookie(newRefreshToken)).willReturn(
        ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, newRefreshToken)
            .path("/")
            .maxAge(Duration.ofSeconds(3600))
            .httpOnly(true)
            .build());

    // when & then
    mockMvc.perform(post(SecurityConstants.REFRESH_URL)
        .cookie(new Cookie(AuthWebConstants.REFRESH_TOKEN, refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.accessToken").value(accessToken))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("REFRESH_TOKEN")));
  }

  @Test
  @DisplayName("refresh_failure_emptyToken: 빈 토큰 제공 시 401 반환")
  void refresh_failure_emptyToken() throws Exception {
    // given & when & then
    mockMvc.perform(post(SecurityConstants.REFRESH_URL))
        .andExpect(status().isUnauthorized());
  }
}
