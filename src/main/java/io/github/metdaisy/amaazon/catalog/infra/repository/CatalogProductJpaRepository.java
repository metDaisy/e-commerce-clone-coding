package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CatalogProductJpaRepository extends JpaRepository<CatalogProduct, UUID>,
    JpaSpecificationExecutor<CatalogProduct>,
    CatalogProductRepository {

  @Override
  default boolean existsIdentifier(UUID id, CatalogProductIdentifierType type, String value) {
    Specification<CatalogProduct> spec = CatalogProductSpecification.hasId(id)
        .and(CatalogProductSpecification.hasIdentifier(type, value));
    return this.exists(spec);
  }
}
