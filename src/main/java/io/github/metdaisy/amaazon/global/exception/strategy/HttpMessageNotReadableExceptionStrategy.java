package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpMessageNotReadableException 처리 전략
 */
@Slf4j
public class HttpMessageNotReadableExceptionStrategy
    extends AbstractExceptionResponseStrategy<HttpMessageNotReadableException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected void logExceptionMessage(HttpMessageNotReadableException exception) {
    log.warn("요청 본문을 읽을 수 없음 (HttpMessageNotReadableException) -> {}", exception.getMessage());
  }

  @Override
  protected ApiErrorResponse createErrorResponse(HttpMessageNotReadableException exception) {
    return ApiErrorResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(HttpMessageNotReadableException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
