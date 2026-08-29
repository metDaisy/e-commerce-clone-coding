package io.github.metdaisy.amaazon.catalog.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantArchivedResponse(
    @JsonProperty("productVariantId") UUID id,
    String publicationStatus,
    Instant archivedAt) {

}
