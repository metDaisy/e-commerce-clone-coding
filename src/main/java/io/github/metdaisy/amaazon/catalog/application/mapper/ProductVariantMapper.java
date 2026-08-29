package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = CatalogProductMapper.class)
public interface ProductVariantMapper {

  ProductVariantDto toDto(ProductVariant variant);

  void update(@MappingTarget ProductVariant variant, ProductVariantUpdateRequest request);
}
