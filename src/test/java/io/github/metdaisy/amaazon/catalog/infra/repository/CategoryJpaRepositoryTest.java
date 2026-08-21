package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("카테고리 JPA 저장소")
class CategoryJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CategoryJpaRepository repository;

  @Test
  @DisplayName("카테고리 저장: 유효한 Category를 저장하고 새 조회에서도 필드를 보존한다")
  void save_persistsCategory() {
    // given
    Category category = Category.of("Computers", null);

    // when
    Category saved = repository.save(category);
    flushAndClear();

    // then
    Optional<Category> persisted = repository.findById(category.getId());
    assertThat(saved.getId()).isEqualTo(category.getId());
    assertThat(persisted).isPresent();
    assertCategory(persisted.orElseThrow(), category.getId(), "Computers", null, 1);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 저장: 영속 상태인 부모와 자식 카테고리를 INSERT 1회로 저장한다")
  void save_persistsChildCategoryWithManagedParentInOneInsert() {
    // given
    Category parent = persistAndFlush(Category.of("Electronics", null));
    queryInspector.clear();

    // when
    Category child = repository.save(Category.of("Computers", parent));
    em.flush();

    // then
    assertThat(child.getParent()).isSameAs(parent);
    assertThat(child.getDepth()).isEqualTo(2);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 조회: 저장된 ID로 Category를 조회한다")
  void findById_returnsPersistedCategory() {
    // given
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    Category found = repository.findById(category.getId()).orElseThrow();

    // then
    assertCategory(found, category.getId(), "Computers", null, 1);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 참조 조회: 존재하는 ID는 초기화되지 않은 JPA 프록시를 반환한다")
  void getReferenceById_returnsUninitializedJpaProxy() {
    // given
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    Category reference = repository.getReferenceById(category.getId());

    // then
    assertThat(reference).isInstanceOf(HibernateProxy.class);
    assertThat(Hibernate.isInitialized(reference)).isFalse();
    assertThat(reference.getId()).isEqualTo(category.getId());
    ensureQueryCount(0);

    assertThat(reference.getName()).isEqualTo("Computers");
    assertThat(Hibernate.isInitialized(reference)).isTrue();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 조회: 존재하지 않는 ID는 빈 결과를 반환한다")
  void findById_returnsEmptyForUnknownCategory() {
    // given
    clear();

    // when
    Optional<Category> found = repository.findById(UUID.randomUUID());

    // then
    assertThat(found).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 삭제: 저장된 Category를 삭제하면 이후 조회되지 않는다")
  void delete_removesCategory() {
    // given
    Category category = persistAndFlush(Category.of("Computers", null));

    // when
    repository.delete(category);
    flushAndClear();

    // then
    assertThat(repository.findById(category.getId())).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 이름 존재 확인: 저장된 이름이면 true를 반환한다")
  void existsByName_returnsTrueWhenNameExists() {
    // given
    persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    boolean exists = repository.existsByName("Computers");

    // then
    assertThat(exists).isTrue();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 이름 존재 확인: 저장되지 않은 이름이면 false를 반환한다")
  void existsByName_returnsFalseWhenNameDoesNotExist() {
    // given
    persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    boolean exists = repository.existsByName("Accessories");

    // then
    assertThat(exists).isFalse();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 수정 중복 확인: 다른 Category가 같은 이름을 가지면 true를 반환한다")
  void existsByNameAndIdNot_returnsTrueForAnotherCategory() {
    // given
    Category existing = persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    boolean exists = repository.existsByNameAndIdNot("Computers", UUID.randomUUID());

    // then
    assertThat(exists).isTrue();
    assertThat(existing.getId()).isNotNull();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 수정 중복 확인: 자기 자신의 이름만 존재하면 false를 반환한다")
  void existsByNameAndIdNot_returnsFalseForSameCategory() {
    // given
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();

    // when
    boolean exists = repository.existsByNameAndIdNot("Computers", category.getId());

    // then
    assertThat(exists).isFalse();
    ensureQueryCount(1);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidCategories")
  @DisplayName("카테고리 저장 실패: 이름 제약을 위반한 Category는 저장하지 않는다")
  void save_rejectsInvalidCategory(String caseName, Category category) {
    // when & then
    assertThatThrownBy(() -> {
      repository.save(category);
      em.flush();
    }).isInstanceOf(ConstraintViolationException.class);
    ensureQueryCount(0);
  }

  private static Stream<Arguments> invalidCategories() {
    return Stream.of(
        Arguments.of("이름 null", Category.of(null, null)),
        Arguments.of("이름 길이 초과", Category.of("x".repeat(256), null)));
  }

  @Test
  @DisplayName("카테고리 저장 실패: 부모가 달라도 전역 이름 중복은 저장하지 않는다")
  void save_rejectsDuplicateNameAcrossParents() {
    // given
    Category firstParent = persistAndFlush(Category.of("Computers", null));
    Category secondParent = persistAndFlush(Category.of("Accessories", null));
    persistAndFlush(Category.of("Laptops", firstParent));
    Category duplicate = Category.of("Laptops", secondParent);

    // when & then
    assertThatThrownBy(() -> {
      repository.save(duplicate);
      em.flush();
    }).isInstanceOf(PersistenceException.class);
  }

  @Test
  @DisplayName("카테고리 전체 조회: 부모와 자식을 EntityGraph로 한 번의 조회에서 불러온다")
  void findAll_fetchesHierarchyInOneQuery() {
    // given
    Category parent = persistAndFlush(Category.of("전자기기", null));
    Category child = persistAndFlush(Category.of("노트북", parent));
    clear();

    // when
    List<Category> categories = repository.findAll();

    // then
    assertThat(categories).extracting(Category::getName)
        .containsExactlyInAnyOrder(parent.getName(), child.getName());
    Category loadedParent = categories.stream()
        .filter(category -> category.getId().equals(parent.getId()))
        .findFirst()
        .orElseThrow();
    assertThat(loadedParent.getChildren()).extracting(Category::getName)
        .containsExactly(child.getName());
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 전체 조회: 여러 단계의 모든 필드와 children을 재귀적으로 접근해도 한 번 조회한다")
  void findAll_fetchesAllFieldsOfMultipleLevelHierarchyInOneQuery() {
    // given
    Category categoryA = persistAndFlush(Category.of("A", null));
    Category categoryB = persistAndFlush(Category.of("B", categoryA));
    Category categoryC = persistAndFlush(Category.of("C", categoryA));
    persistAndFlush(Category.of("D", categoryA));
    persistAndFlush(Category.of("E", categoryA));
    persistAndFlush(Category.of("F", categoryB));
    persistAndFlush(Category.of("G", categoryB));
    persistAndFlush(Category.of("H", categoryC));
    persistAndFlush(Category.of("I", categoryC));
    persistAndFlush(Category.of("J", categoryC));
    clear();

    // when
    List<Category> categories = repository.findAll();
    Category loadedA = categories.stream()
        .filter(category -> category.getName().equals("A"))
        .findFirst()
        .orElseThrow();
    Set<UUID> visitedCategoryIds = new HashSet<>();
    readAllFieldsRecursively(loadedA, visitedCategoryIds);

    // then
    assertThat(visitedCategoryIds).hasSize(10);
    assertThat(categories).extracting(Category::getId)
        .containsAll(visitedCategoryIds);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 개별 조회: A부터 J까지 각각 조회하고 부모·자식 필드 접근 쿼리를 비교한다")
  void findById_fetchesEachCategoryIndividuallyWithExpectedQueryCount() {
    // given
    Category categoryA = persistAndFlush(Category.of("A", null));
    Category categoryB = persistAndFlush(Category.of("B", categoryA));
    Category categoryC = persistAndFlush(Category.of("C", categoryA));
    Category categoryD = persistAndFlush(Category.of("D", categoryA));
    Category categoryE = persistAndFlush(Category.of("E", categoryA));
    Category categoryF = persistAndFlush(Category.of("F", categoryB));
    Category categoryG = persistAndFlush(Category.of("G", categoryB));
    Category categoryH = persistAndFlush(Category.of("H", categoryC));
    Category categoryI = persistAndFlush(Category.of("I", categoryC));
    Category categoryJ = persistAndFlush(Category.of("J", categoryC));

    // when & then
    assertIndividualQueryCount(categoryA, 4);
    assertIndividualQueryCount(categoryB, 4);
    assertIndividualQueryCount(categoryC, 4);
    assertIndividualQueryCount(categoryD, 3);
    assertIndividualQueryCount(categoryE, 3);
    assertIndividualQueryCount(categoryF, 4);
    assertIndividualQueryCount(categoryG, 4);
    assertIndividualQueryCount(categoryH, 4);
    assertIndividualQueryCount(categoryI, 4);
    assertIndividualQueryCount(categoryJ, 4);
  }

  @Test
  @DisplayName("카테고리 조회: depth 1 Category의 부모를 재귀적으로 확인해도 한 번 조회한다")
  void findById_fetchesDepthOneCategoryInOneQuery() {
    // given
    Category root = persistAndFlush(Category.of("전자기기", null));
    clear();

    // when
    Category found = repository.findById(root.getId()).orElseThrow();

    // then
    assertThat(found.getDepth()).isEqualTo(1);
    assertThat(found.getName()).isEqualTo("전자기기");
    assertThat(found.getParent()).isNull();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("카테고리 조회: depth 2 Category의 부모를 루트까지 재귀적으로 조회한다")
  void findById_fetchesDepthTwoCategoryWithOneParentQuery() {
    // given
    Category root = persistAndFlush(Category.of("전자기기", null));
    Category level2 = persistAndFlush(Category.of("컴퓨터", root));
    clear();

    // when
    Category found = repository.findById(level2.getId()).orElseThrow();

    // then
    assertThat(found.getDepth()).isEqualTo(2);
    assertThat(found.getName()).isEqualTo("컴퓨터");
    Category parent = found.getParent();
    assertThat(parent).isNotNull();
    assertThat(parent.getDepth()).isEqualTo(1);
    assertThat(parent.getName()).isEqualTo("전자기기");
    assertThat(parent.getParent()).isNull();
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("카테고리 조회: depth 3 Category의 부모를 루트까지 재귀적으로 조회한다")
  void findById_fetchesDepthThreeCategoryWithTwoParentQueries() {
    // given
    Category root = persistAndFlush(Category.of("전자기기", null));
    Category level2 = persistAndFlush(Category.of("컴퓨터", root));
    Category level3 = persistAndFlush(Category.of("노트북", level2));
    clear();

    // when
    Category found = repository.findById(level3.getId()).orElseThrow();

    // then
    assertThat(found.getDepth()).isEqualTo(3);
    assertThat(found.getName()).isEqualTo("노트북");
    Category level2Parent = found.getParent();
    assertThat(level2Parent).isNotNull();
    assertThat(level2Parent.getDepth()).isEqualTo(2);
    assertThat(level2Parent.getName()).isEqualTo("컴퓨터");
    Category rootParent = level2Parent.getParent();
    assertThat(rootParent).isNotNull();
    assertThat(rootParent.getDepth()).isEqualTo(1);
    assertThat(rootParent.getName()).isEqualTo("전자기기");
    assertThat(rootParent.getParent()).isNull();
    ensureQueryCount(3);
  }

  @Test
  @DisplayName("카테고리 조회: depth 1 Category와 여러 자식을 조회하고 부모·자식 필드 접근 쿼리를 확인한다")
  void findById_fetchesDepthOneCategoryWithMultipleChildren() {
    // given
    Category category = persistCategoryWithChildren(1);
    clear();

    // when
    Category found = repository.findById(category.getId()).orElseThrow();
    assertCategoryWithChildren(found, 1, 3);

    // then
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("카테고리 조회: depth 2 Category와 여러 자식을 조회하고 부모·자식 필드 접근 쿼리를 확인한다")
  void findById_fetchesDepthTwoCategoryWithMultipleChildren() {
    // given
    Category category = persistCategoryWithChildren(2);
    clear();

    // when
    Category found = repository.findById(category.getId()).orElseThrow();
    assertCategoryWithChildren(found, 2, 3);

    // then
    ensureQueryCount(3);
  }

  @Test
  @DisplayName("카테고리 조회: 최대 depth 3 Category의 부모와 빈 자식 컬렉션 접근 쿼리를 확인한다")
  void findById_fetchesDepthThreeCategoryAsLeaf() {
    // given
    Category category = persistCategoryWithChildren(3);
    clear();

    // when
    Category found = repository.findById(category.getId()).orElseThrow();
    assertCategoryWithChildren(found, 3, 0);

    // then
    ensureQueryCount(4);
  }

  private Category persistCategoryWithChildren(int depth) {
    Category category = persistAndFlush(Category.of("Depth 1", null));
    for (int currentDepth = 2; currentDepth <= depth; currentDepth++) {
      category = persistAndFlush(Category.of("Depth " + currentDepth, category));
    }
    if (depth < 3) {
      for (int childNumber = 1; childNumber <= 3; childNumber++) {
        persistAndFlush(Category.of("Depth " + depth + " Child " + childNumber, category));
      }
    }
    return category;
  }

  private void assertCategoryWithChildren(
      Category category, int expectedDepth, int expectedChildrenCount) {
    assertThat(category.getId()).isNotNull();
    assertThat(category.getName()).isEqualTo("Depth " + expectedDepth);
    assertThat(category.getDepth()).isEqualTo(expectedDepth);

    Category current = category;
    for (int currentDepth = expectedDepth; currentDepth >= 1; currentDepth--) {
      assertThat(current.getDepth()).isEqualTo(currentDepth);
      assertThat(current.getName()).isEqualTo("Depth " + currentDepth);
      current = current.getParent();
    }
    assertThat(current).isNull();

    List<Category> children = category.getChildren();
    assertThat(children).hasSize(expectedChildrenCount);
    assertThat(children).allSatisfy(child -> {
      assertThat(child.getId()).isNotNull();
      assertThat(child.getName()).startsWith("Depth " + expectedDepth + " Child ");
      assertThat(child.getDepth()).isEqualTo(expectedDepth + 1);
      assertThat(child.getParent().getId()).isEqualTo(category.getId());
    });
  }

  private void readAllFieldsRecursively(Category category, Set<UUID> visitedCategoryIds) {
    assertThat(category.getId()).isNotNull();
    assertThat(category.getName()).isNotBlank();
    assertThat(category.getDepth()).isBetween(1, 3);
    assertThat(category.getCreatedAt()).isNotNull();
    assertThat(category.getUpdatedAt()).isNotNull();
    visitedCategoryIds.add(category.getId());

    Category parent = category.getParent();
    if (parent != null) {
      assertThat(parent.getId()).isNotNull();
      assertThat(parent.getName()).isNotBlank();
      assertThat(parent.getDepth()).isBetween(1, 3);
    }

    List<Category> children = category.getChildren();
    children.forEach(child -> readAllFieldsRecursively(child, visitedCategoryIds));
  }

  private void assertIndividualQueryCount(Category category, int expectedQueryCount) {
    clear();

    Category found = repository.findById(category.getId()).orElseThrow();
    readParentFieldsRecursively(found);
    readAllFieldsRecursively(found, new HashSet<>());

    int actualQueryCount = queryInspector.getCount();
    assertThat(actualQueryCount)
        .as("individual lookup for category %s", category.getName())
        .isEqualTo(expectedQueryCount);
  }

  private void readParentFieldsRecursively(Category category) {
    Category current = category.getParent();
    while (current != null) {
      assertThat(current.getId()).isNotNull();
      assertThat(current.getName()).isNotBlank();
      assertThat(current.getDepth()).isBetween(1, 3);
      current = current.getParent();
    }
  }

  private void assertCategory(Category category, UUID id, String name, Category parent, int depth) {
    assertThat(category)
        .extracting(Category::getId, Category::getName, Category::getParent, Category::getDepth)
        .containsExactly(id, name, parent, depth);
    assertThat(category.getCreatedAt()).isNotNull();
    assertThat(category.getUpdatedAt()).isNotNull();
  }

}
