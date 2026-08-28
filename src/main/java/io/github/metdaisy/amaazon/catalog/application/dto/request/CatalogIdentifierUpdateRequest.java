package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.application.validator.ValidCatalogIdentifierKeys;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CatalogIdentifierUpdateRequest(
    @ValidCatalogIdentifierKeys
    Map<String, @Size(max = 50) String> identifiers) {
}
