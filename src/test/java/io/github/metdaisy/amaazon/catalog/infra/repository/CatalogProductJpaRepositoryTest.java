package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.transaction.BeforeTransaction;

@DisplayName("카탈로그 상품 JPA 저장소")
class CatalogProductJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CatalogProductJpaRepository repository;

  @Autowired
  private TagJpaRepository tagRepository;

  private Tag officeTag;
  private Tag saleTag;
  private Tag laptopTag;

  @BeforeTransaction
  void persistTagsBeforeTestTransaction() {
    officeTag = tagRepository.saveAndFlush(new Tag("office-" + UUID.randomUUID()));
    saleTag = tagRepository.saveAndFlush(new Tag("sale-" + UUID.randomUUID()));
    laptopTag = tagRepository.saveAndFlush(new Tag("laptop-" + UUID.randomUUID()));
  }

  @Test
  @DisplayName("식별자 중복 확인: 생성 시 동일 식별자를 한 번의 조회로 찾는다")
  void existsIdentifier_shouldFindDuplicateWhenCurrentProductIdIsNull() {
    CatalogProduct product = persistProduct("B000123456");
    flushAndClear();

    assertThat(repository.existsIdentifier(null, CatalogIdentifierType.ASIN,
        product.getAsin())).isTrue();

    ensureQueryCount(1);
  }

  @Test
  @DisplayName("식별자 중복 확인: 수정 시 현재 상품은 제외하고 한 번의 조회로 확인한다")
  void existsIdentifier_shouldExcludeCurrentProduct() {
    CatalogProduct product = persistProduct("B000123456");
    flushAndClear();

    assertThat(repository.existsIdentifier(product.getId(), CatalogIdentifierType.ASIN,
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

    ensureQueryCount(2);
    clear();

    CatalogProduct archived = repository.findById(product.getId()).orElseThrow();
    assertThat(archived.getPublicationStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 아카이빙: 연결된 상품 태그는 삭제하지 않고 유지한다")
  void delete_shouldKeepCatalogProductTagsWhenProductIsArchived() {
    CatalogProduct product = persistProductWithAllFields();
    UUID tagLinkId = product.getTags().get(0).getId();
    clear();

    repository.deleteById(product.getId());
    em.flush();
    clear();

    CatalogProductTag tagLink = em.find(CatalogProductTag.class, tagLinkId);

    assertThat(tagLink).isNotNull();
  }

  @Test
  @DisplayName("상품 저장: 유효한 상품을 저장하면 한 번의 INSERT 쿼리로 저장된다")
  void save_shouldPersistValidProduct() {
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProduct product = CatalogProduct.builder()
        .category(em.getReference(Category.class, category.getId()))
        .name("Laptop")
        .description("Portable computer")
        .asin("B000123456")
        .build();

    CatalogProduct saved = repository.save(product);
    em.flush();

    assertThat(saved.getId()).isNotNull();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 저장: 태그와 함께 저장하면 상품 태그도 함께 저장된다")
  void save_shouldPersistProductWithTags() {
    Category category = persistAndFlush(Category.of("Computers", null));
    queryInspector.clear();

    CatalogProduct product = CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .asin("B000123456")
        .build();
    product.setTags(List.of(
        CatalogProductTag.of(product, officeTag),
        CatalogProductTag.of(product, saleTag),
        CatalogProductTag.of(product, laptopTag)));

    CatalogProduct saved = repository.save(product);
    em.flush();
    UUID productId = saved.getId();

    ensureQueryCount(2);
    flushAndClear();

    CatalogProduct found = repository.findWithDetailsById(productId).orElseThrow();

    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("tags")
        .ignoringCollectionOrder()
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(product);
    assertThat(found.getTags())
        .extracting(productTag -> productTag.getTag().getName())
        .containsExactlyInAnyOrder(officeTag.getName(), saleTag.getName(), laptopTag.getName());
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("상품 ID 조회: 저장된 상품의 필드와 연관관계를 조회한다")
  void findWithDetailsById_shouldReturnPersistedProduct() {
    CatalogProduct product = persistProduct("B000123456");
    UUID productId = product.getId();
    flushAndClear();

    CatalogProduct found = repository.findWithDetailsById(productId).orElseThrow();

    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("tags")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(product);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 상세 조회: 부모와 자식 카테고리가 있는 상품을 한 번의 조회로 가져온다")
  void findWithDetailsById_shouldFetchCategoryParentAndChildrenInOneQuery() {
    Category parent = persistAndFlush(Category.of("Computers", null));
    Category category = persistAndFlush(Category.of("Laptops", parent));
    persistAndFlush(Category.of("Gaming", category));
    persistAndFlush(Category.of("Ultrabooks", category));
    CatalogProduct product = persistAndFlush(CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .asin("B000123456")
        .build());
    UUID productId = product.getId();
    flushAndClear();

    CatalogProduct found = repository.findWithDetailsById(productId).orElseThrow();

    assertThat(found.getCategory().getParent().getName()).isEqualTo("Computers");
    assertThat(found.getCategory().getChildren())
        .extracting(Category::getName)
        .containsExactly("Gaming", "Ultrabooks");
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("상품 ID 조회: 모든 필드와 연관관계에 접근한 뒤 발생한 쿼리 수를 검증한다")
  void findWithDetailsById_shouldAccessAllFieldsAndAssociationsBeforeCountingQueries() {
    CatalogProduct product = persistProductWithAllFields();
    UUID productId = product.getId();
    flushAndClear();

    CatalogProduct found = repository.findWithDetailsById(productId).orElseThrow();
    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("tags")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(product);
    assertThat(found.getTags())
        .extracting(productTag -> productTag.getTag().getName())
        .containsExactly(officeTag.getName());

    ensureQueryCount(3);
  }

  @Test
  @DisplayName("공개 목록 조회: ACTIVE와 키워드 조건으로 상품을 필터링하고 1회의 쿼리로 페이지를 반환한다")
  void findPage_shouldFilterActiveProductsAndKeyword() {
    Category category = persistAndFlush(Category.of("Computers", null));
    CatalogProduct matching = persistAndFlush(product(category, "Office device", "Computer",
        "Brand", "B000123457"));
    persistAndFlush(ProductVariant.of(matching, "Wireless edition", Map.of()));
    CatalogProduct archived = product(category, "Archived device", "Computer", "Brand",
        "B000123458");
    archived.archive();
    persistAndFlush(archived);
    flushAndClear();

    PageResult<CatalogProduct> result = repository.findPage(
        Set.of(category.getId()), "wireless", null, CatalogProductSort.NAME_ASC,
        ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE, new PageQuery(0, 20));

    assertThat(result.content()).extracting(CatalogProduct::getName)
        .containsExactly("Office device");
    assertThat(result.totalElements()).isEqualTo(1);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("공개 목록 페이지: NAME_ASC 정렬로 연속 페이지를 조회하고 각 페이지에서 2회의 쿼리를 실행한다")
  void findPage_shouldReturnNonOverlappingSortedPages() {
    Category category = persistAndFlush(Category.of("Computers", null));
    persistAndFlush(product(category, "A device", "Computer", "Brand", "B000123461"));
    persistAndFlush(product(category, "B device", "Computer", "Brand", "B000123462"));
    persistAndFlush(product(category, "C device", "Computer", "Brand", "B000123463"));
    flushAndClear();

    PageResult<CatalogProduct> firstPage = repository.findPage(
        Set.of(category.getId()), null, null, CatalogProductSort.NAME_ASC,
        ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE, new PageQuery(0, 1));
    ensureQueryCount(2);

    clear();
    PageResult<CatalogProduct> secondPage = repository.findPage(
        Set.of(category.getId()), null, null, CatalogProductSort.NAME_ASC,
        ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE, new PageQuery(1, 1));

    assertThat(firstPage.content()).extracting(CatalogProduct::getName)
        .containsExactly("A device");
    assertThat(secondPage.content()).extracting(CatalogProduct::getName)
        .containsExactly("B device");
    assertThat(firstPage.content()).doesNotContainAnyElementsOf(secondPage.content());
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("상품 저장 제약조건: 외부 식별자가 없으면 저장을 거부한다")
  void save_shouldRejectProductWithoutIdentifier() {
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProduct product = CatalogProduct.builder()
        .category(em.getReference(Category.class, category.getId()))
        .name("Laptop")
        .description("Portable computer")
        .build();

    assertThatThrownBy(() -> repository.saveAndFlush(product))
        .isInstanceOf(DataIntegrityViolationException.class);
    ensureQueryCount(1);
  }

  @ParameterizedTest
  @MethodSource("invalidIdentifierFormats")
  @DisplayName("식별자 형식 제약조건: 잘못된 형식은 저장을 거부한다")
  void save_shouldRejectInvalidIdentifierFormat(String type, String value) {
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProduct.CatalogProductBuilder builder = CatalogProduct.builder()
        .category(em.getReference(Category.class, category.getId()))
        .name("Laptop")
        .description("Portable computer");

    switch (type) {
      case "asin" -> builder.asin(value);
      case "gtin" -> builder.gtin(value);
      case "upc" -> builder.upc(value);
      case "ean" -> builder.ean(value);
      case "isbn" -> builder.isbn(value);
      default -> throw new IllegalArgumentException("Unknown identifier type: " + type);
    }

    assertThatThrownBy(() -> repository.saveAndFlush(builder.build()))
        .isInstanceOf(DataIntegrityViolationException.class);
    ensureQueryCount(1);
  }

  private static Stream<Arguments> invalidIdentifierFormats() {
    return Stream.of(
        Arguments.of("asin", "B00012345"),
        Arguments.of("gtin", "123456789"),
        Arguments.of("upc", "12345678901"),
        Arguments.of("ean", "123456789"),
        Arguments.of("isbn", "978-030640615"));
  }

  private CatalogProduct persistProduct(String asin) {
    Category category = persistAndFlush(Category.of("Computers", null));
    return persistAndFlush(CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .brand("Brand")
        .asin(asin)
        .build());
  }

  private CatalogProduct product(Category category, String name, String description,
      String brand, String asin) {
    return CatalogProduct.builder()
        .category(category)
        .name(name)
        .description(description)
        .brand(brand)
        .asin(asin)
        .build();
  }

  private CatalogProduct persistProductWithAllFields() {
    Category parent = persistAndFlush(Category.of("Computers", null));
    Category category = persistAndFlush(Category.of("Laptops", parent));
    CatalogProduct product = CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .brand("Brand")
        .asin("B000123456")
        .gtin("4006381333931")
        .upc("036000291452")
        .ean("4006381333931")
        .isbn("9780306406157")
        .attributes(Map.of("screenSize", "15"))
        .build();
    product.setTags(List.of(CatalogProductTag.of(product, officeTag)));
    return persistAndFlush(product);
  }
}
