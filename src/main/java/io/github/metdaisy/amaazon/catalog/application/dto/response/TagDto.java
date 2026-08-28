package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link Tag}
 */
public record TagDto(UUID id, Instant createdAt, String name) {

}
