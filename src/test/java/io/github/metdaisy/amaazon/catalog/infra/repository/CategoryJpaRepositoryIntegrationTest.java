package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("카테고리 JPA 저장소 통합 테스트")
class CategoryJpaRepositoryIntegrationTest extends BaseRepositoryTest {

  @Autowired
  private CategoryJpaRepository repository;

  @Test
  @DisplayName("전체 조회: 부모와 자식을 엔터티 그래프로 한 번에 조회한다")
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
}
