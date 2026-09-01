package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO returned after creating, updating, or archiving a {@link CatalogProduct}.
 */
public record CatalogProductCommandDto(UUID id, Instant createdAt,
                                       Instant updatedAt,
                                       CategoryDto category,
                                       List<CatalogProductTagDto> tags,
                                       String name, String description,
                                       String brand, String asin,
                                       String gtin, String upc,
                                       String ean, String isbn,
                                       Map<String, Object> attributes,
                                       String publicationStatus, Instant archivedAt) {

  public CatalogProductCommandDto {
    tags = tags == null ? Collections.emptyList() : tags;
    attributes = attributes == null ? Collections.emptyMap() : attributes;
  }
}
