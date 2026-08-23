package io.github.metdaisy.amaazon.catalog.infra.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;

public interface CatalogProductJpaRepository extends JpaRepository<CatalogProduct, UUID>,
    JpaSpecificationExecutor<CatalogProduct>,
    CatalogProductRepository {

  @Override
  Optional<CatalogProduct> findById(UUID uuid);

  @Override
  @EntityGraph(attributePaths = {
      "category", "category.parent", "category.children"})
  Optional<CatalogProduct> findWithDetailsById(UUID uuid);

  @Override
  default boolean existsIdentifier(UUID id, CatalogIdentifierType type, String value) {
    Specification<CatalogProduct> spec = CatalogProductSpecification.hasIdentifier(type, value);
    if (id != null) {
      spec = spec.and(CatalogProductSpecification.hasIdNot(id));
    }
    return this.exists(spec);
  }
}
