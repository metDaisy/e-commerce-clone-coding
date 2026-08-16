package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class CatalogProductException extends AmaazonException {

  public CatalogProductException(CatalogProductErrorCode errorCode) {
    super(errorCode);
  }

  public CatalogProductException(
      CatalogProductErrorCode errorCode,
      AmaazonExceptionContext context
  ) {
    super(errorCode, context);
  }
}
