package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;

public class BindExceptionStrategy extends AbstractExceptionResponseStrategy<BindException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected ExceptionResponse createErrorResponse(BindException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(BindException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
