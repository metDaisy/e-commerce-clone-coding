package io.github.metdaisy.amaazon.global.exception.strategy;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import io.github.metdaisy.amaazon.global.exception.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * ConstraintViolationException 처리 전략
 */
@Slf4j
public class ConstraintViolationExceptionStrategy
    extends AbstractExceptionResponseStrategy<ConstraintViolationException> {

  @Override
  protected boolean logExceptionMessage() {
    return true;
  }

  @Override
  protected void logException(ConstraintViolationException exception) {
    String detailedErrorLog = exception.getConstraintViolations().stream()
        .map(v -> String.format("경로 [%s] - 입력값: [%s], 원인: [%s]",
            v.getPropertyPath(),
            v.getInvalidValue(),
            v.getMessage()))
        .collect(Collectors.joining(" | "));

    boolean isFromController = ViolationExceptionUtils.isFromController(exception);
    if (isFromController) {
      log.warn("제약조건 위반 (ConstraintViolationException) -> {}", detailedErrorLog);
    } else {
      log.error("제약조건 위반 (ConstraintViolationException) -> {}", detailedErrorLog, exception);
    }
  }

  @Override
  protected ApiErrorResponse createErrorResponse(ConstraintViolationException exception) {
    return ApiErrorResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(ConstraintViolationException exception) {
    boolean isFromController = ViolationExceptionUtils.isFromController(exception);
    return isFromController ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
