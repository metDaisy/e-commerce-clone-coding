package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;

@ExtendWith(MockitoExtension.class)
class TokenValidatorTest {

  @Mock
  private SignedTokenFactory signedTokenFactory;

  @InjectMocks
  private TokenValidator tokenValidator;

  private final String secretKey = "testSecretKeyForJwtSigningMustBeLongEnoughForHS256";
  private JWSHeader header;
  private JWSVerifier verifier;

  @BeforeEach
  void setUp() throws Exception {
    header = new JWSHeader(JWSAlgorithm.HS256);
    verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  @DisplayName("토큰 검증 - 성공: 유효한 토큰은 서명 및 만료 시간 검증을 통과한다")
  @Test
  void validate_success() throws Exception {
    // given
    JWTClaimsSet claimsSet = createValidClaimsSet();
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    given(signedTokenFactory.verify(any(), any())).willReturn(true);

    // when & then
    tokenValidator.validate(token, verifier);
  }

  @DisplayName("토큰 검증 - 실패: 서명 검증 실패 시 JwtException 이 발생한다")
  @Test
  void validate_signatureFailure_failure() throws Exception {
    // given
    JWTClaimsSet claimsSet = createValidClaimsSet();
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    given(signedTokenFactory.verify(any(), any())).willReturn(false);

    // when & then
    assertThatThrownBy(() -> tokenValidator.validate(token, verifier))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.VERIFICATION_FAILED.getCode());
  }

  @DisplayName("서명 검증 - 성공: 유효한 서명일 경우 예외가 발생하지 않는다")
  @Test
  void validateSignature_success() throws Exception {
    // given
    JWTClaimsSet claimsSet = createValidClaimsSet();
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    given(signedTokenFactory.verify(any(), any())).willReturn(true);

    // when & then
    tokenValidator.validateSignature(token, verifier);
    then(signedTokenFactory).should().verify(any(), any());
  }

  @DisplayName("서명 검증 - 실패: 유효하지 않은 서명일 경우 JwtException 이 발생한다")
  @Test
  void validateSignature_failure() throws Exception {
    // given
    JWTClaimsSet claimsSet = createValidClaimsSet();
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
    String token = signedJWT.serialize();

    given(signedTokenFactory.verify(any(), any())).willReturn(false);

    // when & then
    assertThatThrownBy(() -> tokenValidator.validateSignature(token, verifier))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.VERIFICATION_FAILED.getCode());
  }

  @DisplayName("만료 시간 검증 - 성공: 유효한 만료 시간일 경우 예외가 발생하지 않는다")
  @Test
  void validateExpiration_success() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti",
        "subject",
        new Date(),
        new Date(System.currentTimeMillis() + 3600000),
        "role",
        null);

    // when & then
    tokenValidator.validateExpiration(parsedToken);
  }

  @DisplayName("만료 시간 검증 - 실패: 만료된 토큰일 경우 JwtException 이 발생한다")
  @Test
  void validateExpiration_expired_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti",
        "subject",
        new Date(),
        new Date(System.currentTimeMillis() - 3600000),
        "role",
        null);

    // when & then
    assertThatThrownBy(() -> tokenValidator.validateExpiration(parsedToken))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_EXPIRED.getCode());
  }

  @DisplayName("만료 시간 검증 - 실패: 만료 시간이 null 일 경우 JwtException 이 발생한다")
  @Test
  void validateExpiration_nullExpiration_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti",
        "subject",
        new Date(),
        null,
        "role",
        null);

    // when & then
    assertThatThrownBy(() -> tokenValidator.validateExpiration(parsedToken))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  private JWTClaimsSet createValidClaimsSet() {
    return new JWTClaimsSet.Builder()
        .subject("testUser")
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .jwtID("test-jti")
        .claim("role", "ROLE_USER")
        .build();
  }
}
