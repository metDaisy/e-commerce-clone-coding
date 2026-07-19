package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;

public class AuthException extends AmaazonException {

  public AuthException(AuthErrorCode errorCode) {
    super(errorCode);
  }

  public AuthException(AuthErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}