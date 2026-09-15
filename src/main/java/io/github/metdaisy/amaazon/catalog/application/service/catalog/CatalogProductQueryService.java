package io.github.metdaisy.amaazon.catalog.application.service.catalog;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductQueryDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogProductQueryService {

  private final CatalogProductRepository repository;
  private final CatalogProductMapper mapper;

  public PageResult<CatalogProductQueryDto> findPage(
      CatalogProductPageRequest request, Set<UUID> categoryIds,
      ArchiveStatus archiveStatus, ArchiveStatus variantStatus) {
    PageResult<CatalogProduct> page = repository.findPage(categoryIds,
        request.keyword(), request.tag(), request.toSort(), archiveStatus, variantStatus,
        request.toPageQuery());
    return new PageResult<>(page.content().stream()
        .map(mapper::toQueryDto)
        .toList(),
        page.page(), page.size(), page.totalElements(), page.totalPages());
  }

  public CatalogProductQueryDto find(UUID id, ArchiveStatus publicationStatus) {
    CatalogProduct product = repository.findWithDetailsById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
    if (publicationStatus != null && product.getPublicationStatus() != publicationStatus) {
      throw new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of("catalogId", id)));
    }
    return mapper.toQueryDto(product);
  }

}
