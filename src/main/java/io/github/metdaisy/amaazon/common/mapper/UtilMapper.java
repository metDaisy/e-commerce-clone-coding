package io.github.metdaisy.amaazon.common.mapper;

import java.util.Collection;
import org.mapstruct.Named;
import org.mapstruct.SourceParameterCondition;

public interface UtilMapper {

  @SourceParameterCondition
  @Named("checkCollection")
  default <T extends Collection<?>> boolean checkCollection(T collection) {
    return collection != null && !collection.isEmpty();
  }
}
