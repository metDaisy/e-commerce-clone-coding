package io.github.metdaisy.amaazon.user.application.mapper;

import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

  @Mapping(target = "isEnabled", source = "user.enabled")
  UserResponse toDto(User user, String loginEmail);
}
