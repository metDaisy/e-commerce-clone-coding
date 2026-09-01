package io.github.metdaisy.amaazon.catalog.application.service.catalog;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductCommandDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.application.service.tag.TagService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.LinkedHashMap;
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
  private final CategoryQueryService categoryQueryService;
  private final List<CatalogProductIdentifierVerifier> verifiers;

  @Transactional
  public CatalogProductCommandDto create(CatalogProductCreateRequest request) {
    validateIdentifiers(null, request.identifiers(), true);
    Category category = categoryQueryService.getProxy(request.categoryId());
    CatalogProduct catalogProduct = mapper.toEntity(category, request);
    mapper.updateIdentifierFields(catalogProduct, request.identifiers());
    List<CatalogProductTag> tags = tagService.findAndCreate(request.tags())
        .stream()
        .map(tag -> CatalogProductTag.of(catalogProduct, tag))
        .toList();
    catalogProduct.setTags(tags);
    repository.save(catalogProduct);
    return mapper.toCommandDto(catalogProduct);
  }

  @Transactional
  public CatalogProductCommandDto update(UUID id, CatalogProductUpdateRequest request) {
    CatalogProduct catalog = findById(id);
    catalog.validateActive();
    List<CatalogProductTag> tags = tagService.findAndCreate(request.tags())
        .stream()
        .map(tag -> CatalogProductTag.of(catalog, tag))
        .toList();
    mapper.update(catalog, tags, request);
    return mapper.toCommandDto(catalog);
  }

  @Transactional
  public CatalogProductCommandDto updateIdentifier(UUID id,
      Map<String, String> identifiers) {
    CatalogProduct catalog = findById(id);
    catalog.validateActive();
    validateIdentifiers(id, identifiers, false);
    mapper.updateIdentifierFields(catalog, identifiers);
    return mapper.toCommandDto(catalog);
  }

  @Transactional
  public CatalogProductCommandDto archive(UUID id) {
    CatalogProduct catalog = findById(id);
    catalog.archive();
    return mapper.toCommandDto(catalog);
  }

  private void verifyIdentifier(UUID id, String type, String value) {
    for (CatalogProductIdentifierVerifier verifier : verifiers) {
      if (verifier.support(type)) {
        verifier.verify(id, value);
        return;
      }
    }
  }

  private CatalogProduct findById(UUID id) {
    return repository.findWithDetailsById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
  }

  private void validateIdentifiers(UUID id,
      Map<String, String> identifiers, boolean required) {
    if (identifiers == null || identifiers.isEmpty()) {
      if (required) {
        throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID);
      }
      return;
    }
    boolean hasText = identifiers.values().stream().allMatch(StringUtils::hasText);
    if (required && !hasText) {
      throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID);
    }
    Map<String, String> failures = new LinkedHashMap<>();
    Map<String, Object> logDetails = new LinkedHashMap<>();
    identifiers.forEach((key, value) -> {
      try {
        verifyIdentifier(id, key, value);
      } catch (CatalogProductException exception) {
        failures.put(key, exception.getCode());
        logDetails.putAll(exception.getLogDetails());
      }
    });
    if (!failures.isEmpty()) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          new AmaazonExceptionContext(Map.of("fields", failures), logDetails, null));
    }
  }

}
