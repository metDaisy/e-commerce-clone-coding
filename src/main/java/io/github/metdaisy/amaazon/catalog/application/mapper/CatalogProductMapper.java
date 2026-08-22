package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = TagMapper.class)
public interface CatalogProductMapper {

  @Mapping(target = "categoryId", source = "category.id")
  @Mapping(target = "tags", source = "tags", qualifiedByName = "toTagName")
  CatalogProductResponse toDto(CatalogProduct catalogProduct);

  CatalogProductIdentifierUpdateResponse toIdentifierResponse(CatalogProduct catalog);

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
  void update(@MappingTarget CatalogProduct catalog, Map<String, String> identifiers);

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
}
