package io.github.metdaisy.amaazon.catalog.domain.entity.constant;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CatalogIdentifierType {

  public final String ASIN = "asin";
  public final String GTIN = "gtin";
  public final String UPC = "upc";
  public final String EAN = "ean";
  public final String ISBN = "isbn";

  public final Set<String> types = Set.of(ASIN, GTIN, UPC, EAN, ISBN);
}
