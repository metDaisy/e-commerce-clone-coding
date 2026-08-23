package io.github.metdaisy.amaazon.catalog.application.dto.response;

import java.util.Map;

public record ProductVariantResponse(String displayName, Map<String, Object> attributes) {

}
