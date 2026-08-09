package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;

@ExtendWith(MockitoExtension.class)
class TokenBuilderFactoryTest {

  @Mock
  private JwtTokenExpiration jwtTokenExpiration;

  @InjectMocks
  private TokenBuilderFactory tokenBuilderFactory;

  @DisplayName("Access Token 클레임 빌드 - 성공: JWT ID, Subject, IssueTime, ExpirationTime, Role 이 설정된다")
  @Test
  void buildAccessTokenClaims_success() throws Exception {
    // given
    String subject = "user123";
    String authorities = "ROLE_USER,ROLE_ADMIN";
    Duration accessTokenExpiration = Duration.ofSeconds(3600);
    given(jwtTokenExpiration.accessExpiration()).willReturn(accessTokenExpiration);

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildAccessTokenClaims(subject, authorities);

    // then
    assertThat(claimsSet).isNotNull();
    assertThat(claimsSet.getJWTID()).isNotBlank();
    assertThat(claimsSet.getSubject()).isEqualTo(subject);
    assertThat(claimsSet.getIssueTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isAfter(claimsSet.getIssueTime());

    long expectedExpirationMs = accessTokenExpiration.toMillis();
    long actualExpirationDiff = claimsSet.getExpirationTime().getTime() - claimsSet.getIssueTime().getTime();
    assertThat(actualExpirationDiff).isEqualTo(expectedExpirationMs);

    assertThat(claimsSet.getStringClaim("role")).isEqualTo(authorities);
  }

  @DisplayName("Access Token 클레임 빌드 - 성공: authorities 가 빈 문자열일 경우 Role 클레임이 설정되지 않는다")
  @Test
  void buildAccessTokenClaims_noAuthorities() throws Exception {
    // given
    String subject = "guest";
    String authorities = "";
    given(jwtTokenExpiration.accessExpiration()).willReturn(Duration.ofSeconds(3600));

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildAccessTokenClaims(subject, authorities);

    // then
    assertThat(claimsSet.getStringClaim("role")).isNull();
  }

  @DisplayName("Refresh Token 클레임 빌드 - 성공: JWT ID, Subject, IssueTime, ExpirationTime 가 설정된다")
  @Test
  void buildRefreshTokenClaims_success() {
    // given
    String subject = "user456";
    Duration refreshTokenExpiration = Duration.ofSeconds(7200);
    given(jwtTokenExpiration.refreshExpiration()).willReturn(refreshTokenExpiration);

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildRefreshTokenClaims(subject);

    // then
    assertThat(claimsSet).isNotNull();
    assertThat(claimsSet.getJWTID()).isNotBlank();
    assertThat(claimsSet.getSubject()).isEqualTo(subject);
    assertThat(claimsSet.getIssueTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isAfter(claimsSet.getIssueTime());

    long expectedExpirationMs = refreshTokenExpiration.toMillis();
    long actualExpirationDiff = claimsSet.getExpirationTime().getTime() - claimsSet.getIssueTime().getTime();
    assertThat(actualExpirationDiff).isEqualTo(expectedExpirationMs);
  }

  @DisplayName("Guest Token 클레임 빌드 - 성공: Provider, ProviderId, IssueTime, ExpirationTime 가 설정된다")
  @Test
  void buildGuestTokenClaims_success() throws Exception {
    // given
    String provider = "google";
    String providerId = "google_12345";
    Duration guestTokenExpiration = Duration.ofSeconds(1800);
    given(jwtTokenExpiration.guestExpiration()).willReturn(guestTokenExpiration);

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildGuestTokenClaims(provider, providerId);

    // then
    assertThat(claimsSet).isNotNull();
    assertThat(claimsSet.getJWTID()).isNotBlank();
    assertThat(claimsSet.getStringClaim("provider")).isEqualTo(provider);
    assertThat(claimsSet.getStringClaim("providerId")).isEqualTo(providerId);
    assertThat(claimsSet.getIssueTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isAfter(claimsSet.getIssueTime());

    long actualExpirationDiff = claimsSet.getExpirationTime().getTime() - claimsSet.getIssueTime().getTime();
    assertThat(actualExpirationDiff).isEqualTo(guestTokenExpiration.toMillis());
  }
}
