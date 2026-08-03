package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
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
  protected ApiErrorResponse createErrorResponse(MissingServletRequestParameterException exception) {
    return ApiErrorResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(MissingServletRequestParameterException exception) {
    return HttpStatus.BAD_REQUEST;
  }
}
