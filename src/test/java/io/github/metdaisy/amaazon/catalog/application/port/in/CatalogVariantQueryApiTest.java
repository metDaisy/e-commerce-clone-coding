package io.github.metdaisy.amaazon.catalog.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 옵션 공개 application API")
class CatalogVariantQueryApiTest {

  @Mock
  private ProductVariantRepository repository;

  @InjectMocks
  private CatalogVariantQueryApi api;

  @Test
  @DisplayName("활성 상품 옵션 조회: Variant와 CatalogProduct ID 참조를 반환한다")
  void findActiveByVariantId_shouldReturnReferenceForActiveVariant() {
    ProductVariant variant = variant();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    Optional<CatalogVariantQueryApi.CatalogVariantReference> result =
        api.findActiveByVariantId(variant.getId());

    assertThat(result).get().satisfies(reference -> {
      assertThat(reference.variantId()).isEqualTo(variant.getId());
      assertThat(reference.catalogProductId()).isEqualTo(variant.getCatalogProduct().getId());
    });
  }

  @Test
  @DisplayName("보관 상품 옵션 조회: 활성 옵션이 아니면 빈 결과를 반환한다")
  void findActiveByVariantId_shouldReturnEmptyForArchivedVariant() {
    ProductVariant variant = variant();
    variant.archive();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    assertThat(api.findActiveByVariantId(variant.getId())).isEmpty();
  }

  private ProductVariant variant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(CategoryFixture.category());
    return ProductVariantFixture.variant(product);
  }
}
