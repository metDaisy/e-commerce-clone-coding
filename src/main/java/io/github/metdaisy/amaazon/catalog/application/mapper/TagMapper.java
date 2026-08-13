package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {

  @Named("toTagName")
  default List<String> toTagName(List<CatalogProductTag> tags) {
    if (tags.isEmpty()) {
      return Collections.emptyList();
    }
    return tags.stream()
        .map(CatalogProductTag::getTag)
        .map(Tag::getName)
        .toList();
  }
}
