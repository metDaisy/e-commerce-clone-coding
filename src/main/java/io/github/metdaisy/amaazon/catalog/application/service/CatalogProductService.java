package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductIdentifierUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogProductService {

  private final CatalogProductRepository repository;
  private final CatalogProductMapper mapper;
  private final TagService tagService;
  private final CategoryService categoryService;
  private final List<CatalogProductIdentifierVerifier> verifiers;

  @Transactional
  public CatalogProductResponse create(UUID managerId, CatalogProductCreateRequest request) {
    Category category = categoryService.getProxy(request.categoryId());
    CatalogProduct catalogProduct = mapper.toEntity(managerId, category, request);
    List<CatalogProductTag> tags = tagService.findAndCreate(request.tags())
        .stream()
        .map(tag -> CatalogProductTag.of(catalogProduct, tag))
        .toList();
    catalogProduct.setTags(tags);
    repository.save(catalogProduct);
    return mapper.toDto(catalogProduct);
  }

  @Transactional
  public CatalogProductResponse update(UUID id, CatalogProductUpdateRequest request) {
    CatalogProduct catalog = findById(id);
    List<CatalogProductTag> tags = request.tags() == null
        ? null
        : tagService.findAndCreate(request.tags())
            .stream()
            .map(tag -> CatalogProductTag.of(catalog, tag))
            .toList();
    mapper.update(catalog, tags, request);
    return mapper.toDto(catalog);
  }

  @Transactional
  public CatalogProductIdentifierUpdateResponse updateIdentifier(UUID id, CatalogProductIdentifierUpdateRequest request) {
    CatalogProduct catalog = findById(id);
    request.toMap().forEach((type, value) -> verifyIdentifier(id, type, value));
    mapper.update(catalog, request);
    return mapper.toIdentifierResponse(catalog);
  }

  private void verifyIdentifier(UUID id, CatalogProductIdentifierType type, String value) {
    for (CatalogProductIdentifierVerifier verifier: verifiers) {
      if (verifier.support(type)) {
        verifier.verify(id, value);
        return;
      }
    }
  }

  private CatalogProduct findById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            Map.of("catalogId", id)));
  }
}
