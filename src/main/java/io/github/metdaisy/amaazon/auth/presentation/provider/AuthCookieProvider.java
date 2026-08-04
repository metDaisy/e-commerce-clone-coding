package io.github.metdaisy.amaazon.auth.presentation.provider;

import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtCookieProperties;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieProvider {

  private final JwtTokenExpiration jwtTokenExpiration;
  private final JwtCookieProperties jwtCookieProperties;

  public ResponseCookie createRefreshTokenCookie(String token) {
    return ResponseCookie.from(jwtCookieProperties.name(), token)
            .path(jwtCookieProperties.path())
            .maxAge(jwtTokenExpiration.refreshExpiration())
            .httpOnly(true)
            .secure(jwtCookieProperties.secure())
            .sameSite(jwtCookieProperties.sameSite())
            .build();
  }

  public ResponseCookie createDeleteRefreshTokenCookie() {
    return ResponseCookie.from(jwtCookieProperties.name(), null)
            .path(jwtCookieProperties.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(jwtCookieProperties.secure())
            .sameSite(jwtCookieProperties.sameSite())
            .build();
  }

  public ResponseCookie createGuestTokenCookie(String token) {
    return ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, token)
            .path(jwtCookieProperties.path())
            .maxAge(jwtTokenExpiration.guestExpiration())
            .httpOnly(true)
            .secure(jwtCookieProperties.secure())
            .sameSite(jwtCookieProperties.sameSite())
            .build();
  }

  public ResponseCookie createDeleteGuestTokenCookie() {
    return ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, null)
            .path(jwtCookieProperties.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(jwtCookieProperties.secure())
            .sameSite(jwtCookieProperties.sameSite())
            .build();
  }

  public ResponseCookie createDeleteDeviceIdCookie() {
    return ResponseCookie.from(AuthWebConstants.COOKIE_DEVICE_ID, null)
            .path(jwtCookieProperties.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(jwtCookieProperties.secure())
            .sameSite(jwtCookieProperties.sameSite())
            .build();
  }
}
