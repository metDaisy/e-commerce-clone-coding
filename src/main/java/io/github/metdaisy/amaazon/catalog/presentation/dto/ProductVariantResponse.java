package io.github.metdaisy.amaazon.catalog.presentation.dto;

import java.util.Collections;
import java.util.Map;

public record ProductVariantResponse(String displayName, Map<String, Object> attributes) {

  public ProductVariantResponse {
    attributes = attributes == null ? Collections.emptyMap() : attributes;
  }
}
