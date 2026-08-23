package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductRepository extends DomainRepository<CatalogProduct> {

  Optional<CatalogProduct> findWithDetailsById(UUID id);

  boolean existsIdentifier(UUID id, CatalogIdentifierType type, String value);
}
