package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("상품 옵션 JPA 저장소")
class ProductVariantJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private ProductVariantJpaRepository repository;

  @Test
  @DisplayName("상품 옵션 저장: 유효한 옵션을 저장하고 ACTIVE 상태를 유지한다")
  void save_shouldPersistValidVariant() {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    queryInspector.clear();
    ProductVariant variant = ProductVariantFixture.variant(product);

    ProductVariant saved = repository.save(variant);
    em.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicationStatus()).isEqualTo(ArchiveStatus.ACTIVE);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 옵션 상세 조회: 상품 연관관계와 모든 필드를 한 번의 쿼리로 조회한다")
  void findWithCatalogProductById_shouldFetchAllFieldsInOneQuery() {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    flushAndClear();

    ProductVariant found = repository.findWithCatalogProductById(variant.getId()).orElseThrow();

    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("catalogProduct")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(variant);
    assertThat(found.getCatalogProduct().getId()).isEqualTo(product.getId());
    assertThat(found.getCatalogProduct().getName()).isEqualTo(product.getName());
    assertThat(found.getDisplayName()).isEqualTo("Black / 256GB");
    assertThat(found.getAttributes()).containsEntry("storage", "256GB");
    assertThat(found.getPublicationStatus()).isEqualTo(ArchiveStatus.ACTIVE);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("기본 ID 조회: 모든 필드와 지연 부모를 접근하면 두 번의 쿼리가 발생한다")
  void findById_shouldLoadAllFieldsAndLazyCatalogProduct() {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    flushAndClear();

    ProductVariant found = repository.findById(variant.getId()).orElseThrow();

    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("catalogProduct")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(variant);
    assertThat(found.getCatalogProduct().getId()).isEqualTo(product.getId());
    assertThat(found.getCatalogProduct().getName()).isEqualTo(product.getName());
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("상품 옵션 보관: 상태와 보관 시각을 영속화한다")
  void archive_shouldPersistArchivedState() {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();
    ProductVariant managed = repository.findWithCatalogProductById(variant.getId()).orElseThrow();
    queryInspector.clear();

    managed.archive();
    repository.save(managed);
    em.flush();
    clear();
    ProductVariant archived = repository.findWithCatalogProductById(variant.getId()).orElseThrow();

    assertThat(archived.getPublicationStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
    assertThat(archived.getArchivedAt()).isNotNull();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 옵션 삭제: 물리 삭제하지 않고 ARCHIVED 상태로 전환한다")
  void delete_shouldArchivePersistedVariant() {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    flushAndClear();
    ProductVariant managed = repository.findById(variant.getId()).orElseThrow();
    queryInspector.clear();

    repository.delete(managed);
    em.flush();

    ensureQueryCount(1);
    clear();
    ProductVariant archived = repository.findById(variant.getId()).orElseThrow();
    assertThat(archived.getPublicationStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
    assertThat(archived.getArchivedAt()).isNotNull();
  }
}
