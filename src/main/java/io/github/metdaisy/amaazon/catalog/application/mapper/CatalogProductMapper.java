package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface CatalogProductMapper {

  @Mapping(target = "categoryId", source = "category.id")
  CatalogProductResponse toDto(CatalogProduct catalogProduct);

  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = ".", source = "request")
  CatalogProduct toEntity(UUID managerId, Category category,
      CatalogProductCreateRequest request);

  @Mapping(target = "tags", ignore = true)
  void update(@MappingTarget CatalogProduct catalog, List<CatalogProductTag> tags,
      CatalogProductUpdateRequest request);
}
