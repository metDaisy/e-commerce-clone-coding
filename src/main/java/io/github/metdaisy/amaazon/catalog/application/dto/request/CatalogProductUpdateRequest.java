package io.github.metdaisy.amaazon.catalog.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record CatalogProductUpdateRequest(
    @Pattern(regexp = ".*\\S.*", message = "상품명은 비어 있을 수 없습니다.")
    @Size(max = 255) String name,
    String description, @Size(max = 255) String brand, List<String> tags,
    Map<String, Object> attributes) {

}
