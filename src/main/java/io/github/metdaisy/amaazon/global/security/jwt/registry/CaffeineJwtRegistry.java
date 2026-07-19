package io.github.metdaisy.amaazon.global.security.jwt.registry;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
@ConditionalOnProperty(value = "amaazon.jwt.registry-store-type", havingValue = "caffeine")
public class CaffeineJwtRegistry implements JwtRegistry {

  private final Cache<String, Boolean> tokenBlacklist;
  private final Cache<UUID, Instant> userBlacklist;

  public CaffeineJwtRegistry(
          @Qualifier("caffeineWorker") Executor caffeineWorker,
          @Value("${amaazon.jwt.access-token-expiration}") long accessTokenExpiration,
          @Value("${amaazon.cache.caffeine.capacity}") int cacheCapacity) {

    this.tokenBlacklist = Caffeine.newBuilder()
            .initialCapacity(cacheCapacity)
            .expireAfterWrite(accessTokenExpiration, TimeUnit.SECONDS)
            .executor(caffeineWorker)
            .build();

    this.userBlacklist = Caffeine.newBuilder()
            .initialCapacity(cacheCapacity)
            .expireAfterWrite(accessTokenExpiration, TimeUnit.SECONDS)
            .executor(caffeineWorker)
            .build();
  }

  @Override
  public void blacklistToken(String accessToken) {
    tokenBlacklist.put(accessToken, Boolean.TRUE);
  }

  @Override
  public void blacklistUser(UUID userId, Instant compromisedAt) {
    userBlacklist.put(userId, compromisedAt);
  }

  @Override
  public boolean isBlacklisted(String accessToken, UUID userId, Instant issuedAt) {
    if (tokenBlacklist.getIfPresent(accessToken) != null) {
      return true;
    }

    Instant compromisedAt = userBlacklist.getIfPresent(userId);
    return !(compromisedAt == null || issuedAt.isAfter(compromisedAt));
  }
}
