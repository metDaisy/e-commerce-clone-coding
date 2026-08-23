package io.github.metdaisy.amaazon.catalog.support.fixture;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProductVariantFixture {

  private ProductVariantFixture() {
  }

  public static ProductVariant variant(CatalogProduct catalogProduct) {
    return ProductVariant.of(catalogProduct, "Black / 256GB",
        Map.of("color", "BLACK", "storage", "256GB"));
  }

  public static ProductVariantCreateRequest createRequest() {
    return new ProductVariantCreateRequest("Black / 256GB",
        Map.of("color", "BLACK", "storage", "256GB"));
  }

  public static ProductVariantUpdateRequest updateRequest() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("storage", "512GB");
    attributes.put("color", null);
    return new ProductVariantUpdateRequest("Black / 512GB",
        attributes);
  }
}
