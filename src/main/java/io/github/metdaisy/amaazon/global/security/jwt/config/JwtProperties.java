package io.github.metdaisy.amaazon.global.security.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
@ConfigurationProperties(prefix = "amaazon.jwt")
public record JwtProperties(String secretKey,
                            long accessTokenExpiration,
                            long refreshTokenExpiration,
                            long guestTokenExpiration,
                            JwtCookieProperties cookieProperties) {

  @NamedInterface("jwt")
  public record JwtCookieProperties(String name, String path, String sameSite, boolean secure) {

  }
}
