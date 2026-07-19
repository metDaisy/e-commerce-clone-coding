package io.github.metdaisy.amaazon.global.security.jwt.provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@NamedInterface("jwt")
@Slf4j
@Component
public class JwtTokenProvider {

  private final String authoritiesKey = "role";
  private final JwtProperties jwtProperties;
  private final JWSSigner signer;
  private final JWSVerifier verifier;
  private final JWSHeader header;

  public JwtTokenProvider(JwtProperties jwtProperties) throws JOSEException {
    this.jwtProperties = jwtProperties;
    byte[] secretBytes = jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8);
    this.signer = new MACSigner(secretBytes);
    this.verifier = new MACVerifier(secretBytes);
    this.header = new JWSHeader(JWSAlgorithm.HS256);
  }

  public String generateAccessToken(Object subject, Object authorities) {
    return buildToken(subject.toString(), authorities.toString(), getAccessTokenExpiration());
  }

  public String generateRefreshToken(Object subject) {
    return buildToken(subject.toString(), null, getRefreshTokenExpiration());
  }

  public Authentication getAuthentication(String token) {
    SignedJWT signedJWT = getOrThrow(() -> SignedJWT.parse(token),
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, Map.of("refreshToken", token)));
    JWTClaimsSet claims = getOrThrow(signedJWT::getJWTClaimsSet,
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "payload 파싱할 수 없습니다."));
    String authClaim = getOrThrow(() -> claims.getStringClaim(authoritiesKey),
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED,
                    "payload 에서 role 을 파싱할 수 없습니다."));

    if (!StringUtils.hasText(authClaim)) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "payload 에서 role 을 찾을 수 없습니다.");
    }

    Collection<? extends GrantedAuthority> authorities = parseAuthorities(authClaim);
    UserDetails principal = new User(claims.getSubject(), "", authorities);
    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  public void validate(String token) {
    SignedJWT signedJWT = getOrThrow(() -> SignedJWT.parse(token),
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, Map.of("refreshToken", token)));

    boolean verified = getOrThrow(() -> signedJWT.verify(verifier),
            () -> new JwtException(JwtErrorCode.VERIFICATION_FAILED));

    if (!verified) {
      throw new JwtException(JwtErrorCode.INVALID_SIGNATURE);
    }

    Date expirationTime = getOrThrow(() -> signedJWT.getJWTClaimsSet().getExpirationTime(),
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "claim 파싱을 할 수 없습니다."));
    if (expirationTime == null) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "토큰 유효기간을 찾을 수 없습니다.");
    }
    if (expirationTime.before(new Date())) {
      throw new JwtException(JwtErrorCode.TOKEN_EXPIRED);
    }
  }

  public String parseJti(String token) {
    JWTClaimsSet claimsSet = getOrThrow(() -> SignedJWT.parse(token).getJWTClaimsSet(),
            () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, Map.of("jwtToken", token)));
    return getOrThrow(claimsSet::getJWTID, () -> new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED,
            Map.of("token", token, "detailMessage", "jti 를 찾을 수 없습니다.")));
  }

  private String buildToken(String subject, String authorities, long expirationMillis) {
    long now = System.currentTimeMillis();
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .subject(subject)
            .issueTime(new Date(now))
            .expirationTime(new Date(now + expirationMillis));

    if (StringUtils.hasText(authorities)) {
      builder.claim(authoritiesKey, authorities);
    }

    SignedJWT signedJWT = new SignedJWT(header, builder.build());
    runOrThrow(() -> signedJWT.sign(signer),
            () -> new JwtException(JwtErrorCode.SIGN_FAILED));
    return signedJWT.serialize();
  }

  private <T> T getOrThrow(JwtCheckedExceptionSupplier<T> supplier,
          Supplier<? extends AmaazonException> exceptionSupplier) {
    try {
      return supplier.get();
    } catch (Exception e) {
      AmaazonException exception = exceptionSupplier.get();
      log.error(exception.toString(), e);
      throw exception;
    }
  }

  private void runOrThrow(JwtCheckedExceptionRunnable runnable,
          Supplier<? extends AmaazonException> exceptionSupplier) {
    try {
      runnable.run();
    } catch (Exception e) {
      AmaazonException exception = exceptionSupplier.get();
      log.error(exception.toString(), e);
      throw exception;
    }
  }

  @FunctionalInterface
  private interface JwtCheckedExceptionSupplier<T> {

    T get() throws Exception;
  }

  @FunctionalInterface
  private interface JwtCheckedExceptionRunnable {

    void run() throws Exception;
  }

  private long getAccessTokenExpiration() {
    return jwtProperties.accessTokenExpiration() * 1000;
  }

  private long getRefreshTokenExpiration() {
    return jwtProperties.refreshTokenExpiration() * 1000;
  }

  private Collection<? extends GrantedAuthority> parseAuthorities(String authClaim) {
    return Arrays.stream(authClaim.split(","))
            .map(String::toUpperCase)
            .map(SimpleGrantedAuthority::new)
            .toList();
  }
}
