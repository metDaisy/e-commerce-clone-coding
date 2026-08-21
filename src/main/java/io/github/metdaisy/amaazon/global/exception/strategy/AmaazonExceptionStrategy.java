package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import io.github.metdaisy.amaazon.global.exception.util.ErrorTypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * AmaazonException response strategy.
 */
@Slf4j
public class AmaazonExceptionStrategy extends AbstractExceptionResponseStrategy<AmaazonException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected void logExceptionMessage(AmaazonException exception) {
    log.warn("domain exception occurred: code={}, clientMessage={}, systemMessage={}",
        exception.getCode(), exception.getClientMessage(), exception.getSystemMessage());
  }

  @Override
  protected ExceptionResponse createErrorResponse(AmaazonException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(AmaazonException exception) {
    return ErrorTypeResolver.resolve(exception.getErrorType());
  }
}
