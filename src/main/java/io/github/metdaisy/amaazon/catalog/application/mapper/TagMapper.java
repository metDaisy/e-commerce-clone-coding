package io.github.metdaisy.amaazon.catalog.application.mapper;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Named;

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
