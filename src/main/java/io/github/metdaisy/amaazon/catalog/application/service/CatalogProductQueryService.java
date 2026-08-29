package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapper;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogProductQueryService {

  private final CatalogProductRepository repository;
  private final ProductVariantRepository variantRepository;
  private final CatalogProductMapper mapper;
  private final ProductVariantMapper variantMapper;
  private final CategoryQueryService categoryQueryService;

  public PageResult<CatalogProductDto> findPage(
      CatalogProductPageRequest request, boolean admin) {
    ArchiveStatus archiveStatus = resolveStatus(request.catalogPublicationStatus(), admin);
    ArchiveStatus variantStatus = resolveStatus(request.variantPublicationStatus(), admin);
    Set<UUID> categoryIds = request.categoryId() == null
        ? Set.of()
        : categoryQueryService.findSelfAndDescendantIds(request.categoryId());
    PageResult<CatalogProduct> page = repository.findPage(categoryIds,
        request.keyword(), request.tag(), request.toSort(), archiveStatus, variantStatus,
        request.toPageQuery());
    Map<UUID, List<ProductVariant>> variantsByProductId =
        findVariants(page.content(), variantStatus);
    return new PageResult<>(page.content().stream()
        .map(product -> toDto(product, variantsByProductId))
        .toList(),
        page.page(), page.size(), page.totalElements(), page.totalPages());
  }

  public CatalogProductDto find(UUID id, boolean admin) {
    CatalogProduct product = repository.findWithDetailsById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
    ArchiveStatus variantStatus = resolveStatus(null, admin);
    List<ProductVariant> variants = variantRepository
        .findByCatalogProductIdsAndPublicationStatus(List.of(id), variantStatus);
    if (!admin && product.getPublicationStatus() != ArchiveStatus.ACTIVE) {
      throw new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of("catalogId", id)));
    }
    return mapper.toDto(product).withVariants(variants.stream()
        .map(variantMapper::toDto)
        .toList());
  }

  private CatalogProductDto toDto(CatalogProduct product,
      Map<UUID, List<ProductVariant>> variantsByProductId) {
    return mapper.toDto(product).withVariants(variantsByProductId
        .getOrDefault(product.getId(), List.of()).stream()
        .map(variantMapper::toDto)
        .toList());
  }

  private ArchiveStatus resolveStatus(ArchiveStatus requestedStatus, boolean admin) {
    return admin && requestedStatus != null ? requestedStatus : ArchiveStatus.ACTIVE;
  }

  private Map<UUID, List<ProductVariant>> findVariants(List<CatalogProduct> products,
      ArchiveStatus publicationStatus) {
    if (products.isEmpty()) {
      return Map.of();
    }
    List<UUID> productIds = products.stream().map(CatalogProduct::getId).toList();
    return variantRepository.findByCatalogProductIdsAndPublicationStatus(productIds,
            publicationStatus)
        .stream()
        .collect(Collectors.groupingBy(
            variant -> variant.getCatalogProduct().getId(), HashMap::new, Collectors.toList()));
  }
}
