package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ProductPublicationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CatalogProductResponse(UUID id, UUID categoryId, String name, String description,
                                     String brand, String asin, String gtin, String upc, String ean,
                                     String isbn, List<String> tags,
                                     Map<String, Object> attributes,
                                     ProductPublicationStatus publicationStatus,
                                     Instant archivedAt, Instant createdAt, Instant updatedAt) {

}
