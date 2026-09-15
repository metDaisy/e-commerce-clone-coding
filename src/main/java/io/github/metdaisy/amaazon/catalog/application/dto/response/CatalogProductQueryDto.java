package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned when querying a {@link CatalogProduct} with all of its variants.
 */
@Getter
@Builder
public final class CatalogProductQueryDto {

  private final UUID id;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final CategoryDto category;
  @Builder.Default
  private final List<CatalogProductTagDto> tags = Collections.emptyList();
  private final String name;
  private final String description;
  private final String brand;
  private final String asin;
  private final String gtin;
  private final String upc;
  private final String ean;
  private final String isbn;
  @Builder.Default
  private final Map<String, Object> attributes = Collections.emptyMap();
  private final String publicationStatus;
  private final Instant archivedAt;
  @Setter
  @Builder.Default
  private List<ProductVariantDto> variants = Collections.emptyList();

}
