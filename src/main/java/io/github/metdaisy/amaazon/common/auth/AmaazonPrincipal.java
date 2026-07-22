package io.github.metdaisy.amaazon.common.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@NamedInterface
public interface AmaazonPrincipal {

  UUID getId();

  String getRole();

  default Collection<? extends GrantedAuthority> getAuthorities() {
    return Arrays.stream(getRole().split(","))
            .map(SimpleGrantedAuthority::new)
            .toList();
  }
}
