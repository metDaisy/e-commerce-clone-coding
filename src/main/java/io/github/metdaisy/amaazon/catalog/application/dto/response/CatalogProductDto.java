package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link CatalogProduct}
 */
public record CatalogProductDto(UUID id, Instant createdAt,
                                Instant updatedAt,
                                CategoryDto category, List<CatalogProductTagDto> tags,
                                String name, String description,
                                String brand, String asin,
                                String gtin, String upc,
                                String ean, String isbn,
                                Map<String, Object> attributes,
                                String publicationStatus, Instant archivedAt,
                                List<ProductVariantDto> variants
) {

  public CatalogProductDto {
    tags = tags == null ? Collections.emptyList() : tags;
    attributes = attributes == null ? Collections.emptyMap() : attributes;
    variants = variants == null ? Collections.emptyList() : variants;
  }

  public CatalogProductDto withVariants(List<ProductVariantDto> variants) {
    return new CatalogProductDto(id, createdAt, updatedAt, category, tags, name, description,
        brand, asin, gtin, upc, ean, isbn, attributes, publicationStatus, archivedAt, variants);
  }
}
