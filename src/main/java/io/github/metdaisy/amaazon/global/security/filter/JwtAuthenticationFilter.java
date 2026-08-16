package io.github.metdaisy.amaazon.global.security.filter;

import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtErrorCode;
import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtException;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider provider;
  private final BlacklistRegistry registry;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain) throws ServletException, IOException {
    String token = parseToken(request);
    if (!StringUtils.hasText(token)) {
      filterChain.doFilter(request, response);
      return;
    }
    provider.validate(token);
    Authentication authToken = provider.getAuthentication(token);
    UUID userId = UUID.fromString(authToken.getName());
    String jti = provider.parseJti(token);
    Instant issueTime = provider.parseIssueTime(token);
    if (registry.isBlacklisted(jti, userId, issueTime)) {
      throw new JwtException(JwtErrorCode.BLACKLISTED_TOKEN,
          AmaazonExceptionContext.logDetails(Map.of("token", token)));
    }
    SecurityContextHolder.getContext().setAuthentication(authToken);
    filterChain.doFilter(request, response);
  }

  private String parseToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
