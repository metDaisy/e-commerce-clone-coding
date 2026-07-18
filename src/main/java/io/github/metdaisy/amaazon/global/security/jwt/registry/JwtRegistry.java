package io.github.metdaisy.amaazon.global.security.jwt.registry;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
public interface JwtRegistry {

  void register(UUID userId, String token);

  UUID findByToken(String token);

  void invalidateAllByUserId(UUID userId);

  void invalidateByToken(String token);

}
