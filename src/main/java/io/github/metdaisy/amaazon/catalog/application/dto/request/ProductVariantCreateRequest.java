package io.github.metdaisy.amaazon.catalog.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ProductVariantCreateRequest(
    @NotBlank @Size(max = 255) String displayName,
    @NotNull Map<String, Object> attributes) {

}
