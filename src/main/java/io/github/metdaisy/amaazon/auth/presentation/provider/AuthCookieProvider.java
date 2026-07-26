package io.github.metdaisy.amaazon.auth.presentation.provider;

import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieProvider {

  private final JwtProperties properties;

  public ResponseCookie createRefreshTokenCookie(String token) {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(cookieProps.name(), token)
            .path(cookieProps.path())
            .maxAge(properties.refreshTokenExpiration())
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }

  public ResponseCookie createDeleteRefreshTokenCookie() {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(cookieProps.name(), null)
            .path(cookieProps.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }

  public ResponseCookie createGuestTokenCookie(String token) {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, token)
            .path(cookieProps.path())
            .maxAge(properties.guestTokenExpiration() / 1000)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }

  public ResponseCookie createDeleteGuestTokenCookie() {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, null)
            .path(cookieProps.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }

  public ResponseCookie createDeleteDeviceIdCookie() {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(AuthWebConstants.COOKIE_DEVICE_ID, null)
            .path(cookieProps.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }
}