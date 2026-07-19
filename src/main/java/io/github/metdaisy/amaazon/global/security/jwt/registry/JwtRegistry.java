package io.github.metdaisy.amaazon.global.security.jwt.registry;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
public interface JwtRegistry {

  void blacklistToken(String jti, Instant expiredAt);

  void blacklistUser(UUID userId, Instant compromisedAt);

  boolean isBlacklisted(String jti, UUID userId, Instant issuedAt);

}
