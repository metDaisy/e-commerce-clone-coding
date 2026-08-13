package io.github.metdaisy.amaazon.catalog.application.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, UUID parentId, Integer depth,
                               List<CategoryResponse> children) {

}
