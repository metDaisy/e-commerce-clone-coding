package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CatalogProductMapper {

  @Mapping(target = "categoryId", source = "category.id")
  CatalogProductResponse toDto(CatalogProduct catalogProduct);

  CatalogProduct toEntity(UUID managerId, Category category,
      CatalogProductCreateRequest request);
}
