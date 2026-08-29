package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductVariantService {

  private final ProductVariantRepository repository;
  private final CatalogProductRepository catalogProductRepository;
  private final ProductVariantMapper mapper;

  @Transactional
  public ProductVariantDto create(UUID catalogProductId,
      ProductVariantCreateRequest request) {
    CatalogProduct catalogProduct = findActiveCatalogProduct(catalogProductId);
    ProductVariant variant = ProductVariant.of(catalogProduct, request.displayName(),
        request.attributes());
    return mapper.toDto(repository.save(variant));
  }

  public ProductVariantDto findPublic(UUID id) {
    ProductVariant variant = findById(id);
    if (!variant.isActive() || variant.getCatalogProduct().getPublicationStatus()
        != ArchiveStatus.ACTIVE) {
      throw variantNotFound(id);
    }
    return mapper.toDto(variant);
  }

  public ProductVariantDto findAdmin(UUID id) {
    return mapper.toDto(findById(id));
  }

  public ProductVariantDto findForCatalogManager(UUID id) {
    ProductVariant variant = findById(id);
    if (!variant.isActive() || variant.getCatalogProduct().getPublicationStatus()
        != ArchiveStatus.ACTIVE) {
      throw variantNotFound(id);
    }
    return mapper.toDto(variant);
  }

  @Transactional
  public ProductVariantDto update(UUID id, ProductVariantUpdateRequest request) {
    ProductVariant variant = findById(id);
    if (variant.getCatalogProduct().getPublicationStatus() != ArchiveStatus.ACTIVE) {
      throw new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
          AmaazonExceptionContext.logDetails(
              Map.of("catalogId", variant.getCatalogProduct().getId())));
    }
    variant.validateActive();
    mapper.update(variant, request);
    return mapper.toDto(variant);
  }

  @Transactional
  public ProductVariantDto archive(UUID id) {
    ProductVariant variant = findById(id);
    variant.archive();
    return mapper.toDto(variant);
  }

  private ProductVariant findById(UUID id) {
    return repository.findWithCatalogProductById(id)
        .orElseThrow(() -> variantNotFound(id));
  }

  private CatalogProduct findActiveCatalogProduct(UUID id) {
    CatalogProduct catalogProduct = catalogProductRepository.findById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
    if (catalogProduct.getPublicationStatus() != ArchiveStatus.ACTIVE) {
      throw new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of("catalogId", id)));
    }
    return catalogProduct;
  }

  private ProductVariantException variantNotFound(UUID id) {
    return new ProductVariantException(ProductVariantErrorCode.VARIANT_NOT_FOUND,
        AmaazonExceptionContext.logDetails(Map.of("variantId", id)));
  }
}
