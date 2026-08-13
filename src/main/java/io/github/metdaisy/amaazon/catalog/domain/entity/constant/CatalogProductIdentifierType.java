package io.github.metdaisy.amaazon.catalog.domain.entity.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;

public enum CatalogProductIdentifierType {
  ASIN, GTIN, UPC, EAN, ISBN;

  @JsonCreator
  public static CatalogProductIdentifierType from(String value) {
    return Stream.of(values())
        .filter(it -> it.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid identifier: " + value));
  }
}
