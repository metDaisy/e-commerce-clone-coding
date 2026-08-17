package io.github.metdaisy.amaazon.global.security.jwt.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.model.ParsedToken;

class AuthenticationBuilderTest {

  @DisplayName("Authentication 생성 - 성공: 단일 역할로 Spring Security Authentication 객체가 생성된다")
  @Test
  void buildAuthentication_success() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti-1",
        "2bb8df7f-9478-4d51-b055-496016dd421f",
        new Date(),
        new Date(System.currentTimeMillis() + 3600000),
        "USER",
        null);
    String tokenString = "test.token.string";

    // when
    Authentication authentication = AuthenticationBuilder.buildAuthentication(parsedToken, tokenString);

    // then
    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("2bb8df7f-9478-4d51-b055-496016dd421f");
    assertThat(authentication.getCredentials()).isEqualTo(tokenString);
    assertThat(authentication.getPrincipal()).isInstanceOf(AmaazonPrincipal.class);
    assertThat(authentication.getPrincipal()).isNotInstanceOf(UserDetails.class);
    assertThat(((AmaazonPrincipal) authentication.getPrincipal()).getId().toString())
        .isEqualTo("2bb8df7f-9478-4d51-b055-496016dd421f");
    assertThat(authentication.getAuthorities()).hasSize(1);
    assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
  }

  @DisplayName("Authentication 생성 - 성공: 여러 역할이 콤마로 구분되어 있으면 모두 권한으로 변환된다")
  @Test
  void buildAuthentication_multipleRoles_success() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti-2",
        "2bb8df7f-9478-4d51-b055-496016dd421f",
        new Date(),
        new Date(System.currentTimeMillis() + 3600000),
        "USER,ADMIN",
        null);
    String tokenString = "test.token.string";

    // when
    Authentication authentication = AuthenticationBuilder.buildAuthentication(parsedToken, tokenString);

    // then
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities()).hasSize(2);
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @DisplayName("Authentication 생성 - 실패: 역할이 null 이면 JwtException 이 발생한다")
  @Test
  void buildAuthentication_nullRole_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti-3",
        "user-uuid-789",
        new Date(),
        new Date(System.currentTimeMillis() + 3600000),
        null,
        null);
    String tokenString = "test.token.string";

    // when & then
    assertThatThrownBy(() -> AuthenticationBuilder.buildAuthentication(parsedToken, tokenString))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }

  @DisplayName("Authentication 생성 - 실패: 역할이 빈 문자열이면 JwtException 이 발생한다")
  @Test
  void buildAuthentication_emptyRole_failure() {
    // given
    ParsedToken parsedToken = new ParsedToken(
        "jti-4",
        "user-uuid-000",
        new Date(),
        new Date(System.currentTimeMillis() + 3600000),
        "",
        null);
    String tokenString = "test.token.string";

    // when & then
    assertThatThrownBy(() -> AuthenticationBuilder.buildAuthentication(parsedToken, tokenString))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.TOKEN_PARSE_FAILED.getCode());
  }
}
