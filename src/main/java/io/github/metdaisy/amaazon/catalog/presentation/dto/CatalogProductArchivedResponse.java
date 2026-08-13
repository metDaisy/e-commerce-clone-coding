package io.github.metdaisy.amaazon.catalog.presentation.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CatalogProductArchivedResponse {

  private final UUID id;
  private final String publicationStatus = "ARCHIVED";
  private final Instant archivedAt = Instant.now();
  private final Instant updatedAt = Instant.now();
}
