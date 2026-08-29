package io.github.metdaisy.amaazon.common.mapper;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Condition;
import org.mapstruct.ConditionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;
import org.mapstruct.SourceParameterCondition;
import org.springframework.util.StringUtils;

@Mapper(componentModel = ComponentModel.SPRING)
public interface UtilMapper {

  @SourceParameterCondition
  default boolean hasElement(Collection<?> collection) {
    return collection != null && !collection.isEmpty();
  }

  @SourceParameterCondition
  default boolean hasElement(Map<?, ?> map) {
    return map != null && !map.isEmpty();
  }

  @Condition
  default boolean hasText(String value) {
    return StringUtils.hasText(value);
  }

  @AfterMapping
  default <T extends MutableEntity> void update(@MappingTarget T entity) {
    if (entity.isNew()) {
      return;
    }
    entity.setUpdatedAt(Instant.now());
  }
}
