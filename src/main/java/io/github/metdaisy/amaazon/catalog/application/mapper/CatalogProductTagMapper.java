package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductTagDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class, uses = TagMapper.class)
public interface CatalogProductTagMapper {

  CatalogProductTagDto toDto(CatalogProductTag entity);

  List<CatalogProductTagDto> toDto(List<CatalogProductTag> entities);
}
