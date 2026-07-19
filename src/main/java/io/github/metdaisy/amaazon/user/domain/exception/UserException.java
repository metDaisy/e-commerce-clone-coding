package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;

public class UserException extends AmaazonException {

  public UserException(UserErrorCode errorCode) {
    super(errorCode);
  }

  public UserException(UserErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}