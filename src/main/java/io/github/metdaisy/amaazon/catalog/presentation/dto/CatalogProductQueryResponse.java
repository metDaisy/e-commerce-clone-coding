package io.github.metdaisy.amaazon.catalog.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CatalogProductQueryResponse(
    @JsonProperty("catalogProductId") UUID id,
    UUID categoryId,
    String name,
    String description,
    String brand,
    String asin,
    String gtin,
    String upc,
    String ean,
    String isbn,
    List<String> tags,
    Map<String, Object> attributes,
    String publicationStatus,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt,
    List<ProductVariantQueryResponse> variants) {

  public CatalogProductQueryResponse {
    tags = tags == null ? Collections.emptyList() : tags;
    attributes = attributes == null ? Collections.emptyMap() : attributes;
    variants = variants == null ? Collections.emptyList() : variants;
  }
}
