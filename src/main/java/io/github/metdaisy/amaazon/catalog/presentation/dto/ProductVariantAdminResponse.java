package io.github.metdaisy.amaazon.catalog.presentation.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductVariantAdminResponse(
    UUID id,
    UUID catalogProductId,
    String displayName,
    Map<String, Object> attributes,
    String publicationStatus,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt) {

}
