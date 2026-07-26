package io.github.metdaisy.amaazon.auth.presentation.handler;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractLoginSuccessHandler implements AuthenticationSuccessHandler {

  protected final AuthTokenService authTokenService;
  protected final AuthCookieProvider authCookieProvider;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    if (handleGuest(request, response, authentication)) {
      return;
    }
    UUID userId = UUID.fromString(authentication.getName());
    String deviceId = getDeviceId(request);
    JwtLoginDto loginDto = createJwtLoginDto(userId, deviceId);
    response.addHeader(HttpHeaders.SET_COOKIE, authCookieProvider.createRefreshTokenCookie(
        loginDto.refreshToken()).toString());
    onSuccess(request, response, authentication, loginDto);
  }

  protected boolean handleGuest(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    return false;
  }

  protected abstract String getDeviceId(HttpServletRequest request);

  protected JwtLoginDto createJwtLoginDto(UUID userId, String deviceId) {
    return authTokenService.create(userId, deviceId);
  }

  protected abstract void onSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication, JwtLoginDto loginDto) throws IOException;
}
