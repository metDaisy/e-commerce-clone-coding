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
    Specification<CatalogProduct> spec = CatalogProductSpecification.hasIdentifier(type, value);
    if (id != null) {
      spec = spec.and(CatalogProductSpecification.hasIdNot(id));
    }
    return this.exists(spec);
  }
}
