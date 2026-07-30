package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * MethodArgumentTypeMismatchException 처리 전략
 */
@Slf4j
public class MethodArgumentTypeMismatchExceptionStrategy
    extends AbstractExceptionResponseStrategy<MethodArgumentTypeMismatchException> {

  @Override
  protected boolean logExceptionMessage() {
    return true;
  }

  @Override
  protected ApiErrorResponse createErrorResponse(MethodArgumentTypeMismatchException exception) {
    return ApiErrorResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(MethodArgumentTypeMismatchException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
