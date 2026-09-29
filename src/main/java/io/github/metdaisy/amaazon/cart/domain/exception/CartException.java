package io.github.metdaisy.amaazon.cart.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class CartException extends AmaazonException {

  public CartException(CartErrorCode errorCode) {
    super(errorCode);
  }

  public CartException(CartErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
