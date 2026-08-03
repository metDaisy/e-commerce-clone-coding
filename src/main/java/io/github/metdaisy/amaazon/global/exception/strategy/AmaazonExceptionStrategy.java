package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import io.github.metdaisy.amaazon.global.exception.util.ErrorTypeResolver;

/**
 * AmaazonException 처리 전략
 */
public class AmaazonExceptionStrategy extends AbstractExceptionResponseStrategy<AmaazonException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected ApiErrorResponse createErrorResponse(AmaazonException exception) {
    return ApiErrorResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(AmaazonException exception) {
    return ErrorTypeResolver.resolve(exception.getErrorType());
  }
}
