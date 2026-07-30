package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;

class SignedTokenFactoryTest {

  private SignedTokenFactory signedTokenFactory;
  private final String secretKey = "testSecretKeyForJwtSigningMustBeLongEnoughForHS256";
  private JWSHeader header;
  private JWSSigner signer;

  @BeforeEach
  void setUp() throws Exception {
    header = new JWSHeader(JWSAlgorithm.HS256);
    signer = new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8));
    signedTokenFactory = new SignedTokenFactory(header, signer);
  }

  @DisplayName("JWT 서명 - 성공: 유효한 클레임으로 서명된 토큰이 생성된다")
  @Test
  void sign_success() throws Exception {
    // given
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject("testUser")
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .jwtID("test-jti")
        .build();

    // when
    String token = signedTokenFactory.sign(claimsSet);

    // then
    assertThat(token).isNotBlank();
    String[] parts = token.split("\\.");
    assertThat(parts).hasSize(3);
  }

  @DisplayName("JWT 서명 - 실패: 서명 과정에서 예외가 발생하면 JwtException 이 발생한다")
  @Test
  void sign_failure_invalidSigner() throws Exception {
    // given
    JWSHeader invalidHeader = new JWSHeader(JWSAlgorithm.HS256);
    JWSSigner failingSigner = mock(JWSSigner.class);
    given(failingSigner.sign(any(), any()))
        .willThrow(new JOSEException("Signing failed"));
    SignedTokenFactory invalidSignedTokenFactory = new SignedTokenFactory(invalidHeader, failingSigner);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject("testUser")
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .build();

    // when & then
    assertThatThrownBy(() -> invalidSignedTokenFactory.sign(claimsSet))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.SIGN_FAILED.getCode());
  }

  @DisplayName("JWT 검증 - 성공: 유효한 서명이면 true 를 반환한다")
  @Test
  void verify_success() throws Exception {
    // given
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject("testUser")
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .build();

    String serializedToken = signedTokenFactory.sign(claimsSet);
    SignedJWT signedJWT = SignedJWT.parse(serializedToken);
    JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));

    // when
    boolean isVerified = signedTokenFactory.verify(signedJWT, verifier);

    // then
    assertThat(isVerified).isTrue();
  }

  @DisplayName("JWT 검증 - 실패: 잘못된 키로 서명된 토큰은 검증이 실패한다")
  @Test
  void verify_failure_wrongKey() throws Exception {
    // given
    String wrongSecretKey = "wrongSecretKeyForJwtSigningMustBeLongEnoughForHS256";
    JWSSigner wrongSigner = new MACSigner(wrongSecretKey.getBytes(StandardCharsets.UTF_8));
    SignedTokenFactory wrongSignedTokenFactory = new SignedTokenFactory(header, wrongSigner);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject("testUser")
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .build();

    String serializedToken = wrongSignedTokenFactory.sign(claimsSet);
    SignedJWT signedJWT = SignedJWT.parse(serializedToken);
    JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));

    // when
    boolean isVerified = signedTokenFactory.verify(signedJWT, verifier);

    // then
    assertThat(isVerified).isFalse();
  }
}
