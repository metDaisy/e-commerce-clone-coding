package io.github.metdaisy.amaazon.catalog.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ProductVariantUpdateRequest(
    @Pattern(regexp = ".*\\S.*", message = "상품 옵션 표시명은 비어 있을 수 없습니다.")
    @Size(max = 255) String displayName,
    Map<String, Object> attributes) {

}
