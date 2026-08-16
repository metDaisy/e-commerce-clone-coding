package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class UserException extends AmaazonException {

  public UserException(UserErrorCode errorCode) {
    super(errorCode);
  }

  public UserException(UserErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
