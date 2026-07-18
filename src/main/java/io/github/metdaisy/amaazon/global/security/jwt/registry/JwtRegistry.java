package io.github.metdaisy.amaazon.global.security.jwt.registry;

import io.github.metdaisy.amaazon.global.security.jwt.dto.JwtToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
public interface JwtRegistry {

  void register(JwtToken token);

  Optional<JwtToken> findByToken(String token);

  void invalidateAllByUserId(UUID userId);

  void invalidateByToken(String token);

  boolean isActiveSession(UUID userId, String device);
}
