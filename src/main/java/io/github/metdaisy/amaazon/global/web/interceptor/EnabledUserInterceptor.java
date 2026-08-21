package io.github.metdaisy.amaazon.global.web.interceptor;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class EnabledUserInterceptor implements HandlerInterceptor {

  private final UserQueryApi userQueryApi;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod) || !requiresEnabledUser(handlerMethod)) {
      return true;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof AmaazonPrincipal principal)) {
      throw new AuthenticationCredentialsNotFoundException("활성 사용자 인증 정보가 필요합니다.");
    }

    userQueryApi.requireEnabled(principal.getId());
    return true;
  }

  private boolean requiresEnabledUser(HandlerMethod handlerMethod) {
    return handlerMethod.hasMethodAnnotation(RequireEnabledUser.class)
        || handlerMethod.getBeanType().isAnnotationPresent(RequireEnabledUser.class);
  }
}
