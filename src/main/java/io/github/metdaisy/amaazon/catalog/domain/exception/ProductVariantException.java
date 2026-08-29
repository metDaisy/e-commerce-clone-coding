package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class ProductVariantException extends AmaazonException {

  public ProductVariantException(ProductVariantErrorCode errorCode) {
    super(errorCode);
  }

  public ProductVariantException(ProductVariantErrorCode errorCode,
      AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
