package io.github.metdaisy.amaazon.address.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class AddressException extends AmaazonException {

  public AddressException(AddressErrorCode errorCode) {
    super(errorCode);
  }

  public AddressException(AddressErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
