package io.github.metdaisy.amaazon.catalog.support.fixture;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CatalogProductFixture {

  private CatalogProductFixture() {
  }

  public static CatalogProduct product(Category category) {
    return CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .brand("Brand")
        .build();
  }

  public static CatalogProduct persistedProduct(Category category) {
    return CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .brand("Brand")
        .asin("B000123456")
        .build();
  }

  public static CatalogProductCreateRequest createRequest(UUID categoryId) {
    return createRequest(categoryId, Set.of(), Map.of(),
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));
  }

  public static CatalogProductCreateRequest createRequest(UUID categoryId,
      Map<CatalogIdentifierType, String> identifiers) {
    return createRequest(categoryId, Set.of(), Map.of(), identifiers);
  }

  public static CatalogProductCreateRequest createRequestWithoutIdentifiers(UUID categoryId) {
    return new CatalogProductCreateRequest(categoryId, "Laptop", "Portable computer", "Brand",
        Set.of(), Map.of(), null);
  }

  public static CatalogProductCreateRequest createRequest(UUID categoryId, Set<String> tags,
      Map<String, Object> attributes, Map<CatalogIdentifierType, String> identifiers) {
    return new CatalogProductCreateRequest(categoryId, "Laptop", "Portable computer", "Brand",
        tags, attributes, identifiers);
  }

  public static CatalogProductUpdateRequest updateRequest() {
    return new CatalogProductUpdateRequest(
        "Updated laptop", "Updated description", "New brand", List.of("sale"), Map.of());
  }
}
