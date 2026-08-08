package io.github.metdaisy.amaazon.product.application.mapper;

import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.product.application.dto.ProductResponse;
import io.github.metdaisy.amaazon.product.domain.entity.Product;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductMapper {

  ProductResponse toDto(Product product);
}
