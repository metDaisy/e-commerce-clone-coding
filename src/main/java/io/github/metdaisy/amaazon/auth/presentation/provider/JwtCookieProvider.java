package io.github.metdaisy.amaazon.auth.presentation.provider;

import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtCookieProvider {

  private final JwtProperties properties;

  public ResponseCookie createTokenCookie(String token) {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(cookieProps.name(), token)
            .path(cookieProps.path())
            .maxAge(properties.refreshTokenExpiration())
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }

  public ResponseCookie createDeleteTokenCookie() {
    JwtProperties.JwtCookieProperties cookieProps = properties.cookieProperties();
    return ResponseCookie.from(cookieProps.name(), "")
            .path(cookieProps.path())
            .maxAge(0)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .build();
  }
}
