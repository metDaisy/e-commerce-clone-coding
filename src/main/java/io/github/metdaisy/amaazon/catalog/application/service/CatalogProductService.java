package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    Map<CatalogIdentifierType, String> normalizedIdentifiers =
        validateIdentifiers(null, request.identifiers(), true);
    Category category = categoryQueryService.getProxy(request.categoryId());
    CatalogProduct catalogProduct = mapper.toEntity(category, request);
    mapper.update(catalogProduct, normalizedIdentifiers);
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
  public CatalogIdentifierUpdateResponse updateIdentifier(UUID id,
      Map<CatalogIdentifierType, String> identifiers) {
    CatalogProduct catalog = findById(id);
    catalog.validateActive();
    Map<CatalogIdentifierType, String> normalizedIdentifiers =
        validateIdentifiers(id, identifiers, false);
    mapper.update(catalog, normalizedIdentifiers);
    return mapper.toIdentifierResponse(catalog);
  }

  @Transactional
  public CatalogArchivedResponse archive(UUID id) {
    CatalogProduct catalog = findById(id);
    catalog.archive();
    return new CatalogArchivedResponse(catalog.getId(), catalog.getPublicationStatus(),
        catalog.getArchivedAt(), catalog.getUpdatedAt());
  }

  private String verifyIdentifier(UUID id, CatalogIdentifierType type, String value) {
    for (CatalogProductIdentifierVerifier verifier : verifiers) {
      if (verifier.support(type)) {
        try {
          return verifier.verify(id, value);
        } catch (CatalogProductException exception) {
          if (CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode().equals(exception.getCode())) {
            throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_DUPLICATE,
                AmaazonExceptionContext.logDetails(Map.of("identifierType", type.name())));
          }
          throw exception;
        }
      }
    }
    return value;
  }

  private CatalogProduct findById(UUID id) {
    return repository.findWithDetailsById(id)
        .orElseThrow(() -> new CatalogProductException(CatalogProductErrorCode.CATALOG_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("catalogId", id))));
  }

  private Map<CatalogIdentifierType, String> validateIdentifiers(UUID id,
      Map<CatalogIdentifierType, String> identifiers, boolean required) {
    if (identifiers == null || identifiers.isEmpty()) {
      if (required) {
        throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID);
      }
      return Map.of();
    }
    List<Entry<CatalogIdentifierType, String>> validIdentifiers = identifiers.entrySet()
        .stream()
        .filter(entry -> StringUtils.hasText(entry.getValue()))
        .toList();
    if (required && validIdentifiers.isEmpty()) {
      throw new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID);
    }
    List<CatalogProductException> failures = new ArrayList<>();
    List<Map<String, Object>> fields = new ArrayList<>();
    Map<String, Object> logDetails = new LinkedHashMap<>();
    Map<CatalogIdentifierType, String> normalizedIdentifiers = new LinkedHashMap<>();
    validIdentifiers.forEach(entry -> {
      try {
        normalizedIdentifiers.put(entry.getKey(),
            verifyIdentifier(id, entry.getKey(), entry.getValue()));
      } catch (CatalogProductException exception) {
        failures.add(exception);
        fields.add(identifierField(entry.getKey(), exception));
        logDetails.putAll(exception.getLogDetails());
      }
    });
    if (!failures.isEmpty()) {
      throw new CatalogProductException(resolveIdentifierErrorCode(failures),
          new AmaazonExceptionContext(Map.of("fields", List.copyOf(fields)), logDetails, null));
    }
    return normalizedIdentifiers;
  }

  private Map<String, Object> identifierField(CatalogIdentifierType type,
      CatalogProductException exception) {
    String reason = switch (exception.getCode()) {
      case "CATALOG-017" -> "duplicate";
      case "CATALOG-015" -> "external_verification_failed";
      default -> "invalid_format";
    };
    return Map.of("field", type.name().toLowerCase(Locale.ROOT), "reason", reason);
  }

  private CatalogProductErrorCode resolveIdentifierErrorCode(
      List<CatalogProductException> failures) {
    if (failures.stream().anyMatch(exception ->
        CatalogProductErrorCode.IDENTIFIER_INVALID.getCode().equals(exception.getCode()))) {
      return CatalogProductErrorCode.IDENTIFIER_INVALID;
    }
    if (failures.stream().anyMatch(exception ->
        CatalogProductErrorCode.ISBN_EXTERNAL_VERIFICATION_FAILED.getCode()
            .equals(exception.getCode()))) {
      return CatalogProductErrorCode.ISBN_EXTERNAL_VERIFICATION_FAILED;
    }
    return CatalogProductErrorCode.IDENTIFIER_DUPLICATE;
  }

}
