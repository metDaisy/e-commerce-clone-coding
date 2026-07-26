package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class SocialUserDetails implements OAuth2User, AmaazonPrincipal {

  private final UUID id;
  private final String role;
  private final Map<String, Object> attributes;

  private SocialUserDetails(UUID id, String role, Map<String, Object> attributes) {
    this.id = id;
    this.role = role;
    this.attributes = attributes;
  }

  public static SocialUserDetails create(UUID id, String role, Map<String, Object> attributes) {
    return new SocialUserDetails(id, role, attributes);
  }

  public static SocialUserDetails createGuest(Map<String, Object> attributes) {
    return new SocialUserDetails(null, "guest", attributes);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AmaazonPrincipal.super.getAuthorities();
  }

  @Override
  public String getName() {
    if (id != null) {
      return id.toString();
    }
    String provider = (String) attributes.get("provider");
    String providerId = (String) attributes.get("providerId");
    return "guest:" + provider + ":" + providerId;
  }
}