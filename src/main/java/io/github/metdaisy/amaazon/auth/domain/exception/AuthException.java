package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class AuthException extends AmaazonException {

  public AuthException(AuthErrorCode errorCode) {
    super(errorCode);
  }

  public AuthException(AuthErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
