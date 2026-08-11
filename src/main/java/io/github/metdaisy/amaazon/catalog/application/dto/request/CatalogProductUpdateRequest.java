package io.github.metdaisy.amaazon.catalog.application.dto.request;

import java.util.List;
import java.util.Map;

public record CatalogProductUpdateRequest(String name, String description, String brand,
                                          List<String> tags, Map<String, Object> attributes) {

}
