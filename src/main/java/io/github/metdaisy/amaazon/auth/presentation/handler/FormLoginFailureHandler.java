package io.github.metdaisy.amaazon.auth.presentation.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.event.IncorrectPasswordEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormLoginFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    publishIncorrectPasswordEvent(request, exception);
    int status = HttpStatus.UNAUTHORIZED.value();
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(response.getWriter(), Map.of("status", status, "message", "로그인 실패하였습니다."));
    log.error("인증 실패하였습니다", exception);
  }

  private void publishIncorrectPasswordEvent(HttpServletRequest request,
      AuthenticationException exception) {
    if (exception instanceof BadCredentialsException) {
      String email = request.getParameter("username");
      eventPublisher.publishEvent(new IncorrectPasswordEvent(email));
    }
  }
}
