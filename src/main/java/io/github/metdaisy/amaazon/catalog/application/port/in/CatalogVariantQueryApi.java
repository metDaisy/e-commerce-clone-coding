package io.github.metdaisy.amaazon.catalog.application.port.in;

import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogVariantQueryApi {

  private final ProductVariantRepository repository;

  public Optional<CatalogVariantReference> findActiveByVariantId(UUID variantId) {
    return repository.findWithCatalogProductById(variantId)
        .filter(ProductVariant::isActive)
        .filter(variant -> variant.getCatalogProduct().getPublicationStatus() == CatalogStatus.ACTIVE)
        .map(variant -> new CatalogVariantReference(variant.getId(),
            variant.getCatalogProduct().getId()));
  }

  public record CatalogVariantReference(UUID variantId, UUID catalogProductId) {
  }
}
