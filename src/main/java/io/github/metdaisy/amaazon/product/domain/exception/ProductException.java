package io.github.metdaisy.amaazon.product.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;

public class ProductException extends AmaazonException {

  public ProductException(AmaazonErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
