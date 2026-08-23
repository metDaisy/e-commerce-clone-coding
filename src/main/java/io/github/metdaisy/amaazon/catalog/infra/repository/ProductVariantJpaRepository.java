package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID>,
    ProductVariantRepository {

  @Override
  Optional<ProductVariant> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = "catalogProduct")
  Optional<ProductVariant> findWithCatalogProductById(UUID id);
}
