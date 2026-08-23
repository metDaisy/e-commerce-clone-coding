package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.StringUtils;

@Mapper(config = GlobalMapperConfig.class, uses = TagMapper.class)
public interface CatalogProductMapper {

  @Mapping(target = "categoryId", source = "category.id")
  @Mapping(target = "tags", source = "tags", qualifiedByName = "toTagName")
  CatalogProductResponse toDto(CatalogProduct catalogProduct);

  CatalogIdentifierUpdateResponse toIdentifierResponse(CatalogProduct catalog);

  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = ".", source = "request")
  CatalogProduct toEntity(Category category, CatalogProductCreateRequest request);

  @Mapping(target = "tags", source = "tags", conditionQualifiedByName = "checkCollection")
  void update(@MappingTarget CatalogProduct catalog, List<CatalogProductTag> tags,
      CatalogProductUpdateRequest request);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "asin")
  @Mapping(target = "gtin")
  @Mapping(target = "upc")
  @Mapping(target = "ean")
  @Mapping(target = "isbn")
  void updateIdentifierFields(@MappingTarget CatalogProduct catalog,
      Map<String, String> identifiers);

  default void update(@MappingTarget CatalogProduct catalog,
      Map<CatalogIdentifierType, String> identifiers) {
    updateIdentifierFields(catalog, toIdentifierMap(identifiers));
  }

  @AfterMapping
  default void updateUpdatedAt(@MappingTarget CatalogProduct catalog,
      List<CatalogProductTag> tags, CatalogProductUpdateRequest request) {
    catalog.setUpdatedAt(Instant.now());
  }

  @AfterMapping
  default void updateUpdatedAt(@MappingTarget CatalogProduct catalog,
      Map<String, String> identifiers) {
    catalog.setUpdatedAt(Instant.now());
  }

  @AfterMapping
  default void mapIdentifiers(CatalogProductCreateRequest request,
      @MappingTarget CatalogProduct catalog) {
    update(catalog, request.identifiers());
  }

  private Map<String, String> toIdentifierMap(
      Map<CatalogIdentifierType, String> identifiers) {
    if (identifiers == null || identifiers.isEmpty()) {
      return Collections.emptyMap();
    }
    return identifiers.entrySet().stream()
        .filter(entry -> StringUtils.hasText(entry.getValue()))
        .collect(Collectors.toMap(
            entry -> entry.getKey().name().toLowerCase(Locale.ROOT),
            Entry::getValue));
  }
}
