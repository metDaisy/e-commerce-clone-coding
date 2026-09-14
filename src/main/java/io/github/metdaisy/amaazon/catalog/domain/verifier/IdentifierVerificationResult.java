package io.github.metdaisy.amaazon.catalog.domain.verifier;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import java.util.Collections;
import java.util.Map;

public record IdentifierVerificationResult(
    boolean valid,
    String code,
    String message,
    Map<String, Object> logDetails
) {

  public IdentifierVerificationResult {
    logDetails = logDetails == null || logDetails.isEmpty()
        ? Collections.emptyMap()
        : Map.copyOf(logDetails);
  }

  public static IdentifierVerificationResult success() {
    return new IdentifierVerificationResult(true, null, null, Collections.emptyMap());
  }

  public static IdentifierVerificationResult failure(CatalogProductErrorCode errorCode) {
    return failure(errorCode, Collections.emptyMap());
  }

  public static IdentifierVerificationResult failure(
      CatalogProductErrorCode errorCode, Map<String, Object> logDetails) {
    return new IdentifierVerificationResult(
        false, errorCode.getCode(), errorCode.getMessage(), logDetails);
  }
}
