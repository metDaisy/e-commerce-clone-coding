package io.github.metdaisy.amaazon.catalog.application.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapperImpl;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
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
@DisplayName("카테고리 명령 서비스")
class CategoryCommandServiceTest {

  @Mock
  private CategoryRepository repository;

  @Spy
  private CategoryMapper mapper = new CategoryMapperImpl();

  @InjectMocks
  private CategoryCommandService service;

  @Test
  @DisplayName("카테고리 생성: 부모가 없으면 깊이 1의 루트 카테고리를 저장한다")
  void create_shouldCreateRootCategory() {
    // given
    CategoryCreateRequest request = new CategoryCreateRequest("Computers", null);
    Category saved = Category.of("Computers", null);
    given(repository.save(any(Category.class))).willReturn(saved);

    // when
    CategoryResponse result = service.create(request);

    // then
    assertThat(result.id()).isEqualTo(saved.getId());
    assertThat(result.name()).isEqualTo("Computers");
    assertThat(result.depth()).isEqualTo(1);

    then(repository).should().save(any(Category.class));
  }

  @Test
  @DisplayName("카테고리 생성: 부모가 있으면 자식 관계와 깊이를 설정한다")
  void create_shouldCreateChildCategory() {
    // given
    Category parent = Category.of("Computers", null);
    CategoryCreateRequest request = new CategoryCreateRequest("Ultrabooks", parent.getId());
    given(repository.findById(parent.getId())).willReturn(Optional.of(parent));
    given(repository.save(any(Category.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    CategoryResponse result = service.create(request);

    // then
    assertThat(result.id()).isNotNull();
    assertThat(result.name()).isEqualTo("Ultrabooks");
    assertThat(result.parentId()).isEqualTo(parent.getId());
    assertThat(result.depth()).isEqualTo(2);
    assertThat(parent.getChildren()).anyMatch(category -> category.getName().equals("Ultrabooks"));
  }

  @Test
  @DisplayName("카테고리 생성 실패: 존재하지 않는 부모는 카테고리 없음 오류를 반환한다")
  void create_shouldRejectMissingParent() {
    // given
    UUID parentId = UUID.randomUUID();
    given(repository.findById(parentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> service.create(new CategoryCreateRequest("Laptops", parentId)))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code", CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("카테고리 생성 실패: 부모와 상관없이 이름이 중복되면 거절한다")
  void create_shouldRejectDuplicateName() {
    // given
    Category parent = Category.of("Computers", null);
    UUID parentId = parent.getId();
    given(repository.findById(parentId)).willReturn(Optional.of(parent));
    given(repository.existsByName("Laptops")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> service.create(new CategoryCreateRequest("Laptops", parentId)))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_NAME_DUPLICATE.getCode());

    then(repository).should(never()).save(any(Category.class));
  }

  @Test
  @DisplayName("카테고리 생성 실패: 기존 루트 이름과 중복되면 거절한다")
  void create_shouldRejectDuplicateRootName() {
    // given
    given(repository.existsByName("Computers")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> service.create(new CategoryCreateRequest("Computers", null)))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_NAME_DUPLICATE.getCode());

    then(repository).should(never()).save(any(Category.class));
  }

  @Test
  @DisplayName("카테고리 생성 실패: 4단계 깊이는 허용하지 않는다")
  void create_shouldRejectFourthLevel() {
    // given
    Category root = Category.of("Root", null);
    Category level2 = Category.of("Level 2", root);
    Category level3 = Category.of("Level 3", level2);
    UUID parentId = level3.getId();
    given(repository.findById(parentId)).willReturn(Optional.of(level3));

    // when & then
    assertThatThrownBy(() -> service.create(new CategoryCreateRequest("Level 4", parentId)))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_DEPTH_EXCEEDED.getCode());
  }

  @Test
  @DisplayName("카테고리 수정: 새 부모로 이동하면 모든 하위 카테고리의 깊이를 갱신한다")
  void update_shouldMoveCategoryAndDescendants() {
    // given
    Category root = Category.of("Root", null);
    Category oldParent = Category.of("Old parent", root);
    Category category = Category.of("Category", oldParent);
    Category descendant = Category.of("Descendant", category);
    Category newParent = Category.of("New parent", null);
    given(repository.findById(category.getId())).willReturn(Optional.of(category));
    given(repository.findById(newParent.getId())).willReturn(Optional.of(newParent));
    given(repository.findAll()).willReturn(
        List.of(root, oldParent, category, descendant, newParent));

    // when
    CategoryResponse result = service.update(category.getId(),
        new CategoryUpdateRequest("Renamed", newParent.getId()));

    // then
    assertThat(result.id()).isEqualTo(category.getId());
    assertThat(result.name()).isEqualTo("Renamed");
    assertThat(result.parentId()).isEqualTo(newParent.getId());
    assertThat(result.depth()).isEqualTo(2);

    assertThat(category.getParent()).isSameAs(newParent);
    assertThat(category.getDepth()).isEqualTo(2);
    assertThat(descendant.getDepth()).isEqualTo(3);
    assertThat(oldParent.getChildren()).doesNotContain(category);
    assertThat(newParent.getChildren()).contains(category);
  }

  @Test
  @DisplayName("카테고리 수정: parentId가 null이면 루트 카테고리로 이동한다")
  void update_shouldMoveCategoryToRoot_whenParentIdIsNull() {
    // given
    Category oldParent = Category.of("Old parent", null);
    Category category = Category.of("Category", oldParent);
    given(repository.findById(category.getId())).willReturn(Optional.of(category));
    given(repository.findAll()).willReturn(List.of(oldParent, category));

    // when
    CategoryResponse result = service.update(category.getId(),
        new CategoryUpdateRequest("Renamed", null));

    // then
    assertThat(result.id()).isEqualTo(category.getId());
    assertThat(result.name()).isEqualTo("Renamed");
    assertThat(result.parentId()).isNull();
    assertThat(result.depth()).isEqualTo(1);

    assertThat(category.getParent()).isNull();
    assertThat(category.getDepth()).isEqualTo(1);
    assertThat(oldParent.getChildren()).doesNotContain(category);
    then(repository).should(never()).findById(null);
  }

  @Test
  @DisplayName("카테고리 수정 실패: 자기 하위 카테고리를 부모로 지정하면 순환을 거절한다")
  void update_shouldRejectCycle() {
    // given
    Category root = Category.of("Root", null);
    Category child = Category.of("Child", root);
    given(repository.findById(root.getId())).willReturn(Optional.of(root));
    given(repository.findById(child.getId())).willReturn(Optional.of(child));

    // when & then
    assertThatThrownBy(() -> service.update(root.getId(),
        new CategoryUpdateRequest("Root", child.getId())))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_CYCLE_DETECTED.getCode());
  }

  @Test
  @DisplayName("카테고리 수정 실패: 이동 후 깊이가 3단계를 넘으면 거절한다")
  void update_shouldRejectHierarchyThatExceedsDepth() {
    // given
    Category root = Category.of("Root", null);
    Category category = Category.of("Category", root);
    Category descendant = Category.of("Descendant", category);
    Category newParent = Category.of("New parent", root);
    given(repository.findById(category.getId())).willReturn(Optional.of(category));
    given(repository.findById(newParent.getId())).willReturn(Optional.of(newParent));
    given(repository.findAll()).willReturn(List.of(root, category, descendant, newParent));

    // when & then
    assertThatThrownBy(() -> service.update(category.getId(),
        new CategoryUpdateRequest("Category", newParent.getId())))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_DEPTH_EXCEEDED.getCode());
  }

  @Test
  @DisplayName("카테고리 수정 실패: 존재하지 않는 카테고리는 카테고리 없음 오류를 반환한다")
  void update_shouldRejectUnknownCategory() {
    // given
    UUID categoryId = UUID.randomUUID();
    given(repository.findById(categoryId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> service.update(categoryId,
        new CategoryUpdateRequest("Category", null)))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code", CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("카테고리 수정 실패: 부모가 달라도 다른 카테고리와 이름이 중복되면 거절한다")
  void update_shouldRejectDuplicateName() {
    // given
    Category parent = Category.of("Computers", null);
    Category category = Category.of("Laptops", parent);
    Category anotherParent = Category.of("Accessories", null);
    UUID categoryId = category.getId();
    given(repository.findById(categoryId)).willReturn(Optional.of(category));
    given(repository.findById(anotherParent.getId())).willReturn(Optional.of(anotherParent));
    given(repository.existsByNameAndIdNot("Desktops", categoryId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> service.update(categoryId,
        new CategoryUpdateRequest("Desktops", anotherParent.getId())))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code",
            CategoryErrorCode.CATEGORY_NAME_DUPLICATE.getCode());

    then(repository).should(never()).findAll();
  }
}
