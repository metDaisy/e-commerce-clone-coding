package io.github.metdaisy.amaazon.global.security.jwt.builder;

import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;
import java.util.Arrays;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * ParsedToken을 Spring Security의 Authentication 객체로 변환하는 빌더입니다. JWT 클레임과 Spring Security 인프라를 분리합니다.
 */
@UtilityClass
public class AuthenticationBuilder {

  /**
   * ParsedToken을 Authentication 객체로 변환합니다.
   */
  public Authentication buildAuthentication(ParsedToken token, String tokenString) {
    String authClaim = token.role();
    if (!StringUtils.hasText(authClaim)) {
      throw new JwtException(JwtErrorCode.TOKEN_PARSE_FAILED, "payload에서 role을 찾을 수 없습니다.");
    }

    Collection<? extends GrantedAuthority> authorities = parseAuthorities(authClaim);
    UserDetails principal = new User(token.subject(), "", authorities);
    return new UsernamePasswordAuthenticationToken(principal, tokenString, authorities);
  }

  private Collection<? extends GrantedAuthority> parseAuthorities(String authClaim) {
    return Arrays.stream(authClaim.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .map(auth -> auth.startsWith("ROLE_") ? auth : "ROLE_" + auth)
        .map(SimpleGrantedAuthority::new)
        .toList();
  }
}
