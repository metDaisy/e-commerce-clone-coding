package io.github.metdaisy.amaazon.catalog.domain.port.out;

import java.util.UUID;

public interface CatalogSellerPort {

  boolean existsActiveSellerByUserId(UUID userId);
}
