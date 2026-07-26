package io.github.metdaisy.amaazon.auth.presentation.handler;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.application.service.GuestTokenService;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SocialLoginSuccessHandler extends AbstractLoginSuccessHandler {

  private static final String ROLE_GUEST = "ROLE_GUEST";
  private final GuestTokenService guestTokenService;

  public SocialLoginSuccessHandler(AuthCookieProvider authCookieProvider,
      AuthTokenService authTokenService, GuestTokenService guestTokenService) {
    super(authTokenService, authCookieProvider);
    this.guestTokenService = guestTokenService;
  }

  @Override
  protected boolean handleGuest(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {

    if (isGuest(authentication)) {
      addGuestTokenCookie(authentication, response);
      response.sendRedirect(AuthWebConstants.GUEST_REDIRECT_URL);
      return true;
    }
    return false;
  }

  @Override
  protected String getDeviceId(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, AuthWebConstants.COOKIE_DEVICE_ID);
    if (cookie == null) {
      throw new AuthException(AuthErrorCode.DEVICE_ID_NOT_FOUND);
    }
    return cookie.getValue();
  }

  @Override
  protected void onSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication, JwtLoginDto loginDto) throws IOException {
    clearDeviceIdCookie(response);
    response.sendRedirect(AuthWebConstants.DEFAULT_SUCCESS_URL);
  }

  private void addGuestTokenCookie(Authentication authentication, HttpServletResponse response) {
    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    String provider = principal.getAttribute("provider");
    String providerId = principal.getAttribute("providerId");
    String guestToken = guestTokenService.create(provider, providerId);
    response.addHeader(HttpHeaders.SET_COOKIE, authCookieProvider.createGuestTokenCookie(guestToken).toString());
  }

  private void clearDeviceIdCookie(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, authCookieProvider.createDeleteDeviceIdCookie().toString());
  }

  private boolean isGuest(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals(ROLE_GUEST));
  }
}
