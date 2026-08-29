package io.github.metdaisy.amaazon.catalog.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record CatalogArchivedResponse(@JsonProperty("catalogProductId") UUID id,
                                      String publicationStatus,
                                      Instant archivedAt, Instant updatedAt) {

}
