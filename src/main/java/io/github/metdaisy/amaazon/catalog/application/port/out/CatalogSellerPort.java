package io.github.metdaisy.amaazon.catalog.application.port.out;

import java.util.UUID;

public interface CatalogSellerPort {

  boolean existsSeller(UUID id);
}
