package io.github.metdaisy.amaazon.catalog.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import java.time.Instant;
import java.util.UUID;

public record CatalogArchivedResponse(@JsonProperty("catalogProductId") UUID id,
                                      CatalogStatus publicationStatus,
                                      Instant archivedAt, Instant updatedAt) {

}
