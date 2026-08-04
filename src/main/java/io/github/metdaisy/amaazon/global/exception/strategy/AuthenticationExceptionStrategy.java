package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

public class AuthenticationExceptionStrategy extends AbstractExceptionResponseStrategy<AuthenticationException> {

  @Override
  protected boolean hasExceptionMessage() {
    return false;
  }

  @Override
  protected ExceptionResponse createErrorResponse(AuthenticationException exception) {
    return ExceptionResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(AuthenticationException exception) {
    return HttpStatus.UNAUTHORIZED;
  }
}
