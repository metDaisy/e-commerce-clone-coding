package io.github.metdaisy.amaazon.global.security.jwt;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  private final Cache<String, JwtToken> tokenCache;
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
  public void register(JwtToken jwtToken) {
    tokenCache.put(jwtToken.token(), jwtToken);
    userTokenSet.computeIfAbsent(jwtToken.userId(), k -> ConcurrentHashMap.newKeySet())
        .add(jwtToken.token());
  }

  @Override
  public Optional<JwtToken> findByToken(String token) {
    return Optional.ofNullable(tokenCache.getIfPresent(token));
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

  @Override
  public boolean isActiveSession(UUID userId, String device) {
    Set<String> tokens = userTokenSet.getOrDefault(userId, Collections.emptySet());
    return tokens.stream().map(tokenCache::getIfPresent).filter(Objects::nonNull)
        .anyMatch(jwtToken -> jwtToken.device().equals(device));
  }

  private void onTokenRemoved(String token, JwtToken jwtToken, RemovalCause cause) {
    if (jwtToken == null) {
      return;
    }
    userTokenSet.computeIfPresent(jwtToken.userId(),
        (userId, tokens) -> removeAndCheckEmpty(tokens, token));
  }

  private Set<String> removeAndCheckEmpty(Set<String> tokens, String token) {
    tokens.remove(token);
    return tokens.isEmpty() ? null : tokens;
  }
}
