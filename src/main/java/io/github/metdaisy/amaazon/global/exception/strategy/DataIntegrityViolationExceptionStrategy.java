package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * DataIntegrityViolationException 처리 전략
 */
@Slf4j
public class DataIntegrityViolationExceptionStrategy
    extends AbstractExceptionResponseStrategy<DataIntegrityViolationException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected void logExceptionMessage(DataIntegrityViolationException exception) {
    log.warn("데이터 무결성 제약조건 위반: {}", exception.getMessage());
  }

  @Override
  protected ExceptionResponse createErrorResponse(DataIntegrityViolationException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(DataIntegrityViolationException exception) {
    return HttpStatus.CONFLICT;
  }
}
