package io.github.metdaisy.amaazon.product.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProductCreateRequest(@NotBlank String category,
                                   List<String> tags,
                                   @NotBlank String name,
                                   @NotBlank String description,
                                   @NotNull Integer price) {

}
