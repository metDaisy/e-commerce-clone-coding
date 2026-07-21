package io.github.metdaisy.amaazon.global.infra.cache.caffeine.config;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import io.github.metdaisy.amaazon.global.security.jwt.registry.CaffeineJwtRegistry;

@Configuration
@ConditionalOnProperty(value = "amaazon.jwt.registry-store-type", havingValue = "caffeine")
@Import({CaffeineJwtRegistry.class, CaffeineCacheAsyncConfig.class})
public class CaffeineConfig {

  private final Executor caffeineWorker;
  private final JwtProperties properties;
  private final int cacheCapacity;

  public CaffeineConfig(
      JwtProperties properties,
      @Value("${amaazon.cache.caffeine.capacity}") int cacheCapacity,
      @Qualifier("caffeineWorker") Executor caffeineWorker) {
    this.properties = properties;
    this.cacheCapacity = cacheCapacity;
    this.caffeineWorker = caffeineWorker;
  }

  @Bean
  public CacheManager caffeineManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .maximumSize(cacheCapacity)
        .expireAfterWrite(properties.accessTokenExpiration(), TimeUnit.SECONDS)
        .executor(caffeineWorker)
        .recordStats());
    return manager;
  }

  @Bean(name = "tokenBlacklistCache")
  public Cache<String, Boolean> tokenBlacklistCache() {
    return Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .expireAfterWrite(properties.accessTokenExpiration(), TimeUnit.SECONDS)
        .executor(caffeineWorker)
        .build();
  }

  @Bean(name = "userBlacklistCache")
  public Cache<UUID, Instant> userBlacklistCache() {
    return Caffeine.newBuilder()
        .initialCapacity(cacheCapacity)
        .expireAfterWrite(properties.refreshTokenExpiration(), TimeUnit.SECONDS)
        .executor(caffeineWorker)
        .build();
  }
}
