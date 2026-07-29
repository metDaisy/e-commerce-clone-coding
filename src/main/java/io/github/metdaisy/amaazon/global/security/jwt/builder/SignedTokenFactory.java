package io.github.metdaisy.amaazon.global.security.jwt.builder;

import org.springframework.stereotype.Component;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWTClaimsSet을 서명된 SignedJWT로 변환하는 팩토리입니다. 서명 및 직렬화 로직을 캡슐화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignedTokenFactory {

  private final JWSHeader header;
  private final JWSSigner signer;

  /**
   * 클레임셋을 서명하여 JWT 문자열로 직렬화합니다.
   */
  public String sign(JWTClaimsSet claimsSet) {
    try {
      SignedJWT signedJWT = new SignedJWT(header, claimsSet);
      signedJWT.sign(signer);
      return signedJWT.serialize();
    } catch (Exception e) {
      log.error("JWT 서명 실패", e);
      throw new JwtException(JwtErrorCode.SIGN_FAILED);
    }
  }

  /**
   * 서명된 JWT의 유효성을 검증합니다.
   */
  public boolean verify(SignedJWT signedJWT, JWSVerifier verifier) {
    try {
      return signedJWT.verify(verifier);
    } catch (Exception e) {
      log.error("JWT 검증 실패", e);
      return false;
    }
  }
}
