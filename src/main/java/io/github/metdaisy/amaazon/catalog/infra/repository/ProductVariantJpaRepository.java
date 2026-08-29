package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID>,
    ProductVariantRepository {

  @Override
  Optional<ProductVariant> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = "catalogProduct")
  Optional<ProductVariant> findWithCatalogProductById(UUID id);

  @Override
  @Query("select variant from ProductVariant variant "
      + "where variant.catalogProduct.id in :catalogProductIds "
      + "and variant.publicationStatus = :publicationStatus "
      + "order by variant.id")
  List<ProductVariant> findByCatalogProductIdsAndPublicationStatus(
      @Param("catalogProductIds") Collection<UUID> catalogProductIds,
      @Param("publicationStatus") ArchiveStatus publicationStatus);
}
