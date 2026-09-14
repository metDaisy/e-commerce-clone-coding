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
import io.github.metdaisy.amaazon.catalog.domain.verifier.IdentifierVerificationResult;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

  private CatalogProduct findById(UUID id) {
    return repository.findWithDetailsById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
  }

  private IdentifierVerificationResult verifyIdentifier(UUID id, String type, String value) {
    for (CatalogProductIdentifierVerifier verifier : verifiers) {
      if (verifier.support(type)) {
        return verifier.verify(id, value);
      }
    }
    return IdentifierVerificationResult.success();
  }

  private void validateIdentifiers(UUID id,
      Map<String, String> identifiers, boolean required) {
    if (identifiers == null || identifiers.isEmpty()) {
      if (required) {
        throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
            new AmaazonExceptionContext(
                Map.of("fields", Map.of(
                    "identifiers", Map.of(
                        "code", CatalogProductErrorCode.IDENTIFIER_INVALID.getCode(),
                        "message", CatalogProductErrorCode.IDENTIFIER_INVALID.getMessage()))),
                Collections.emptyMap(), null));
      }
      return;
    }
    IdentifierValidationFailures validation = collectIdentifierFailures(id, identifiers);
    if (!validation.failures().isEmpty()) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          new AmaazonExceptionContext(
              Map.of("fields", toClientFields(validation.failures())),
              validation.logDetails(), null));
    }
  }

  private IdentifierValidationFailures collectIdentifierFailures(UUID id,
      Map<String, String> identifiers) {
    Map<String, IdentifierVerificationResult> failures = new LinkedHashMap<>();
    Map<String, Object> logDetails = new LinkedHashMap<>();
    identifiers.entrySet().forEach(entry ->
        collectIdentifierFailure(id, entry, failures, logDetails));
    return new IdentifierValidationFailures(failures, logDetails);
  }

  private Map<String, Map<String, String>> toClientFields(
      Map<String, IdentifierVerificationResult> failures) {
    return failures.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> Map.of(
                "code", entry.getValue().code(),
                "message", entry.getValue().message()),
            (first, second) -> first,
            LinkedHashMap::new));
  }

  private void collectIdentifierFailure(UUID id,
      Map.Entry<String, String> entry,
      Map<String, IdentifierVerificationResult> failures,
      Map<String, Object> logDetails) {
    String key = entry.getKey();
    String value = entry.getValue();
    if (!StringUtils.hasText(value)) {
      failures.put(key, IdentifierVerificationResult.failure(
          CatalogProductErrorCode.IDENTIFIER_INVALID));
      return;
    }
    IdentifierVerificationResult result = verifyIdentifier(id, key, value);
    if (!result.valid()) {
      failures.put(key, result);
      logDetails.putAll(result.logDetails());
    }
  }

  private record IdentifierValidationFailures(
      Map<String, IdentifierVerificationResult> failures,
      Map<String, Object> logDetails) {
  }

}
