package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductVariantMapper {

  @Mapping(target = "catalogProductId", source = "catalogProduct.id")
  ProductVariantDto toDto(ProductVariant variant);

  void update(@MappingTarget ProductVariant variant, ProductVariantUpdateRequest request);
}
