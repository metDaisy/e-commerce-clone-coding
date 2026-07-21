package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
public class FormUserDetails implements UserDetails, CredentialsContainer, AmaazonPrincipal {

  private final UUID id;
  private final String role;
  private String password;

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
    return List.of(new SimpleGrantedAuthority(role));
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
  public void eraseCredentials() {
    this.password = null;
  }
}
