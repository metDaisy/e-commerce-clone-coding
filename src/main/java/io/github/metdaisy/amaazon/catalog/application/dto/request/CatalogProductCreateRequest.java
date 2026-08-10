package io.github.metdaisy.amaazon.catalog.application.dto.request;

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
    @Size(max = 255) String brand,
    @Size(max = 50) String asin,
    @Size(max = 50) String gtin,
    @Size(max = 50) String upc,
    @Size(max = 50) String ean,
    @Size(max = 50) String isbn,
    Set<String> tags,
    Map<String, Object> attributes
) {

}
