package io.github.metdaisy.amaazon.catalog.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.common.mapper.UtilMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductTagMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.TagMapperImpl;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("상품 옵션 매퍼")
class ProductVariantMapperTest {

  private final ProductVariantMapper mapper = new ProductVariantMapperImpl(
      new CatalogProductMapperImpl(new CategoryMapperImpl(new UtilMapperImpl()),
          new CatalogProductTagMapperImpl(new TagMapperImpl()), new UtilMapperImpl()),
      new UtilMapperImpl());

  @Test
  @DisplayName("상품 옵션 수정: 표시명과 attributes를 JSON Merge Patch 규칙으로 반영한다")
  void update_shouldMergeDisplayNameAndAttributes() {
    CatalogProduct product = CatalogProductFixture.product(CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    Map<String, Object> patch = new LinkedHashMap<>();
    patch.put("storage", "512GB");
    patch.put("color", null);
    patch.put("material", "ALUMINUM");

    mapper.update(variant, new ProductVariantUpdateRequest("Black / 512GB", patch)
    );

    assertThat(variant.getDisplayName()).isEqualTo("Black / 512GB");
    assertThat(variant.getAttributes()).containsExactlyInAnyOrderEntriesOf(
        Map.of("storage", "512GB", "material", "ALUMINUM"));
  }
}
