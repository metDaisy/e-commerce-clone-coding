package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private final CategoryQueryService categoryQueryService;
  private final List<CatalogProductIdentifierVerifier> verifiers;

  @Transactional
  public CatalogProductResponse create(CatalogProductCreateRequest request) {
    validateIdentifiers(null, request.identifiers());
    Category category = categoryQueryService.getProxy(request.categoryId());
    CatalogProduct catalogProduct = mapper.toEntity(category, request);
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
    catalog.validateActive();
    List<CatalogProductTag> tags = request.tags() == null ? null
        : tagService.findAndCreate(request.tags())
            .stream()
            .map(tag -> CatalogProductTag.of(catalog, tag))
            .toList();
    mapper.update(catalog, tags, request);
    return mapper.toDto(catalog);
  }

  @Transactional
  public CatalogProductIdentifierUpdateResponse updateIdentifier(UUID id,
      Map<CatalogProductIdentifierType, String> identifiers) {
    CatalogProduct catalog = findById(id);
    catalog.validateActive();
    validateIdentifiers(id, identifiers);
    mapper.update(catalog, identifiers);
    return mapper.toIdentifierResponse(catalog);
  }

  @Transactional
  public CatalogProductArchivedResponse archive(UUID id) {
    CatalogProduct catalog = findById(id);
    catalog.archive();
    return new CatalogProductArchivedResponse(catalog.getId(), catalog.getPublicationStatus(),
        catalog.getArchivedAt(), catalog.getUpdatedAt());
  }

  private void verifyIdentifier(UUID id, CatalogProductIdentifierType type, String value) {
    for (CatalogProductIdentifierVerifier verifier : verifiers) {
      if (verifier.support(type)) {
        try {
          verifier.verify(id, value);
        } catch (CatalogProductException exception) {
          if (CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode().equals(exception.getCode())) {
            throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_DUPLICATE,
                AmaazonExceptionContext.logDetails(Map.of("identifierType", type.name())));
          }
          throw exception;
        }
        return;
      }
    }
  }

  private CatalogProduct findById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
  }

  private void validateIdentifiers(UUID id, Map<CatalogProductIdentifierType, String> identifiers) {
    if (identifiers == null || identifiers.isEmpty()) {
      return;
    }
    identifiers.entrySet()
        .stream()
        .filter(entry -> StringUtils.hasText(entry.getValue()))
        .forEach(entry -> verifyIdentifier(id, entry.getKey(), entry.getValue()));
  }

}
