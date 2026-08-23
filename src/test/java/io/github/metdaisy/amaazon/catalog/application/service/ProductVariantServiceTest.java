package io.github.metdaisy.amaazon.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapperImpl;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 옵션 서비스")
class ProductVariantServiceTest {

  @Mock
  private ProductVariantRepository repository;

  @Mock
  private CatalogProductRepository catalogProductRepository;

  @Spy
  private ProductVariantMapper mapper = new ProductVariantMapperImpl();

  @InjectMocks
  private ProductVariantService service;

  @Test
  @DisplayName("상품 옵션 생성: 활성 상품에 옵션을 저장하고 관리자 응답으로 변환한다")
  void create_shouldPersistVariantForActiveProduct() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariantCreateRequest request = ProductVariantFixture.createRequest();
    given(catalogProductRepository.findById(product.getId())).willReturn(Optional.of(product));
    given(repository.save(any(ProductVariant.class))).willAnswer(invocation -> invocation.getArgument(0));

    ProductVariantAdminResponse result = service.create(product.getId(), request);

    assertThat(result.catalogProductId()).isEqualTo(product.getId());
    assertThat(result.displayName()).isEqualTo("Black / 256GB");
    then(repository).should().save(any(ProductVariant.class));
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 보관된 상품은 CATALOG-019로 거절한다")
  void create_shouldRejectArchivedCatalogProduct() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    product.setPublicationStatus(CatalogStatus.ARCHIVED);
    given(catalogProductRepository.findById(product.getId())).willReturn(Optional.of(product));

    assertThatThrownBy(() -> service.create(product.getId(), ProductVariantFixture.createRequest()))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.CATALOG_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 존재하지 않는 상품은 CATALOG-019로 거절한다")
  void create_shouldRejectUnknownCatalogProduct() {
    UUID productId = UUID.randomUUID();
    given(catalogProductRepository.findById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(productId, ProductVariantFixture.createRequest()))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.CATALOG_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("상품 옵션 공개 조회: 보관 옵션은 CATALOG-031로 거절한다")
  void findPublic_shouldRejectArchivedVariant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    assertThatThrownBy(() -> service.findPublic(variant.getId()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("상품 옵션 공개 조회 실패: 보관된 상품에 속한 옵션은 CATALOG-031로 거절한다")
  void findPublic_shouldRejectVariantOfArchivedCatalogProduct() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    product.setPublicationStatus(CatalogStatus.ARCHIVED);
    ProductVariant variant = ProductVariantFixture.variant(product);
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    assertThatThrownBy(() -> service.findPublic(variant.getId()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("상품 옵션 수정: 표시명과 속성 병합 결과를 관리자 응답으로 반환한다")
  void update_shouldMergeRequestIntoActiveVariant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    ProductVariantUpdateRequest request = ProductVariantFixture.updateRequest();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    ProductVariantAdminResponse result = service.update(variant.getId(), request);

    assertThat(result.displayName()).isEqualTo("Black / 512GB");
    assertThat(result.attributes()).containsOnlyKeys("storage");
    assertThat(result.productVariantId()).isEqualTo(variant.getId());
    assertThat(result.catalogProductId()).isEqualTo(product.getId());
  }

  @Test
  @DisplayName("상품 옵션 수정 실패: 보관된 옵션은 CATALOG-033으로 거절한다")
  void update_shouldRejectArchivedVariant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    assertThatThrownBy(() -> service.update(variant.getId(), ProductVariantFixture.updateRequest()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_ARCHIVED.getCode());
  }

  @Test
  @DisplayName("상품 옵션 보관 실패: 이미 보관된 옵션은 CATALOG-035로 거절한다")
  void archive_shouldRejectAlreadyArchivedVariant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    assertThatThrownBy(() -> service.archive(variant.getId()))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_ALREADY_ARCHIVED
            .getCode());
  }

  @Test
  @DisplayName("관리자 상품 옵션 조회: 보관된 옵션도 내부 상태를 포함해 반환한다")
  void findAdmin_shouldReturnArchivedVariant() {
    CatalogProduct product = CatalogProductFixture.persistedProduct(
        CategoryFixture.category());
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    given(repository.findWithCatalogProductById(variant.getId())).willReturn(Optional.of(variant));

    ProductVariantAdminResponse result = service.findAdmin(variant.getId());

    assertThat(result.productVariantId()).isEqualTo(variant.getId());
    assertThat(result.catalogProductId()).isEqualTo(product.getId());
    assertThat(result.publicationStatus()).isEqualTo(CatalogStatus.ARCHIVED);
    assertThat(result.archivedAt()).isNotNull();
  }

  @Test
  @DisplayName("상품 옵션 조회 실패: 존재하지 않는 옵션은 CATALOG-031로 거절한다")
  void findAdmin_shouldRejectUnknownVariant() {
    UUID variantId = UUID.randomUUID();
    given(repository.findWithCatalogProductById(variantId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.findAdmin(variantId))
        .isInstanceOf(ProductVariantException.class)
        .hasFieldOrPropertyWithValue("code", ProductVariantErrorCode.VARIANT_NOT_FOUND.getCode());
  }
}
