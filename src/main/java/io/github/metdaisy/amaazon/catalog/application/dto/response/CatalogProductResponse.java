package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ProductPublicationStatus;
import java.time.Instant;
import java.util.UUID;

public record CatalogProductResponse(UUID id, UUID categoryId, String name, String description,
                                     String brand, ProductPublicationStatus publicationStatus,
                                     Instant createdAt) {

}
