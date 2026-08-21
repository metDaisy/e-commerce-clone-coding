package io.github.metdaisy.amaazon.global.security.jwt.model;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.security.Principal;
import java.util.UUID;

public record JwtPrincipal(UUID id, String role) implements AmaazonPrincipal, Principal {

  public JwtPrincipal(String subject, String role) {
    this(UUID.fromString(subject), role);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getRole() {
    return role;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return id.toString();
  }
}
