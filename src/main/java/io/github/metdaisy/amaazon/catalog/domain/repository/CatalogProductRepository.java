package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductRepository
    extends DomainRepository<CatalogProduct>, CatalogProductQuery {

  Optional<CatalogProduct> findWithDetailsById(UUID id);
}
