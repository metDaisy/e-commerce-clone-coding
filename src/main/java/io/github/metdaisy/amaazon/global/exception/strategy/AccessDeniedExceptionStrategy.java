package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

public class AccessDeniedExceptionStrategy extends AbstractExceptionResponseStrategy<AccessDeniedException> {

  @Override
  protected boolean hasExceptionMessage() {
    return false;
  }

  @Override
  protected ExceptionResponse createErrorResponse(AccessDeniedException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(AccessDeniedException exception) {
    return HttpStatus.FORBIDDEN;
  }
}
