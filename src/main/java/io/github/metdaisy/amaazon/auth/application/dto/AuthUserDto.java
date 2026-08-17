package io.github.metdaisy.amaazon.auth.application.dto;

import java.util.List;
import java.util.UUID;

public record AuthUserDto(UUID id, List<String> roles, boolean isEnabled) {

  public String rolesCsv() {
    return String.join(",", roles);
  }

}
