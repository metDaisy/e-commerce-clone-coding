package io.github.metdaisy.amaazon.catalog.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import java.util.Map;
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
    assertThat(variant.getPublicationStatus()).isEqualTo(ArchiveStatus.ACTIVE);
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: attributes가 null이면 거절한다")
  void of_shouldRejectNullAttributes() {
    assertThatThrownBy(() -> ProductVariant.of(product(), "Black / 256GB", null))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_INVALID.getCode());
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
