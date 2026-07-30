package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.PrintWriter;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.application.service.GuestTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class FormLoginSuccessHandlerTest {

  @Mock
  private AuthTokenService authTokenService;
  @Mock
  private AuthCookieProvider authCookieProvider;
  @Mock
  private ObjectMapper mapper;
  @Mock
  private GuestTokenService guestTokenService;
  @Mock
  private AuthService authService;
  @Mock
  private Authentication authentication;

  @InjectMocks
  private FormLoginSuccessHandler handler;

  @Test
  @DisplayName("onAuthenticationSuccess: guest token cookie absent")
  void onAuthenticationSuccess_noGuestToken() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(AuthWebConstants.HEADER_DEVICE_ID, "device-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    UUID userId = UUID.randomUUID();
    given(authentication.getName()).willReturn(userId.toString());

    JwtLoginDto loginDto = new JwtLoginDto(userId, "access", "refresh");
    given(authTokenService.create(userId, "device-123")).willReturn(loginDto);
    given(authCookieProvider.createRefreshTokenCookie("refresh"))
        .willReturn(ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, "refresh").build());
    given(authCookieProvider.createDeleteGuestTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, "").build());

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    verify(guestTokenService, never()).validate(anyString());
    verify(authService, never()).createSocial(any(UUID.class), anyString(), anyString());
    verify(mapper).writeValue(any(PrintWriter.class), any(JwtLoginDto.class));
  }

  @Test
  @DisplayName("onAuthenticationSuccess: guest token cookie present")
  void onAuthenticationSuccess_withGuestToken() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(AuthWebConstants.HEADER_DEVICE_ID, "device-123");
    request.setCookies(new Cookie(AuthWebConstants.COOKIE_GUEST_TOKEN, "guest-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    UUID userId = UUID.randomUUID();
    given(authentication.getName()).willReturn(userId.toString());

    JwtLoginDto loginDto = new JwtLoginDto(userId, "access", "refresh");
    given(authTokenService.create(userId, "device-123")).willReturn(loginDto);
    given(authCookieProvider.createRefreshTokenCookie("refresh"))
        .willReturn(ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, "refresh").build());
    given(authCookieProvider.createDeleteGuestTokenCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, "").build());

    given(guestTokenService.getProvider("guest-token")).willReturn("google");
    given(guestTokenService.getProviderId("guest-token")).willReturn("google-id");

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    verify(guestTokenService).validate("guest-token");
    verify(authService).createSocial(userId, "google", "google-id");
  }
}
