package io.github.metdaisy.amaazon.catalog.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductVariantQueryResponse(
    @JsonProperty("variantId") UUID id,
    String displayName,
    Map<String, Object> attributes,
    String publicationStatus,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt) {

}
