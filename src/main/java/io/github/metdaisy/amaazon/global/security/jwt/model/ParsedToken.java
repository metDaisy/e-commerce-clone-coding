package io.github.metdaisy.amaazon.global.security.jwt.model;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import com.nimbusds.jwt.JWTClaimsSet;

/**
 * 파싱된 JWT 클레임 정보를 담는 불변 Record 클래스입니다. 토큰 파싱 오버헤드를 줄이기 위해 SignedJWT를 한 번만 파싱하여 이 객체로 변환합니다.
 */
public record ParsedToken(
    String jti,
    String subject,
    Date issueTime,
    Date expirationTime,
    String role,
    Map<String, Object> otherClaims) {

  /**
   * JWTClaimsSet으로부터 ParsedToken을 생성합니다.
   */
  public static ParsedToken from(JWTClaimsSet claims) throws ParseException {
    return new ParsedToken(
        claims.getJWTID(),
        claims.getSubject(),
        claims.getIssueTime(),
        claims.getExpirationTime(),
        claims.getStringClaim("role"),
        claims.getClaims());
  }
}
