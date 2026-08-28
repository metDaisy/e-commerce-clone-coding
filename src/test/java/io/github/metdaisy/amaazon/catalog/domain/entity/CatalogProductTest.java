package io.github.metdaisy.amaazon.catalog.domain.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("카탈로그 상품 도메인")
class CatalogProductTest {

  @Test
  @DisplayName("상품 상태 검증: 활성 상품은 통과한다")
  void validateActive_shouldAllowActiveProduct() {
    CatalogProduct product = product();

    assertThatCode(product::validateActive).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("상품 상태 검증 실패: 보관된 상품은 수정할 수 없다")
  void validateActive_shouldRejectArchivedProduct() {
    CatalogProduct product = product();
    product.setPublicationStatus(ArchiveStatus.ARCHIVED);

    assertThatThrownBy(product::validateActive)
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code",
            CatalogProductErrorCode.CATALOG_PRODUCT_ARCHIVED.getCode());
  }

  private CatalogProduct product() {
    return CatalogProduct.builder()
        .category(Category.of("Computers", null))
        .name("Laptop")
        .description("Portable computer")
        .build();
  }
}
