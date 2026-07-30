package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;

class TokenParserTest {

  private final String secretKey = "testSecretKeyForJwtSigningMustBeLongEnoughForHS256";
  private JWSHeader header;
  private JWSSigner signer;

  @BeforeEach
  void setUp() throws Exception {
    header = new JWSHeader(JWSAlgorithm.HS256);
    signer = new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  @DisplayName("JWT 파싱 - 성공: 유효한 토큰으로부터 모든 클레임을 파싱한다")
  @Test
  void parse_success() throws Exception {
    // given
    String subject = "testUser";
    String role = "ROLE_USER";
    String jti = UUID.randomUUID().toString();
    long currentTimeSec = System.currentTimeMillis() / 1000 * 1000;
    Date now = new Date(currentTimeSec);
    Date expiration = new Date(currentTimeSec + 3600000);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(now)
        .expirationTime(expiration)
        .claim("role", role)
        .claim("provider", "google")
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(signer);
    String token = signedJWT.serialize();

    // when
    ParsedToken parsedToken = TokenParser.parse(token);

    // then
    assertThat(parsedToken).isNotNull();
    assertThat(parsedToken.jti()).isEqualTo(jti);
    assertThat(parsedToken.subject()).isEqualTo(subject);
    assertThat(parsedToken.role()).isEqualTo(role);
    assertThat(parsedToken.issueTime()).isEqualTo(now);
    assertThat(parsedToken.expirationTime()).isEqualTo(expiration);
    assertThat(parsedToken.otherClaims().get("provider")).isEqualTo("google");
  }

  @DisplayName("JTI 파싱 - 성공: 토큰에서 JTI 를 추출한다")
  @Test
  void parseJti_success() throws Exception {
    // given
    String jti = UUID.randomUUID().toString();
    ParsedToken parsedToken = createParsedToken(jti, "subject");

    // when
    String parsedJti = TokenParser.parseJti(parsedToken);

    // then
    assertThat(parsedJti).isEqualTo(jti);
  }

  @DisplayName("JTI 파싱 - 실패: JTI 가 null 이면 JwtException 이 발생한다")
  @Test
  void parseJti_null_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken(null, "subject", new Date(), new Date(), "role", null);

    // when & then
    assertThatThrownBy(() -> TokenParser.parseJti(parsedToken))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  @DisplayName("IssuedAt 파싱 - 성공: 토큰에서 IssuedAt 을 추출한다")
  @Test
  void parseIssueTime_success() throws Exception {
    // given
    String jti = UUID.randomUUID().toString();
    long currentTimeSec = System.currentTimeMillis() / 1000 * 1000;
    Date now = new Date(currentTimeSec);
    ParsedToken parsedToken = createParsedToken(jti, "subject", now);

    // when
    Instant issueTime = TokenParser.parseIssueTime(parsedToken);

    // then
    assertThat(issueTime).isEqualTo(now.toInstant());
  }

  @DisplayName("IssuedAt 파싱 - 실패: IssueTime 이 null 이면 JwtException 이 발생한다")
  @Test
  void parseIssueTime_null_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken("jti", "subject", null, new Date(), "role", null);

    // when & then
    assertThatThrownBy(() -> TokenParser.parseIssueTime(parsedToken))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  @DisplayName("클레임 파싱 - 성공: 토큰에서 지정된 키의 클레임을 추출한다")
  @Test
  void parseClaim_success() throws Exception {
    // given
    String jti = UUID.randomUUID().toString();
    java.util.Map<String, Object> otherClaims = new java.util.HashMap<>();
    otherClaims.put("provider", "google");
    ParsedToken parsedToken = new ParsedToken(jti, "subject", new Date(System.currentTimeMillis() / 1000 * 1000),
        new Date(System.currentTimeMillis() / 1000 * 1000), "role", otherClaims);

    // when
    String provider = TokenParser.parseClaim(parsedToken, "provider");

    // then
    assertThat(provider).isEqualTo("google");
  }

  @DisplayName("클레임 파싱 - 실패: 존재하지 않는 키로 클레임을 조회하면 JwtException 이 발생한다")
  @Test
  void parseClaim_nonExistentKey_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken("jti", "subject", new Date(), new Date(), "role", null);

    // when & then
    assertThatThrownBy(() -> TokenParser.parseClaim(parsedToken, "nonExistent"))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  @DisplayName("JWT 파싱 - 실패: 유효하지 않은 토큰 형식일 경우 JwtException 이 발생한다")
  @Test
  void parse_invalidToken_failure() {
    // when & then
    assertThatThrownBy(() -> TokenParser.parse("invalid.token.format"))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  private ParsedToken createParsedToken(String jti, String subject) throws Exception {
    return createParsedToken(jti, subject, new Date(System.currentTimeMillis() / 1000 * 1000));
  }

  private ParsedToken createParsedToken(String jti, String subject, Date issueTime) throws Exception {
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .jwtID(jti)
        .subject(subject)
        .issueTime(issueTime)
        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
        .claim("role", "ROLE_USER")
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(signer);
    return TokenParser.parse(signedJWT.serialize());
  }
}
