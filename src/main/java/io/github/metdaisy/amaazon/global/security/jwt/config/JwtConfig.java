package io.github.metdaisy.amaazon.global.security.jwt.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 관련 인프라 구조체(Signer, Verifier, Header)를 Spring Bean으로 관리하는 설정 클래스입니다. JwtTokenProvider 및 내부
 * 모듈들이 이 Bean들을 주입받아 사용합니다.
 */
@Configuration
@EnableConfigurationProperties({JwtTokenExpiration.class, JwtCookieProperties.class})
public class JwtConfig {

  private final String secretKey;

  public JwtConfig(@Value("${amaazon.jwt.secret-key}") String secretKey) {
    this.secretKey = secretKey;
  }

  @Bean
  public JWSHeader jwsHeader() {
    return new JWSHeader(JWSAlgorithm.HS256);
  }

  @Bean
  public JWSSigner jwsSigner() throws KeyLengthException {
    byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    return new MACSigner(secretBytes);
  }

  @Bean
  public JWSVerifier jwsVerifier() throws JOSEException {
    byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    return new MACVerifier(secretBytes);
  }
}
