package io.github.metdaisy.amaazon.global.infra.cache.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.modulith.NamedInterface;

@NamedInterface("cache-constants")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheConstants {

  public static final String CATEGORIES = "categories";
}
