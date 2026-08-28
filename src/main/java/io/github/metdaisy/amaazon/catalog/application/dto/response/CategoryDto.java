package io.github.metdaisy.amaazon.catalog.application.dto.response;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Category}
 */
public record CategoryDto(UUID id, UUID parentId, Instant createdAt, Instant updatedAt,
                          String name, Integer depth, List<CategoryDto> children) {

  public CategoryDto {
    children = children == null ? List.of() : children;
  }

}
