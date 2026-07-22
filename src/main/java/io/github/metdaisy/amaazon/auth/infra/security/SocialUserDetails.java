package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
@RequiredArgsConstructor
public class SocialUserDetails implements OAuth2User, AmaazonPrincipal {

  private final UUID id;
  private final String role;
  private final Map<String, Object> attributes;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AmaazonPrincipal.super.getAuthorities();
  }

  @Override
  public String getName() {
    return id.toString();
  }
}
