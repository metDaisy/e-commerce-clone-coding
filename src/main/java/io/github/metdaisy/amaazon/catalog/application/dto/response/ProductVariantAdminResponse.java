package io.github.metdaisy.amaazon.catalog.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductVariantAdminResponse(
    @JsonProperty("id") UUID productVariantId,
    UUID catalogProductId,
    String displayName,
    Map<String, Object> attributes,
    CatalogStatus publicationStatus,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt) {

}
