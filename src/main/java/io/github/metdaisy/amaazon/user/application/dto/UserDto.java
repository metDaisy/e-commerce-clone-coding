package io.github.metdaisy.amaazon.user.application.dto;

import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("user-api")
public record UserDto(UUID id, List<UserRole> roles, boolean isEnabled) {

}
