package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class JwtException extends AmaazonException {

  public JwtException(JwtErrorCode errorCode) {
    super(errorCode);
  }

  public JwtException(JwtErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
