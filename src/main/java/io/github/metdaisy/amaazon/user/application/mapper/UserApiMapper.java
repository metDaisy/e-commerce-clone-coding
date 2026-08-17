package io.github.metdaisy.amaazon.user.application.mapper;

import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface UserApiMapper {

  UserDto toDto(User user);
}
