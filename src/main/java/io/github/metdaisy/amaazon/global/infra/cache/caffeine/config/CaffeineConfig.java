package io.github.metdaisy.amaazon.global.infra.cache.caffeine.config;

import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.metdaisy.amaazon.global.security.jwt.config.CaffeineCacheAsyncConfig;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;
import io.github.metdaisy.amaazon.global.security.jwt.registry.CaffeineBlacklistRegistry;

@Configuration
@ConditionalOnProperty(value = "amaazon.cache.type", havingValue = "caffeine")
@Import({CaffeineBlacklistRegistry.class, CaffeineCacheAsyncConfig.class})
public class CaffeineConfig {

  private final Executor caffeineWorker;
  private final JwtTokenExpiration jwtTokenExpiration;
  private final int cacheCapacity;

  public CaffeineConfig(
      JwtTokenExpiration jwtTokenExpiration,
      @Value("${amaazon.cache.capacity}") int cacheCapacity,
      @Qualifier("caffeineWorker") Executor caffeineWorker) {
    this.jwtTokenExpiration = jwtTokenExpiration;
    this.cacheCapacity = cacheCapacity;
    this.caffeineWorker = caffeineWorker;
  }

  @Bean
  public CacheManager caffeineManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .maximumSize(cacheCapacity)
        .expireAfterWrite(jwtTokenExpiration.accessExpiration())
        .executor(caffeineWorker)
        .recordStats());
    manager.registerCustomCache("categories", Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterAccess(Duration.ofHours(1))
        .executor(caffeineWorker)
        .recordStats()
        .build());
    return manager;
  }

  @Bean(name = "tokenBlacklistCache")
  public Cache<String, Instant> tokenBlacklistCache() {
    return Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .expireAfter(new CaffeineExpiry<String>())
        .executor(caffeineWorker)
        .build();
  }

  @Bean(name = "userBlacklistCache")
  public Cache<UUID, Instant> userBlacklistCache() {
    return Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .expireAfter(new CaffeineExpiry<UUID>())
        .executor(caffeineWorker)
        .build();
  }

  private static class CaffeineExpiry<T> implements Expiry<T, Instant> {

    @Override
    public long expireAfterCreate(T key, Instant value, long currentTime) {
      long ttlNanos = Duration.between(Instant.now(), value).toNanos();
      return ttlNanos > 0 ? ttlNanos : 0;
    }

    @Override
    public long expireAfterUpdate(T key, Instant value, long currentTime,
            long currentDuration) {
      return currentDuration;
    }

    @Override
    public long expireAfterRead(T key, Instant value, long currentTime, long currentDuration) {
      return currentDuration;
    }
  }
}
