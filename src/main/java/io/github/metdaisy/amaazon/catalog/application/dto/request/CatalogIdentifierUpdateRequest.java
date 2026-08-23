package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CatalogIdentifierUpdateRequest(
    Map<CatalogIdentifierType, @Size(max = 50) String> identifiers) {
}
