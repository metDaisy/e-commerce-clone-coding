package io.github.metdaisy.amaazon.catalog.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("카테고리 도메인")
class CategoryTest {

  @Test
  @DisplayName("카테고리 계층: 생성과 이동 시 부모-자식 관계와 이름을 갱신한다")
  void category_shouldMaintainHierarchyWhenCreatedAndMoved() {
    // given
    Category root = Category.of("Computers", null);
    Category child = Category.of("Laptops", root);
    Category anotherRoot = Category.of("Accessories", null);

    assertThat(root.getChildren()).containsExactly(child);
    assertThat(child.getDepth()).isEqualTo(2);

    // when
    child.moveTo(anotherRoot);
    child.rename("Chargers");
    child.updateDepth(2);

    // then
    assertThat(root.getChildren()).isEmpty();
    assertThat(anotherRoot.getChildren()).containsExactly(child);
    assertThat(child.getName()).isEqualTo("Chargers");
  }

  @Test
  @DisplayName("카테고리 자식 관리: 동일 자식은 중복 추가하지 않고 제거할 수 있다")
  void category_shouldNotDuplicateOrRetainRemovedChildren() {
    // given
    Category root = Category.of("Computers", null);
    Category child = Category.of("Laptops", root);

    // when
    root.addChild(child);
    root.removeChild(child);

    // then
    assertThat(root.getChildren()).isEmpty();
  }

  @Test
  @DisplayName("카테고리 계층: 자신의 하위 카테고리로 이동하면 순환을 거절한다")
  void moveTo_shouldRejectDescendantAsParent() {
    // given
    Category root = Category.of("Computers", null);
    Category child = Category.of("Laptops", root);

    // when & then
    assertThatThrownBy(() -> root.moveTo(child))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_CYCLE_DETECTED.getCode());
  }
}
