package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor
public class FormUserDetails implements UserDetails, CredentialsContainer, AmaazonPrincipal {

  private final UUID id;
  private final String role;
  private String password;
  private final boolean isEnabled;
  private final boolean isLocked;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AmaazonPrincipal.super.getAuthorities();
  }

  @Override
  public String getUsername() {
    return id.toString();
  }

  @Override
  public void eraseCredentials() {
    this.password = null;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !isLocked;
  }
}
