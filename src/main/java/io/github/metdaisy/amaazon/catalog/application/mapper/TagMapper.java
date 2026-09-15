package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.application.dto.response.TagDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

@Mapper(componentModel = ComponentModel.SPRING)
public interface TagMapper {

  TagDto toDto(Tag tag);

}
