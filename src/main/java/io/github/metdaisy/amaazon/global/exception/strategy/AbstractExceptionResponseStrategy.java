package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 예외 응답 전략의 추상 기반 클래스
 */
@Slf4j
public abstract class AbstractExceptionResponseStrategy<T extends Exception>
    implements ExceptionResponseStrategy<T> {

  /**
   * 예외 메시지를 반환하는지 여부
   */
  protected abstract boolean hasExceptionMessage();

  /**
   * API 응답 오류 객체를 생성합니다.
   */
  protected abstract ExceptionResponse createErrorResponse(T exception);

  protected void logExceptionMessage(T exception) {
    if (hasExceptionMessage()) {
      log.warn("exceptionType={}, systemMessage={}",
          exception.getClass().getSimpleName(), exception.getMessage());
    }
  }

  protected HttpStatus getHttpStatus(T exception) {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /**
   * 전체 응답 엔티티를 생성합니다.
   */
  @Override
  public ResponseEntity<ExceptionResponse> buildResponse(T exception) {
    logExceptionMessage(exception);
    return ResponseEntity
        .status(getHttpStatus(exception))
        .body(createErrorResponse(exception));
  }

}
