package io.github.metdaisy.amaazon.catalog.presentation.dto;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, UUID parentId, Integer depth,
                               List<CategoryResponse> children) {

}
