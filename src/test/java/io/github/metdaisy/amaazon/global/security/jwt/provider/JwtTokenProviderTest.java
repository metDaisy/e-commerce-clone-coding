package io.github.metdaisy.amaazon.global.security.jwt.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.global.security.jwt.builder.SignedTokenFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.builder.TokenBuilderFactory;
import io.github.metdaisy.amaazon.global.security.jwt.builder.TokenValidator;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  @Mock
  private TokenBuilderFactory tokenBuilderFactory;

  @Mock
  private SignedTokenFactory signedTokenFactory;

  @Mock
  private TokenValidator tokenValidator;

  @Mock
  private JWSVerifier verifier;

  @InjectMocks
  private JwtTokenProvider jwtTokenProvider;

  private final String secretKey = "testSecretKeyForJwtSigningMustBeLongEnoughForHS256";
  private JWSHeader header;

  @BeforeEach
  void setUp() throws Exception {
    header = new JWSHeader(JWSAlgorithm.HS256);
  }

  @DisplayName("Access Token 생성 - 성공: 클레임 빌드 및 서명이 호출되어 토큰이 생성된다")
  @Test
  void generateAccessToken_success() {
    String subject = "user123";
    String authorities = "ROLE_USER";
    JWTClaimsSet mockClaims = new JWTClaimsSet.Builder().subject(subject).build();
    String mockToken = "mock.access.token";

    given(tokenBuilderFactory.buildAccessTokenClaims(subject, authorities)).willReturn(mockClaims);
    given(signedTokenFactory.sign(mockClaims)).willReturn(mockToken);

    String token = jwtTokenProvider.generateAccessToken(subject, authorities);

    assertThat(token).isEqualTo(mockToken);
  }

  @DisplayName("Refresh Token 생성 - 성공: 클레임 빌드 및 서명이 호출되어 토큰이 생성된다")
  @Test
  void generateRefreshToken_success() {
    String subject = "user456";
    JWTClaimsSet mockClaims = new JWTClaimsSet.Builder().subject(subject).build();
    String mockToken = "mock.refresh.token";

    given(tokenBuilderFactory.buildRefreshTokenClaims(subject)).willReturn(mockClaims);
    given(signedTokenFactory.sign(mockClaims)).willReturn(mockToken);

    String token = jwtTokenProvider.generateRefreshToken(subject);

    assertThat(token).isEqualTo(mockToken);
  }

  @DisplayName("Guest Token 생성 - 성공: 클레임 빌드 및 서명이 호출되어 토큰이 생성된다")
  @Test
  void generateGuestToken_success() {
    String provider = "google";
    String providerId = "google_12345";
    JWTClaimsSet mockClaims = new JWTClaimsSet.Builder()
        .claim("provider", provider)
        .claim("providerId", providerId)
        .build();
    String mockToken = "mock.guest.token";

    given(tokenBuilderFactory.buildGuestTokenClaims(provider, providerId)).willReturn(mockClaims);
    given(signedTokenFactory.sign(mockClaims)).willReturn(mockToken);

    String token = jwtTokenProvider.generateGuestToken(provider, providerId);

    assertThat(token).isEqualTo(mockToken);
  }

  @DisplayName("Authentication 조회 - 성공: 유효한 토큰으로 Authentication 이 생성된다")
  @Test
  void getAuthentication_success() throws Exception {
    String subject = UUID.randomUUID().toString();
    String role = "USER";
    String jti = UUID.randomUUID().toString();
    Date now = new Date();
    Date expiration = new Date(System.currentTimeMillis() + 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(now)
        .expirationTime(expiration)
        .claim("role", role)
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    Authentication authentication = jwtTokenProvider.getAuthentication(token);

    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo(subject);
    assertThat(authentication.getAuthorities()).hasSize(1);
  }

  @DisplayName("Authentication 조회 - 실패: 만료된 토큰일 경우 JwtException 이 발생한다")
  @Test
  void getAuthentication_expiredToken_failure() throws Exception {
    String subject = "user-uuid-123";
    String role = "USER";
    String jti = UUID.randomUUID().toString();
    Date now = new Date();
    Date expiration = new Date(System.currentTimeMillis() - 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(now)
        .expirationTime(expiration)
        .claim("role", role)
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    willThrow(new JwtException(JwtErrorCode.TOKEN_EXPIRED))
        .given(tokenValidator).validateExpiration(any());

    assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(token))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_EXPIRED.getCode());
  }

  @DisplayName("토큰 검증 - 성공: 유효한 토큰일 경우 예외가 발생하지 않는다")
  @Test
  void validate_success() {
    String validToken = "valid.token.format";

    willDoNothing().given(tokenValidator).validate(validToken, verifier);

    jwtTokenProvider.validate(validToken);

    verify(tokenValidator).validate(validToken, verifier);
  }

  @DisplayName("토큰 검증 - 실패: 유효하지 않은 토큰일 경우 JwtException 이 발생한다")
  @Test
  void validate_invalidSignature_failure() throws Exception {
    String invalidToken = "invalid.token.format";

    willThrow(new JwtException(JwtErrorCode.VERIFICATION_FAILED)).given(tokenValidator).validate(invalidToken,
        verifier);

    assertThatThrownBy(() -> jwtTokenProvider.validate(invalidToken))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.VERIFICATION_FAILED.getCode());
  }

  @DisplayName("JTI 파싱 - 성공: 유효한 토큰에서 JTI 가 추출된다")
  @Test
  void parseJti_success() throws Exception {
    String jti = UUID.randomUUID().toString();
    String subject = "user-uuid-123";
    Date now = new Date();
    Date expiration = new Date(System.currentTimeMillis() + 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(now)
        .expirationTime(expiration)
        .claim("role", "USER")
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    String parsedJti = jwtTokenProvider.parseJti(token);

    assertThat(parsedJti).isEqualTo(jti);
  }

  @DisplayName("IssuedAt 파싱 - 성공: 유효한 토큰에서 발행 시간이 추출된다")
  @Test
  void parseIssueTime_success() throws Exception {
    String jti = UUID.randomUUID().toString();
    String subject = "user-uuid-123";
    long currentTimeSec = System.currentTimeMillis() / 1000 * 1000;
    Date now = new Date(currentTimeSec);
    Date expiration = new Date(currentTimeSec + 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(now)
        .expirationTime(expiration)
        .claim("role", "USER")
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    Instant issueTime = jwtTokenProvider.parseIssueTime(token);

    assertThat(issueTime).isEqualTo(now.toInstant());
  }

  @DisplayName("Provider 파싱 - 성공: Guest 토큰에서 provider 클레임이 추출된다")
  @Test
  void parseProvider_success() throws Exception {
    String provider = "google";
    String providerId = "google_12345";
    String jti = UUID.randomUUID().toString();
    Date now = new Date();
    Date expiration = new Date(System.currentTimeMillis() + 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject("guest")
        .issueTime(now)
        .expirationTime(expiration)
        .claim("provider", provider)
        .claim("providerId", providerId)
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    String parsedProvider = jwtTokenProvider.parseProvider(token, "provider");

    assertThat(parsedProvider).isEqualTo(provider);
  }
}
