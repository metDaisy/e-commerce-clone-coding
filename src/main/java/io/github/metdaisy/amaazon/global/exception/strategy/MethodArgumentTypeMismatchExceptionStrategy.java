package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * MethodArgumentTypeMismatchException 처리 전략
 */
@Slf4j
public class MethodArgumentTypeMismatchExceptionStrategy
    extends AbstractExceptionResponseStrategy<MethodArgumentTypeMismatchException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected ExceptionResponse createErrorResponse(MethodArgumentTypeMismatchException exception) {
    return ExceptionResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(MethodArgumentTypeMismatchException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
