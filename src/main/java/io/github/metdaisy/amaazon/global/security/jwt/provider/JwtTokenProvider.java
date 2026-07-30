package io.github.metdaisy.amaazon.global.security.jwt.provider;

import java.time.Instant;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.JWSVerifier;
import io.github.metdaisy.amaazon.global.security.jwt.builder.AuthenticationBuilder;
import io.github.metdaisy.amaazon.global.security.jwt.builder.SignedTokenFactory;
import io.github.metdaisy.amaazon.global.security.jwt.builder.TokenBuilderFactory;
import io.github.metdaisy.amaazon.global.security.jwt.builder.TokenParser;
import io.github.metdaisy.amaazon.global.security.jwt.builder.TokenValidator;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NamedInterface("jwt")
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final TokenBuilderFactory tokenBuilderFactory;
  private final SignedTokenFactory signedTokenFactory;
  private final TokenValidator tokenValidator;
  private final JWSVerifier verifier;

  public String generateAccessToken(Object subject, Object authorities) {
    var claims = tokenBuilderFactory.buildAccessTokenClaims(subject.toString(), authorities.toString());
    return signedTokenFactory.sign(claims);
  }

  public String generateRefreshToken(Object subject) {
    var claims = tokenBuilderFactory.buildRefreshTokenClaims(subject.toString());
    return signedTokenFactory.sign(claims);
  }

  public Authentication getAuthentication(String token) {
    tokenValidator.validateSignature(token, verifier);
    ParsedToken parsedToken = TokenParser.parse(token);
    tokenValidator.validateExpiration(parsedToken);
    return AuthenticationBuilder.buildAuthentication(parsedToken, token);
  }

  public void validate(String token) {
    tokenValidator.validate(token, verifier);
  }

  public String parseJti(String token) {
    ParsedToken parsedToken = TokenParser.parse(token);
    return TokenParser.parseJti(parsedToken);
  }

  public Instant parseIssueTime(String token) {
    ParsedToken parsedToken = TokenParser.parse(token);
    return TokenParser.parseIssueTime(parsedToken);
  }

  public String generateGuestToken(String provider, String providerId) {
    var claims = tokenBuilderFactory.buildGuestTokenClaims(provider, providerId);
    return signedTokenFactory.sign(claims);
  }

  public String parseProvider(String token, String key) {
    ParsedToken parsedToken = TokenParser.parse(token);
    return TokenParser.parseClaim(parsedToken, key);
  }
}
