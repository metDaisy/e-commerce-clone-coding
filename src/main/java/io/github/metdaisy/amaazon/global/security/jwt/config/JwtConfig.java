package io.github.metdaisy.amaazon.global.security.jwt.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import lombok.RequiredArgsConstructor;

/**
 * JWT 관련 인프라 구조체(Signer, Verifier, Header)를 Spring Bean으로 관리하는 설정 클래스입니다. JwtTokenProvider 및 내부
 * 모듈들이 이 Bean들을 주입받아 사용합니다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtConfig {

  private final JwtProperties jwtProperties;

  @Bean
  public JWSHeader jwsHeader() {
    return new JWSHeader(JWSAlgorithm.HS256);
  }

  @Bean
  public JWSVerifier jwsVerifier() throws JOSEException {
    byte[] secretBytes = jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8);
    return new MACVerifier(secretBytes);
  }
}
