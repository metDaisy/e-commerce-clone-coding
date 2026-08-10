package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;

public class CatalogProductException extends AmaazonException {

  public CatalogProductException(AmaazonErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
