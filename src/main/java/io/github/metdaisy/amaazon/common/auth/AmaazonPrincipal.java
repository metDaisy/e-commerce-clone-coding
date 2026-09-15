package io.github.metdaisy.amaazon.common.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

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

  default boolean hasRole(String role) {
    if (!StringUtils.hasText(role)) {
      return false;
    }
    String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
    String authority = normalizedRole.startsWith("ROLE_")
        ? normalizedRole
        : "ROLE_" + normalizedRole;
    return getAuthorities().stream()
        .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
  }
}
