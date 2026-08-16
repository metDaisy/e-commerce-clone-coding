package io.github.metdaisy.amaazon.global.exception.strategy;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * MethodArgumentNotValidException 처리 전략
 */
@Slf4j
public class MethodArgumentNotValidExceptionStrategy
    extends AbstractExceptionResponseStrategy<MethodArgumentNotValidException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected void logExceptionMessage(MethodArgumentNotValidException exception) {
    String detailedErrorLog = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> String.format("필드 [%s] - 입력값: [%s], 원인: [%s]",
            error.getField(),
            error.getRejectedValue(),
            error.getDefaultMessage()))
        .collect(Collectors.joining(" | "));
    log.warn("유효성 검사 실패 (MethodArgumentNotValidException) -> {}", detailedErrorLog);
  }

  @Override
  protected ExceptionResponse createErrorResponse(MethodArgumentNotValidException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(MethodArgumentNotValidException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
