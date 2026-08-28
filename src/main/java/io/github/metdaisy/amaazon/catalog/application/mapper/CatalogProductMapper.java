package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.List;
import java.util.Map;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = {CategoryMapper.class,
    CatalogProductTagMapper.class})
public interface CatalogProductMapper {

  CatalogProductDto toDto(CatalogProduct catalogProduct);

  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = ".", source = "request")
  CatalogProduct toEntity(Category category, CatalogProductCreateRequest request);

  @Mapping(target = "tags", source = "tags")
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
}
