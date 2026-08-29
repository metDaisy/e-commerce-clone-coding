package io.github.metdaisy.amaazon.catalog.presentation.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantArchivedResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantQueryResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantResponse;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductVariantPresentationMapper {

  @Mapping(target = "catalogProductId", source = "catalogProduct.id")
  ProductVariantAdminResponse toAdminResponse(ProductVariantDto source);

  ProductVariantResponse toResponse(ProductVariantDto source);

  ProductVariantQueryResponse toQueryResponse(ProductVariantDto source);

  ProductVariantArchivedResponse toArchivedResponse(ProductVariantDto source);
}
