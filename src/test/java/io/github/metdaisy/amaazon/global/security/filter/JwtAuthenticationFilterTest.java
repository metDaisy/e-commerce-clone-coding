package io.github.metdaisy.amaazon.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private BlacklistRegistry blacklistRegistry;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Captor
  private ArgumentCaptor<Authentication> authenticationCaptor;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(tokenProvider, blacklistRegistry);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @DisplayName("필터 실행 - 성공: Authorization 헤더가 없으면 인증 없이 다음 필터로 전달된다")
  @Test
  void doFilter_noAuthHeader_passesThrough() throws ServletException, IOException {
    given(request.getHeader("Authorization")).willReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @DisplayName("필터 실행 - 성공: Bearer 토큰이 유효하면 SecurityContext 에 인증이 설정된다")
  @Test
  void doFilter_validToken_setsAuthentication() throws ServletException, IOException {
    String token = "valid.bearer.token";
    given(request.getHeader("Authorization")).willReturn("Bearer " + token);
    willDoNothing().given(tokenProvider).validate(token);

    Authentication mockAuth = createMockAuthentication();
    given(tokenProvider.getAuthentication(token)).willReturn(mockAuth);
    given(tokenProvider.parseJti(token)).willReturn("test-jti");
    given(tokenProvider.parseIssueTime(token)).willReturn(Instant.now());
    given(blacklistRegistry.isBlacklisted(any(), any(), any())).willReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(mockAuth);
  }

  @DisplayName("필터 실행 - 실패: 유효하지 않은 토큰일 경우 JwtException 이 발생한다")
  @Test
  void doFilter_invalidToken_throwsException() throws ServletException, IOException {
    String token = "invalid.token";
    given(request.getHeader("Authorization")).willReturn("Bearer " + token);
    willThrow(new JwtException(JwtErrorCode.VERIFICATION_FAILED)).given(tokenProvider).validate(token);

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.VERIFICATION_FAILED.getCode());
    then(filterChain).should(never()).doFilter(any(), any());
  }

  @DisplayName("필터 실행 - 실패: 블랙리스트에 등록된 토큰일 경우 JwtException 이 발생한다")
  @Test
  void doFilter_blacklistedToken_throwsException() throws ServletException, IOException {
    String token = "blacklisted.token";
    given(request.getHeader("Authorization")).willReturn("Bearer " + token);
    willDoNothing().given(tokenProvider).validate(token);

    Authentication mockAuth = createMockAuthentication();
    given(tokenProvider.getAuthentication(token)).willReturn(mockAuth);
    given(tokenProvider.parseJti(token)).willReturn("blacklisted-jti");
    given(tokenProvider.parseIssueTime(token)).willReturn(Instant.now());
    given(blacklistRegistry.isBlacklisted(any(), any(), any())).willReturn(true);

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(JwtException.class)
        .hasFieldOrPropertyWithValue("code", JwtErrorCode.BLACKLISTED_TOKEN.getCode());
    then(filterChain).should(never()).doFilter(any(), any());
  }

  @DisplayName("필터 실행 - 성공: Bearer 가 아닌 Authorization 헤더는 토큰 없이 처리된다")
  @Test
  void doFilter_nonBearerHeader_passesThrough() throws ServletException, IOException {
    given(request.getHeader("Authorization")).willReturn("Basic dXNlcjpwYXNz");

    filter.doFilterInternal(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  private Authentication createMockAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        UUID.randomUUID().toString(), "token", Collections.emptyList());
  }
}
