package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link CatalogProductTag}
 */
public record CatalogProductTagDto(UUID id, Instant createdAt, TagDto tag) {

}
