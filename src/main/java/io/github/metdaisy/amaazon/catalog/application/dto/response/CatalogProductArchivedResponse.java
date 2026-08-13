package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ProductPublicationStatus;
import java.time.Instant;
import java.util.UUID;

public record CatalogProductArchivedResponse(UUID id, ProductPublicationStatus publicationStatus,
                                             Instant archivedAt, Instant updatedAt) {

}
