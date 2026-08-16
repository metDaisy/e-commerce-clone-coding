package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * MissingServletRequestParameterException 처리 전략
 */
@Slf4j
public class MissingServletRequestParameterExceptionStrategy
    extends AbstractExceptionResponseStrategy<MissingServletRequestParameterException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected ExceptionResponse createErrorResponse(MissingServletRequestParameterException exception) {
    return ExceptionResponse.from(exception, getHttpStatus(exception));
  }

  @Override
  protected HttpStatus getHttpStatus(MissingServletRequestParameterException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
