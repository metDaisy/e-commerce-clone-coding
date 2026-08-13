package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.UUID;

public interface CatalogProductRepository extends DomainRepository<CatalogProduct> {

  boolean existsIdentifier(UUID id, CatalogProductIdentifierType type, String value);
}
