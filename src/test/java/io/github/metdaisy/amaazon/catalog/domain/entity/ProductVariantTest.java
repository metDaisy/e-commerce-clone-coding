package io.github.metdaisy.amaazon.catalog.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("상품 옵션 도메인")
class ProductVariantTest {

  @Test
  @DisplayName("상품 옵션 생성: 유효한 표시명과 속성으로 ACTIVE 상태를 생성한다")
  void of_shouldCreateActiveVariantWithAttributes() {
    ProductVariant variant = ProductVariant.of(product(), "Black / 256GB",
        Map.of("color", "BLACK"));

    assertThat(variant.getDisplayName()).isEqualTo("Black / 256GB");
    assertThat(variant.getAttributes()).containsEntry("color", "BLACK");
    assertThat(variant.getPublicationStatus()).isEqualTo(CatalogStatus.ACTIVE);
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 공백 표시명은 거절한다")
  void of_shouldRejectBlankDisplayName() {
    assertThatThrownBy(() -> ProductVariant.of(product(), " ", Map.of()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_INVALID.getCode());
  }

  @Test
  @DisplayName("상품 옵션 수정: 속성은 병합하고 null 값은 기존 키를 삭제한다")
  void update_shouldMergeAttributesAndRemoveNullValues() {
    ProductVariant variant = ProductVariant.of(product(), "Black / 256GB",
        Map.of("color", "BLACK", "storage", "256GB"));

    Map<String, Object> patch = new LinkedHashMap<>();
    patch.put("storage", "512GB");
    patch.put("color", null);
    variant.update("Black / 512GB", patch);

    assertThat(variant.getDisplayName()).isEqualTo("Black / 512GB");
    assertThat(variant.getAttributes()).containsOnlyKeys("storage")
        .containsEntry("storage", "512GB");
  }

  @Test
  @DisplayName("상품 옵션 보관 후 수정: ARCHIVED 옵션은 CATALOG-033으로 거절한다")
  void update_shouldRejectArchivedVariant() {
    ProductVariant variant = ProductVariant.of(product(), "Black / 256GB", Map.of());
    variant.archive();

    assertThatThrownBy(() -> variant.update("Black / 512GB", Map.of()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_ARCHIVED.getCode());
  }

  @Test
  @DisplayName("상품 옵션 재보관: 이미 ARCHIVED인 옵션은 CATALOG-035로 거절한다")
  void archive_shouldRejectAlreadyArchivedVariant() {
    ProductVariant variant = ProductVariant.of(product(), "Black / 256GB", Map.of());
    variant.archive();

    assertThatThrownBy(variant::archive)
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code",
            ProductVariantErrorCode.VARIANT_ALREADY_ARCHIVED.getCode());
  }

  private CatalogProduct product() {
    return CatalogProduct.builder()
        .category(Category.of("Computers", null))
        .name("Laptop")
        .description("Portable computer")
        .asin("B000123456")
        .build();
  }
}
