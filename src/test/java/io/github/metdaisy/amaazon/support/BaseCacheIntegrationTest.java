package io.github.metdaisy.amaazon.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;

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
    Cache cache = cacheManager.getCache("categories");
    if (cache == null) {
      throw new IllegalStateException("categories cache is not configured");
    }
    return cache;
  }
}
