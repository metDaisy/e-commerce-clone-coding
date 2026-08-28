package io.github.metdaisy.amaazon.catalog.presentation.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CategoryResponse;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CategoryPresentationMapper {

  CategoryResponse toResponse(CategoryDto source);

  List<CategoryResponse> toResponse(List<CategoryDto> source);
}
