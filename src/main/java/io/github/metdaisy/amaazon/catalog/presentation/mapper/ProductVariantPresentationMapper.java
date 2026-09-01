package io.github.metdaisy.amaazon.catalog.presentation.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantArchivedResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantQueryResponse;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductVariantPresentationMapper {

  ProductVariantAdminResponse toAdminResponse(ProductVariantDto source);

  ProductVariantQueryResponse toQueryResponse(ProductVariantDto source);

  ProductVariantArchivedResponse toArchivedResponse(ProductVariantDto source);
}
