package io.github.metdaisy.amaazon.global.security.jwt.builder;

import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;
import lombok.RequiredArgsConstructor;

/**
 * JWT 클레임셋(JWTClaimsSet)을 생성하는 팩토리입니다. Access Token, Refresh Token, Guest Token의 클레임셋 생성 로직을
 * 캡슐화합니다.
 */
@Component
@RequiredArgsConstructor
public class TokenBuilderFactory {

  private final JwtTokenExpiration jwtTokenExpiration;

  /**
   * Access Token용 클레임셋을 생성합니다.
   */
  public JWTClaimsSet buildAccessTokenClaims(String subject, String authorities) {
    long now = System.currentTimeMillis();
    long expirationMillis = jwtTokenExpiration.accessExpiration().toMillis();

    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
        .jwtID(UUID.randomUUID().toString())
        .subject(subject)
        .issueTime(new Date(now))
        .expirationTime(new Date(now + expirationMillis));

    if (StringUtils.hasText(authorities)) {
      builder.claim("role", authorities);
    }

    return builder.build();
  }

  /**
   * Refresh Token용 클레임셋을 생성합니다.
   */
  public JWTClaimsSet buildRefreshTokenClaims(String subject) {
    long now = System.currentTimeMillis();
    long expirationMillis = jwtTokenExpiration.refreshExpiration().toMillis();

    return new JWTClaimsSet.Builder()
        .jwtID(UUID.randomUUID().toString())
        .subject(subject)
        .issueTime(new Date(now))
        .expirationTime(new Date(now + expirationMillis))
        .build();
  }

  /**
   * Guest Token용 클레임셋을 생성합니다.
   */
  public JWTClaimsSet buildGuestTokenClaims(String provider, String providerId) {
    long now = System.currentTimeMillis();
    long expirationMillis = jwtTokenExpiration.guestExpiration().toMillis();

    return new JWTClaimsSet.Builder()
        .jwtID(UUID.randomUUID().toString())
        .claim("provider", provider)
        .claim("providerId", providerId)
        .issueTime(new Date(now))
        .expirationTime(new Date(now + expirationMillis))
        .build();
  }
}
