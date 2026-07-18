package io.github.metdaisy.amaazon.global.security.jwt.registry;

import io.github.metdaisy.amaazon.global.security.jwt.exception.JwtTokenNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

@Component
@ConditionalOnProperty(value = "amaazon.jwt.registry-store-type", havingValue = "caffeine")
public class CaffeineJwtRegistry implements JwtRegistry {

  private final Cache<String, UUID> tokenCache;
  private final Map<UUID, Set<String>> userTokenSet;

  public CaffeineJwtRegistry(@Qualifier("caffeineWorker") Executor caffeineWorker,
      @Value("${amaazon.jwt.refresh-token-expiration}") long refreshTokenExpiration,
      @Value("${amaazon.cache.caffeine.capacity}") int cacheCapacity) {
    this.userTokenSet = new ConcurrentHashMap<>();
    this.tokenCache = Caffeine.newBuilder().initialCapacity(cacheCapacity)
        .expireAfterWrite(refreshTokenExpiration, TimeUnit.SECONDS).executor(caffeineWorker)
        .removalListener(this::onTokenRemoved).build();
  }

  @Override
  public void register(UUID userId, String token) {
    tokenCache.put(token, userId);
    userTokenSet.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
        .add(token);
  }

  @Override
  public UUID findByToken(String token) {
    UUID userId = tokenCache.getIfPresent(token);
    if (userId == null) {
      throw new JwtTokenNotFoundException(token);
    }
    return userId;
  }

  @Override
  public void invalidateAllByUserId(UUID userId) {
    Set<String> tokens = userTokenSet.getOrDefault(userId, Collections.emptySet());
    tokenCache.invalidateAll(tokens);
  }

  @Override
  public void invalidateByToken(String token) {
    tokenCache.invalidate(token);
  }

  private void onTokenRemoved(String token, UUID userId, RemovalCause cause) {
    if (userId == null) {
      return;
    }
    userTokenSet.computeIfPresent(userId,
        (id, tokens) -> removeAndCheckEmpty(tokens, token));
  }

  private Set<String> removeAndCheckEmpty(Set<String> tokens, String token) {
    tokens.remove(token);
    return tokens.isEmpty() ? null : tokens;
  }
}
