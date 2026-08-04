package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;

@Slf4j
public class DisabledExceptionStrategy extends AbstractExceptionResponseStrategy<DisabledException> {

  @Override
  protected boolean hasExceptionMessage() {
    return true;
  }

  @Override
  protected void logExceptionMessage(DisabledException exception) {
    log.warn("탈퇴한 유저입니다.", exception);
  }

  @Override
  protected ExceptionResponse createErrorResponse(DisabledException exception) {
    return ExceptionResponse.from(exception);
  }

  @Override
  protected HttpStatus getHttpStatus(DisabledException exception) {
    return HttpStatus.NOT_FOUND;
  }
}
