package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CategoryMapper {

  @Mapping(target = "parentId", source = "parent.id")
  CategoryDto toDto(Category entity);

  List<CategoryDto> toDto(List<Category> entities);

}
