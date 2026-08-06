package io.github.metdaisy.amaazon.auth.presentation.handler;

import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

  private final AuthTokenService service;
  private final AuthCookieProvider provider;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) {
    Cookie cookie = WebUtils.getCookie(request, AuthWebConstants.REFRESH_TOKEN);
    if (cookie != null) {
      service.delete(cookie.getValue());
    }
    ResponseCookie deleteGuestCookie = provider.createDeleteGuestTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, deleteGuestCookie.toString());
    ResponseCookie deleteCookie = provider.createDeleteRefreshTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
  }
}
