package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class FormUserDetails implements UserDetails, CredentialsContainer, AmaazonPrincipal {

  private final UUID id;
  private final String role;
  private final boolean enabled;
  private final boolean isLocked;
  private String password;

  public FormUserDetails(UUID id, String role, String password, boolean isEnabled,
      boolean isLocked) {
    this.id = id;
    this.role = role;
    this.password = password;
    this.enabled = isEnabled;
    this.isLocked = isLocked;
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
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AmaazonPrincipal.super.getAuthorities();
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return id.toString();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !isLocked;
  }

  @Override
  public void eraseCredentials() {
    password = null;
  }
}
