package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * 일반 Exception 처리 전략
 */
@Slf4j
public class ExceptionStrategy extends AbstractExceptionResponseStrategy<Exception> {

  @Override
  public boolean hasExceptionMessage() {
    return false;
  }

  @Override
  public void logExceptionMessage(Exception exception) {
    log.error("Unexpected exception", exception);
  }

  @Override
  public ExceptionResponse createErrorResponse(Exception exception) {
    return ExceptionResponse.of(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "SYSTEM-001",
        "요청을 처리하지 못했습니다.",
        null);
  }

}
