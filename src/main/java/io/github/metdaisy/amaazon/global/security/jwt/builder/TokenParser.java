package io.github.metdaisy.amaazon.global.security.jwt.builder;

import java.time.Instant;
import java.util.Map;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰을 파싱하여 ParsedToken으로 변환하는 파서입니다. SignedJWT 파싱 오버헤드를 줄이기 위해 한 번 파싱한 결과를 ParsedToken으로 캐싱합니다.
 */
@Slf4j
@UtilityClass
public class TokenParser {

  /**
   * JWT 토큰을 파싱하여 ParsedToken으로 변환합니다.
   */
  public ParsedToken parse(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return ParsedToken.from(signedJWT.getJWTClaimsSet());
    } catch (Exception e) {
      log.error("JWT 토큰 파싱 실패: {}", token, e);
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, Map.of("token", token));
    }
  }

  /**
   * 토큰에서 JTI(JWT ID)를 추출합니다.
   */
  public String parseJti(ParsedToken token) {
    if (token.jti() == null) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "JTI를 찾을 수 없습니다.");
    }
    return token.jti();
  }

  /**
   * 토큰에서 IssuedAt을 추출합니다.
   */
  public Instant parseIssueTime(ParsedToken token) {
    if (token.issueTime() == null) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "IssuedAt을 찾을 수 없습니다.");
    }
    return Instant.ofEpochSecond(token.issueTime().getTime());
  }

  /**
   * 토큰에서 지정된 키의 클레임 값을 추출합니다.
   */
  public String parseClaim(ParsedToken token, String key) {
    Object claim = token.otherClaims().get(key);
    if (claim == null) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, key + " 클레임을 찾을 수 없습니다.");
    }
    return claim.toString();
  }
}
