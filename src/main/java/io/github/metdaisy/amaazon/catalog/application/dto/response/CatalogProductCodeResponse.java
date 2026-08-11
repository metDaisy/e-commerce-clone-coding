package io.github.metdaisy.amaazon.catalog.application.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CatalogProductCodeResponse(UUID id, String asin, String gtin, String upc, String ean,
                                         String isbn, Instant updatedAt) {

}
