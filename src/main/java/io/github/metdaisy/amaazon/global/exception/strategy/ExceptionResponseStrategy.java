package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.ResponseEntity;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;

/**
 * 예외 유형별 응답 생성 전략 인터페이스
 */
public interface ExceptionResponseStrategy<T extends Exception> {

  /**
   * 전체 응답 엔티티를 생성합니다.
   */
  ResponseEntity<ApiErrorResponse> buildResponse(T exception);

}
