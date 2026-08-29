package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.application.validator.ValidCatalogIdentifierKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CatalogProductCreateRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 255) String name,
    @NotBlank String description,
    @NotBlank @Size(max = 255) String brand,
    Set<String> tags,
    Map<String, Object> attributes,
    @ValidCatalogIdentifierKeys
    Map<String, @Size(max = 50) String> identifiers
) {

}
