package io.github.metdaisy.amaazon.catalog.presentation.dto;

import java.util.Map;

public record ProductVariantResponse(String displayName, Map<String, Object> attributes) {

}
