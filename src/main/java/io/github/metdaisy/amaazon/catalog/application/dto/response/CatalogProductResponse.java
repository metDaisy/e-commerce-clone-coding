package io.github.metdaisy.amaazon.catalog.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CatalogProductResponse(@JsonProperty("catalogProductId") UUID id,
                                     UUID categoryId, String name, String description,
                                     String brand, String asin, String gtin, String upc, String ean,
                                     String isbn, List<String> tags,
                                     Map<String, Object> attributes,
                                     CatalogStatus publicationStatus,
                                     Instant archivedAt, Instant createdAt) {

}
