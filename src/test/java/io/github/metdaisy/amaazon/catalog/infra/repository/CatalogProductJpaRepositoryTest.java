package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

@DisplayName("카탈로그 상품 JPA 저장소")
class CatalogProductJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CatalogProductJpaRepository repository;

  @Test
  @DisplayName("식별자 중복 확인: 생성 시 동일 식별자를 한 번의 조회로 찾는다")
  void existsIdentifier_shouldFindDuplicateWhenCurrentProductIdIsNull() {
    CatalogProduct product = persistProduct("B000123456");
    flushAndClear();

    assertThat(repository.existsIdentifier(null, CatalogProductIdentifierType.ASIN,
        product.getAsin())).isTrue();

    ensureQueryCount(1);
  }

  @Test
  @DisplayName("식별자 중복 확인: 수정 시 현재 상품은 제외하고 한 번의 조회로 확인한다")
  void existsIdentifier_shouldExcludeCurrentProduct() {
    CatalogProduct product = persistProduct("B000123456");
    flushAndClear();

    assertThat(repository.existsIdentifier(product.getId(), CatalogProductIdentifierType.ASIN,
        product.getAsin())).isFalse();

    ensureQueryCount(1);
  }

  @Test
  @DisplayName("논리 삭제: 삭제 쿼리 후 아카이브 상품은 일반 조회에서 제외된다")
  void delete_shouldHideArchivedProductFromFindById() {
    CatalogProduct product = persistProduct("B000123456");
    clear();

    repository.deleteById(product.getId());
    em.flush();

    ensureQueryCount(3);
    clear();

    assertThat(((JpaRepository<CatalogProduct, UUID>) repository).findById(product.getId()))
        .isEmpty();
    ensureQueryCount(1);
  }

  private CatalogProduct persistProduct(String asin) {
    Category category = persistAndFlush(Category.of("Computers", null));
    return persistAndFlush(CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .asin(asin)
        .build());
  }
}
