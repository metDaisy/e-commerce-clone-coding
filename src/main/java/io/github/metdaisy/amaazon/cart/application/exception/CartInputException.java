package io.github.metdaisy.amaazon.cart.application.exception;

import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;

public class CartInputException extends AmaazonException {

  public CartInputException() {
    super(CartErrorCode.CART_INVALID_INPUT);
  }
}
