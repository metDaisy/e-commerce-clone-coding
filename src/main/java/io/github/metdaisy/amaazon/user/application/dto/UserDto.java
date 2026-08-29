package io.github.metdaisy.amaazon.user.application.dto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("api")
public record UserDto(UUID id, List<String> roles, boolean isEnabled) {

  public UserDto {
    roles = roles == null ? Collections.emptyList() : roles;
  }

  public String rolesCsv() {
    return String.join(",", roles);
  }
}
