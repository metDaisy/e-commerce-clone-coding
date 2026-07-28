package io.github.metdaisy.amaazon.global.exception.strategy;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * IOException 처리 전략 (SSE 연결 종료 등)
 */
@Slf4j
public class IOExceptionStrategy extends AbstractExceptionResponseStrategy<IOException> {

  @Override
  protected boolean logExceptionMessage() {
    return false;
  }

  @Override
  protected void logException(IOException exception) {
    log.debug("클라이언트 연결 종료로 응답 전송 실패: {}", exception.getMessage());
  }

  @Override
  protected ApiErrorResponse createErrorResponse(
      IOException exception) {
    // IOException은 응답을 반환하지 않음
    return null;
  }

  @Override
  protected HttpStatus getHttpStatus(IOException exception) {
    return HttpStatus.OK;
  }
}
