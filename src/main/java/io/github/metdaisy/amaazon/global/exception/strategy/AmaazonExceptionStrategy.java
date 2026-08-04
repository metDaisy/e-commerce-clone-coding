package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
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
  protected ExceptionResponse createErrorResponse(AmaazonException exception) {
    return ExceptionResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(AmaazonException exception) {
    return ErrorTypeResolver.resolve(exception.getErrorType());
  }
}
