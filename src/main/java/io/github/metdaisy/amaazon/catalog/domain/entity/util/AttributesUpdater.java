package io.github.metdaisy.amaazon.catalog.domain.entity.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AttributesUpdater {

  private AttributesUpdater() {
  }

  public static Map<String, Object> update(Map<String, Object> current,
      Map<String, Object> newAttributes) {
    Map<String, Object> merged =
        current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
    newAttributes.forEach(
        (key, value) -> {
          if (value == null) {
            merged.remove(key);
          } else {
            merged.put(key, value);
          }
        });
    return merged;
  }
}
