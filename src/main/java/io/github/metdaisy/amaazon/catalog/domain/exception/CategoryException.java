package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class CategoryException extends AmaazonException {

  public CategoryException(CategoryErrorCode errorCode) {
    super(errorCode);
  }

  public CategoryException(CategoryErrorCode errorCode, AmaazonExceptionContext context) {
    super(errorCode, context);
  }
}
