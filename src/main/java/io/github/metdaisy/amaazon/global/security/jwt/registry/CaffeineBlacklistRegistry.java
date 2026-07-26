package io.github.metdaisy.amaazon.global.security.jwt.registry;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistTokenCreatedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistUserCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;

public class CaffeineBlacklistRegistry implements BlacklistRegistry {

  // token: jti
  private final Cache<String, Instant> tokenBlacklist;
  private final Cache<UUID, Instant> userBlacklist;
  private final ApplicationEventPublisher eventPublisher;

  public CaffeineBlacklistRegistry(
          @Qualifier("tokenBlacklistCache") Cache<String, Instant> tokenBlacklist,
          @Qualifier("userBlacklistCache") Cache<UUID, Instant> userBlacklist,
          ApplicationEventPublisher eventPublisher) {
    this.tokenBlacklist = tokenBlacklist;
    this.userBlacklist = userBlacklist;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void blacklistToken(String jti, Instant expiredAt) {
    tokenBlacklist.put(jti, expiredAt);
    eventPublisher.publishEvent(new BlacklistTokenCreatedEvent(jti, expiredAt));
  }

  @Override
  public void blacklistUser(UUID userId, Instant compromisedAt) {
    userBlacklist.put(userId, compromisedAt);
    eventPublisher.publishEvent(new BlacklistUserCreatedEvent(userId, compromisedAt));
  }

  @Override
  public boolean isBlacklisted(String jti, UUID userId, Instant issuedAt) {
    if (tokenBlacklist.getIfPresent(jti) != null) {
      return true;
    }

    Instant compromisedAt = userBlacklist.getIfPresent(userId);
    return !(compromisedAt == null || issuedAt.isAfter(compromisedAt));
  }
}
