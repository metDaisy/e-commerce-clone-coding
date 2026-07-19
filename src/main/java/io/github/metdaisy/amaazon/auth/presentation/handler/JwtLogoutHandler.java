package io.github.metdaisy.amaazon.auth.presentation.handler;

import io.github.metdaisy.amaazon.auth.application.service.JwtTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.JwtCookieProvider;
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

  private final JwtTokenService service;
  private final JwtCookieProvider provider;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) {
    Cookie cookie = WebUtils.getCookie(request, "REFRESH_TOKEN");
    if (cookie != null) {
      service.delete(cookie.getValue());
    }
    ResponseCookie deleteCookie = provider.createDeleteTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
  }
}
