package io.github.metdaisy.amaazon.catalog.application.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapperImpl;
import io.github.metdaisy.amaazon.common.mapper.UtilMapperImpl;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("카테고리 조회 서비스")
class CategoryQueryServiceTest {

  @Mock
  private CategoryRepository repository;

  @Spy
  private CategoryMapper mapper = new CategoryMapperImpl(new UtilMapperImpl());

  @InjectMocks
  private CategoryQueryService service;

  @Test
  @DisplayName("카테고리 목록 조회: 저장소 결과를 응답 DTO 목록으로 변환한다")
  void findAll_shouldReturnMappedCategories() {
    // given
    List<Category> categories = List.of(Category.of("Computers", null));
    given(repository.findAll()).willReturn(categories);

    // when
    List<CategoryDto> result = service.findAll();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0))
        .usingRecursiveComparison()
        .isEqualTo(new CategoryDto(categories.get(0).getId(), null, null, null, "Computers", 1,
            List.of()));
  }

  @Test
  @DisplayName("카테고리 참조 조회: 저장소가 반환한 참조를 그대로 반환한다")
  void getProxy_shouldReturnRepositoryReference_whenCategoryExists() {
    // given
    UUID categoryId = UUID.randomUUID();
    Category category = Category.of("Computers", null);
    given(repository.existsById(categoryId)).willReturn(true);
    given(repository.getReferenceById(categoryId)).willReturn(category);

    // when
    Category result = service.getProxy(categoryId);

    // then
    assertThat(result).isSameAs(category);
  }

  @Test
  @DisplayName("카테고리 프록시 조회 실패: 없는 카테고리는 카테고리 없음 오류를 반환한다")
  void getProxy_shouldRejectUnknownCategory() {
    // given
    UUID categoryId = UUID.randomUUID();
    given(repository.existsById(categoryId)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> service.getProxy(categoryId))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code", CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());
  }
}
