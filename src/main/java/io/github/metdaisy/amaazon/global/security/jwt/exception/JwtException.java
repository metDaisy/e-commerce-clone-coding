package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;

public class JwtException extends AmaazonException {

  public JwtException(JwtErrorCode errorCode) {
    super(errorCode);
  }

  public JwtException(JwtErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public JwtException(JwtErrorCode errorCode, String detailMessage) {
    this(errorCode, Map.of("detailMessage", detailMessage));
  }
}
