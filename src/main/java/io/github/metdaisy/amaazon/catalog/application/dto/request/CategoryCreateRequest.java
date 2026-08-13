package io.github.metdaisy.amaazon.catalog.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryCreateRequest(
    @NotBlank @Size(max = 255) String name,
    UUID parentId) {

}
