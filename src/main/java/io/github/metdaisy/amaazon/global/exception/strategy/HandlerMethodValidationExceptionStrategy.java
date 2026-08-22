package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * HandlerMethodValidationException 처리 전략
 */
public class HandlerMethodValidationExceptionStrategy
    extends AbstractExceptionResponseStrategy<HandlerMethodValidationException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected ExceptionResponse createErrorResponse(HandlerMethodValidationException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(HandlerMethodValidationException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
