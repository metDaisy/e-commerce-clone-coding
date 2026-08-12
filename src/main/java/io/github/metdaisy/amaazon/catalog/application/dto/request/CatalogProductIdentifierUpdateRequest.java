package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public record CatalogProductIdentifierUpdateRequest(String asin, String gtin, String upc,
                                                    String ean,
                                                    String isbn) {

  public Map<CatalogProductIdentifierType, String> toMap() {
    Map<CatalogProductIdentifierType, String> map = new HashMap<>();
    if (StringUtils.hasText(asin())) {
      map.put(CatalogProductIdentifierType.ASIN, asin());
    }
    if (StringUtils.hasText(gtin())) {
      map.put(CatalogProductIdentifierType.GTIN, gtin());
    }
    if (StringUtils.hasText(upc())) {
      map.put(CatalogProductIdentifierType.UPC, upc());
    }
    if (StringUtils.hasText(ean())) {
      map.put(CatalogProductIdentifierType.EAN, ean());
    }
    if (StringUtils.hasText(isbn())) {
      map.put(CatalogProductIdentifierType.ISBN, isbn());
    }
    return map;
  }
}
