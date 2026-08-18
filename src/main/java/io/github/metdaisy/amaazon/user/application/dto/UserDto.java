package io.github.metdaisy.amaazon.user.application.dto;

import java.util.List;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("user-api")
public record UserDto(UUID id, List<String> roles, boolean isEnabled) {

  public String rolesCsv() {
    return String.join(",", roles);
  }
}
