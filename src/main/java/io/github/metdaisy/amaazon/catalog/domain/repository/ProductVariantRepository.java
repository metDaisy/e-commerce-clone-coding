package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends DomainRepository<ProductVariant> {

  Optional<ProductVariant> findWithCatalogProductById(UUID id);

  List<ProductVariant> findByCatalogProductIdsAndPublicationStatus(
      Collection<UUID> catalogProductIds, ArchiveStatus publicationStatus);
}
