package io.github.metdaisy.amaazon.catalog.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CatalogIdentifierUpdateResponse(@JsonProperty("catalogProductId") UUID id,
                                              String asin, String gtin,
                                              String upc, String ean, String isbn
) {

}
