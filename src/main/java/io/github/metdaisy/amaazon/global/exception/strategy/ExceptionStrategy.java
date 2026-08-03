package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

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
  public ApiErrorResponse createErrorResponse(Exception exception) {
    return new ApiErrorResponse("INTERNAL_SERVER_ERROR",
        "서버 내부 에러가 발생했습니다.", null);
  }

}
