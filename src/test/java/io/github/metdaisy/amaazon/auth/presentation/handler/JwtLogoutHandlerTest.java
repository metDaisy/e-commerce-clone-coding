package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtLogoutHandlerTest {

  @Mock
  private AuthTokenService authTokenService;

  @Mock
  private AuthCookieProvider authCookieProvider;

  @InjectMocks
  private JwtLogoutHandler jwtLogoutHandler;

  @Test
  @DisplayName("logout_success")
  void logout_success() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Cookie cookie = new Cookie(AuthWebConstants.REFRESH_TOKEN, "test-refresh-token");
    request.setCookies(cookie);

    given(authCookieProvider.createDeleteRefreshTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, "").build());
    given(authCookieProvider.createDeleteGuestTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, "").build());

    // when
    jwtLogoutHandler.logout(request, response, null);

    // then
    verify(authTokenService).delete("test-refresh-token");
    verify(authCookieProvider).createDeleteRefreshTokenCookie();
    verify(authCookieProvider).createDeleteGuestTokenCookie();
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNotNull();
  }

  @Test
  @DisplayName("logout_failure_noCookie")
  void logout_failure_noCookie() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    // No cookie set

    given(authCookieProvider.createDeleteRefreshTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, "").build());
    given(authCookieProvider.createDeleteGuestTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, "").build());

    // when
    jwtLogoutHandler.logout(request, response, null);

    // then
    verify(authTokenService, never()).delete(anyString());
    verify(authCookieProvider).createDeleteRefreshTokenCookie();
    verify(authCookieProvider).createDeleteGuestTokenCookie();
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNotNull();
  }
}
