package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogProductService {

  private final CatalogProductRepository repository;
  private final CatalogProductMapper mapper;
  private final TagService tagService;
  private final CategoryService categoryService;

  @Transactional
  public CatalogProductResponse create(UUID managerId, CatalogProductCreateRequest request) {
    Category category = categoryService.getProxy(request.categoryId());
    validateProductCodes(request);
    CatalogProduct catalogProduct = mapper.toEntity(managerId, category, request);
    List<CatalogProductTag> tags = tagService.findAndCreate(request.tags())
        .stream()
        .map(tag -> CatalogProductTag.of(catalogProduct, tag))
        .toList();
    catalogProduct.setTags(tags);
    repository.save(catalogProduct);
    return mapper.toDto(catalogProduct);
  }

  private void validateProductCodes(CatalogProductCreateRequest request) {
    if (StringUtils.hasText(request.asin()) && repository.existsByAsin(request.asin())) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          Map.of("asin", request.asin()));
    }
    if (StringUtils.hasText(request.gtin()) && repository.existsByGtin(request.gtin())) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          Map.of("gtin", request.gtin()));
    }
    if (StringUtils.hasText(request.upc()) && repository.existsByUpc(request.upc())) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          Map.of("upc", request.upc()));
    }
    if (StringUtils.hasText(request.ean()) && repository.existsByEan(request.ean())) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          Map.of("ean", request.ean()));
    }
    if (StringUtils.hasText(request.isbn()) && repository.existsByIsbn(request.isbn())) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          Map.of("isbn", request.isbn()));
    }
  }
}
