package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;

@ExtendWith(MockitoExtension.class)
class TokenBuilderFactoryTest {

  @Mock
  private JwtProperties jwtProperties;

  @InjectMocks
  private TokenBuilderFactory tokenBuilderFactory;

  @DisplayName("Access Token 클레임 빌드 - 성공: JWT ID, Subject, IssueTime, ExpirationTime, Role 이 설정된다")
  @Test
  void buildAccessTokenClaims_success() throws Exception {
    // given
    String subject = "user123";
    String authorities = "ROLE_USER,ROLE_ADMIN";
    long accessTokenExpiration = 3600L;
    given(jwtProperties.accessTokenExpiration()).willReturn(accessTokenExpiration);

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildAccessTokenClaims(subject, authorities);

    // then
    assertThat(claimsSet).isNotNull();
    assertThat(claimsSet.getJWTID()).isNotBlank();
    assertThat(claimsSet.getSubject()).isEqualTo(subject);
    assertThat(claimsSet.getIssueTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isAfter(claimsSet.getIssueTime());

    long expectedExpirationMs = accessTokenExpiration * 1000;
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
    given(jwtProperties.accessTokenExpiration()).willReturn(3600L);

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
    long refreshTokenExpiration = 7200L;
    given(jwtProperties.refreshTokenExpiration()).willReturn(refreshTokenExpiration);

    // when
    JWTClaimsSet claimsSet = tokenBuilderFactory.buildRefreshTokenClaims(subject);

    // then
    assertThat(claimsSet).isNotNull();
    assertThat(claimsSet.getJWTID()).isNotBlank();
    assertThat(claimsSet.getSubject()).isEqualTo(subject);
    assertThat(claimsSet.getIssueTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isNotNull();
    assertThat(claimsSet.getExpirationTime()).isAfter(claimsSet.getIssueTime());

    long expectedExpirationMs = refreshTokenExpiration * 1000;
    long actualExpirationDiff = claimsSet.getExpirationTime().getTime() - claimsSet.getIssueTime().getTime();
    assertThat(actualExpirationDiff).isEqualTo(expectedExpirationMs);
  }

  @DisplayName("Guest Token 클레임 빌드 - 성공: Provider, ProviderId, IssueTime, ExpirationTime 가 설정된다")
  @Test
  void buildGuestTokenClaims_success() throws Exception {
    // given
    String provider = "google";
    String providerId = "google_12345";
    long guestTokenExpiration = 1800L;
    given(jwtProperties.guestTokenExpiration()).willReturn(guestTokenExpiration);

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
    assertThat(actualExpirationDiff).isEqualTo(guestTokenExpiration);
  }
}
