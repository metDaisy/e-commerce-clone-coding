package io.github.metdaisy.amaazon.user.application.mapper;

import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface UserApiMapper {

  UserDto toDto(User user);

  default List<String> mapRoles(Set<UserRole> roles) {
    return roles == null ? List.of() : roles.stream().map(Enum::name).toList();
  }
}
