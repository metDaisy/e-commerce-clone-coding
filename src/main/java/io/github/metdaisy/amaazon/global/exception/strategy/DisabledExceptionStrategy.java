package io.github.metdaisy.amaazon.global.exception.strategy;

import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
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
  protected ApiErrorResponse createErrorResponse(DisabledException exception) {
    return ApiErrorResponse.from(exception);
  }
}
