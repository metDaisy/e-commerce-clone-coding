package io.github.metdaisy.amaazon.catalog.presentation.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductDto;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductTagDto;
import io.github.metdaisy.amaazon.catalog.application.dto.response.TagDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogArchivedResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogProductQueryResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogProductResponse;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = ProductVariantPresentationMapper.class)
public interface CatalogProductPresentationMapper {

  @Mapping(target = "categoryId", source = "category.id")
  CatalogProductResponse toResponse(CatalogProductDto source);

  @Mapping(target = "categoryId", source = "category.id")
  CatalogProductQueryResponse toQueryResponse(CatalogProductDto source);

  CatalogIdentifierUpdateResponse toIdentifierResponse(CatalogProductDto source);

  CatalogArchivedResponse toArchivedResponse(CatalogProductDto source);

  PageResult<CatalogProductQueryResponse> toQueryResponse(PageResult<CatalogProductDto> source);

  default List<String> toTagNames(List<CatalogProductTagDto> tags) {
    if (tags == null || tags.isEmpty()) {
      return Collections.emptyList();
    }
    return tags.stream().map(CatalogProductTagDto::tag)
        .filter(Objects::nonNull)
        .map(TagDto::name)
        .toList();
  }
}
