package io.github.metdaisy.amaazon.common.mapper;

import java.util.Collection;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.Named;
import org.mapstruct.SourceParameterCondition;

@Mapper(componentModel = ComponentModel.SPRING)
public interface UtilMapper {

  @SourceParameterCondition
  @Named("checkCollection")
  default <T extends Collection<?>> boolean checkCollection(T collection) {
    return collection != null && !collection.isEmpty();
  }
}
