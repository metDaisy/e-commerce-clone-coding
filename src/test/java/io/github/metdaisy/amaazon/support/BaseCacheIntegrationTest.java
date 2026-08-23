package io.github.metdaisy.amaazon.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import io.github.metdaisy.amaazon.global.infra.cache.constant.CacheConstants;

@Import(QueryInspectorConfig.class)
public abstract class BaseCacheIntegrationTest extends BaseIntegrationTest {

  @Autowired
  protected CacheManager cacheManager;

  @Autowired
  protected QueryInspector queryInspector;

  @Override
  protected void clear() {
    super.clear();
    queryInspector.clear();
  }

  @BeforeEach
  void clearCacheState() {
    cacheManager.getCacheNames().forEach(cacheName -> {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    });
    queryInspector.clear();
  }

  protected Cache categoriesCache() {
    Cache cache = cacheManager.getCache(CacheConstants.CATEGORIES);
    if (cache == null) {
      throw new IllegalStateException(CacheConstants.CATEGORIES + " cache is not configured");
    }
    return cache;
  }
}
