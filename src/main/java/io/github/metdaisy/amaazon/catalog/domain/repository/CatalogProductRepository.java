package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.UUID;

public interface CatalogProductRepository extends DomainRepository<CatalogProduct> {

  boolean existsByIdAndManagerId(UUID id, UUID managerId);

  boolean existsByAsin(String asin);

  boolean existsByGtin(String gtin);

  boolean existsByUpc(String upc);

  boolean existsByEan(String ean);

  boolean existsByIsbn(String isbn);
}
