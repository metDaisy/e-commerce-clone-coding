package io.github.metdaisy.amaazon.common.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@NamedInterface
public interface AmaazonPrincipal {

  UUID getId();

  String getRole();

  boolean isEnabled();

  default Collection<? extends GrantedAuthority> getAuthorities() {
    return Arrays.stream(getRole().split(","))
        .map(role -> "ROLE_" + role.toUpperCase())
        .map(SimpleGrantedAuthority::new)
        .toList();
  }
}
