package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestTokenService {

  private final JwtTokenProvider jwtTokenProvider;

  public String create(String provider, String providerId) {
    return jwtTokenProvider.generateGuestToken(provider, providerId);
  }

  public String getProvider(String token) {
    return jwtTokenProvider.parseProvider(token, "provider");
  }

  public String getProviderId(String token) {
    return jwtTokenProvider.parseProvider(token, "providerId");
  }
}
