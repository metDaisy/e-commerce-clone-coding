package io.github.metdaisy.amaazon.global.exception;

import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionResponseStrategy;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class SecurityExceptionHandler {

  private final ExceptionStrategyFactory strategyFactory;

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ExceptionResponse> handle(AuthenticationException ex) {
    ExceptionResponseStrategy<Exception> strategy = strategyFactory.getStrategy(ex.getClass());
    return strategy.buildResponse(ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ExceptionResponse> handle(AccessDeniedException ex) {
    ExceptionResponseStrategy<Exception> strategy = strategyFactory.getStrategy(ex.getClass());
    return strategy.buildResponse(ex);
  }
}
