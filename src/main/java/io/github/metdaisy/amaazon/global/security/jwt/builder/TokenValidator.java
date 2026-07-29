package io.github.metdaisy.amaazon.global.security.jwt.builder;

import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰의 유효성을 검증하는 검증기입니다. 서명 검증, 만료 시간 검증, 클레임 검증을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidator {

  private final SignedTokenFactory signedTokenFactory;

  /**
   * 토큰의 서명과 만료 시간을 모두 검증합니다.
   */
  public void validate(String token, JWSVerifier verifier) {
    SignedJWT signedJWT = parseSignedJWT(token);
    verifySignature(signedJWT, verifier);
    verifyExpiration(signedJWT);
  }

  /**
   * 토큰의 서명만 검증합니다.
   */
  public void validateSignature(String token, JWSVerifier verifier) {
    SignedJWT signedJWT = parseSignedJWT(token);
    verifySignature(signedJWT, verifier);
  }

  /**
   * 토큰의 만료 시간만 검증합니다.
   */
  public void validateExpiration(ParsedToken token) {
    if (token.expirationTime() == null) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "토큰 유효기간을 찾을 수 없습니다.");
    }
    if (token.expirationTime().before(new Date())) {
      throw new JwtException(JwtErrorCode.TOKEN_EXPIRED);
    }
  }

  private SignedJWT parseSignedJWT(String token) {
    try {
      return SignedJWT.parse(token);
    } catch (Exception e) {
      log.error("JWT 토큰 파싱 실패: {}", token, e);
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, Map.of("token", token));
    }
  }

  private void verifySignature(SignedJWT signedJWT, JWSVerifier verifier) {
    boolean verified = signedTokenFactory.verify(signedJWT, verifier);
    if (!verified) {
      log.error("JWT 서명 검증 실패");
      throw new JwtException(JwtErrorCode.VERIFICATION_FAILED);
    }
  }

  private void verifyExpiration(SignedJWT signedJWT) {
    try {
      java.util.Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime == null) {
        throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "claim 파싱을 할 수 없습니다.");
      }
      if (expirationTime.before(new java.util.Date())) {
        throw new JwtException(JwtErrorCode.TOKEN_EXPIRED);
      }
    } catch (JwtException e) {
      throw e;
    } catch (Exception e) {
      log.error("JWT 만료 시간 검증 실패", e);
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "만료 시간 파싱 실패");
    }
  }
}
