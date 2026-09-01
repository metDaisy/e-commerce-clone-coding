package io.github.metdaisy.amaazon.catalog.application.service.catalog;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductQueryDto;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.application.service.variant.ProductVariantQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogQueryService {

  private final CatalogProductQueryService catalogProductQueryService;
  private final ProductVariantQueryService productVariantQueryService;
  private final CategoryQueryService categoryQueryService;

  public PageResult<CatalogProductQueryDto> findPageForProductManager(
      CatalogProductPageRequest request) {
    return findPage(request, ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE);
  }

  public PageResult<CatalogProductQueryDto> findPageForAdmin(
      CatalogProductPageRequest request) {
    return findPage(request, request.catalogPublicationStatus(),
        request.variantPublicationStatus());
  }

  public CatalogProductQueryDto findForProductManager(UUID id) {
    return find(id, ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE);
  }

  public CatalogProductQueryDto findForAdmin(UUID id) {
    return find(id, null, null);
  }

  private PageResult<CatalogProductQueryDto> findPage(CatalogProductPageRequest request,
      ArchiveStatus catalogStatus, ArchiveStatus variantStatus) {
    Set<UUID> categoryIds = findCategoryIds(request.categoryId());
    PageResult<CatalogProductQueryDto> page = catalogProductQueryService.findPage(request,
        categoryIds,
        catalogStatus,
        variantStatus);
    Map<UUID, List<ProductVariantDto>> variantsByProductId =
        productVariantQueryService.findByCatalogProductIds(
            page.content().stream().map(CatalogProductQueryDto::getId).toList(),
            variantStatus);
    withVariants(page, variantsByProductId);
    return page;
  }

  private CatalogProductQueryDto find(UUID id, ArchiveStatus catalogStatus,
      ArchiveStatus variantStatus) {
    CatalogProductQueryDto product = catalogProductQueryService.find(id, catalogStatus);
    List<ProductVariantDto> byCatalogProductId = productVariantQueryService.findByCatalogProductId(
        id, variantStatus);
    product.setVariants(byCatalogProductId);
    return product;
  }

  private Set<UUID> findCategoryIds(UUID categoryId) {
    return categoryId == null
        ? Collections.emptySet()
        : categoryQueryService.findSelfAndDescendantIds(categoryId);
  }

  private void withVariants(PageResult<CatalogProductQueryDto> page,
      Map<UUID, List<ProductVariantDto>> variantsByProductId) {
    page.content().forEach(product -> product.setVariants(
        variantsByProductId.getOrDefault(product.getId(), Collections.emptyList())));
  }
}
