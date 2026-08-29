package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogProductJpaRepository extends JpaRepository<CatalogProduct, UUID>,
    CatalogProductRepository, CatalogProductQueryRepository {

  @Override
  Optional<CatalogProduct> findById(UUID uuid);

  @Override
  @EntityGraph(attributePaths = {
      "category", "category.parent", "category.children"})
  Optional<CatalogProduct> findWithDetailsById(UUID uuid);

}
