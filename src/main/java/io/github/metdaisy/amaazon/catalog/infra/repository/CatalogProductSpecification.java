package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class CatalogProductSpecification {

  public Specification<CatalogProduct> hasId(UUID id) {
    return (root, query, cb)
        -> cb.equal(root.get("id"), id);
  }

  public Specification<CatalogProduct> hasIdNot(UUID id) {
    return (root, query, cb)
        -> cb.notEqual(root.get("id"), id);
  }

  public Specification<CatalogProduct> hasIdentifier(CatalogIdentifierType type,
      String value) {
    String field = switch (type) {
      case ASIN -> "asin";
      case EAN -> "ean";
      case UPC -> "upc";
      case GTIN -> "gtin";
      case ISBN -> "isbn";
    };
    return (root, query, cb)
        -> cb.equal(root.get(field), value);
  }
}
