package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link ProductVariant}
 */
public record ProductVariantDto(UUID id, Instant createdAt, Instant updatedAt,
                                CatalogProductDto catalogProduct,
                                String displayName,
                                Map<String, Object> attributes,
                                String publicationStatus,
                                Instant archivedAt) {

  public ProductVariantDto {
    attributes = attributes == null ? Collections.emptyMap() : attributes;
  }
}
