package io.github.metdaisy.amaazon.global.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Order(1)
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_KEY = "requestId";
  private static final String REQUEST_METHOD_KEY = "requestMethod";
  private static final String REQUEST_URI_KEY = "requestUri";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = UUID.randomUUID().toString();
    MDC.put(REQUEST_ID_KEY, requestId);
    MDC.put(REQUEST_METHOD_KEY, request.getMethod());
    MDC.put(REQUEST_URI_KEY, request.getRequestURI());

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.equals("/health")
        || path.startsWith("/swagger");
  }
}
