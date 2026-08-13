package io.github.metdaisy.amaazon.catalog.application.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("카테고리 조회 서비스")
class CategoryQueryServiceTest {

  @Mock
  private CategoryRepository repository;

  @Mock
  private CategoryMapper mapper;

  @InjectMocks
  private CategoryQueryService service;

  @Test
  @DisplayName("카테고리 목록 조회: 저장소 결과를 응답 DTO 목록으로 변환한다")
  void findAll_shouldReturnMappedCategories() {
    List<Category> categories = List.of(Category.of("Computers", null));
    List<CategoryResponse> response = List.of(
        new CategoryResponse(categories.get(0).getId(), "Computers", null, 1, List.of()));
    given(repository.findAll()).willReturn(categories);
    given(mapper.toDto(categories)).willReturn(response);

    assertThat(service.findAll()).isSameAs(response);
  }

  @Test
  @DisplayName("카테고리 프록시 조회: 존재하는 카테고리의 참조를 반환한다")
  void getProxy_shouldReturnReference_whenCategoryExists() {
    UUID categoryId = UUID.randomUUID();
    Category category = Category.of("Computers", null);
    given(repository.existsById(categoryId)).willReturn(true);
    given(repository.getReferenceById(categoryId)).willReturn(category);

    assertThat(service.getProxy(categoryId)).isSameAs(category);
  }

  @Test
  @DisplayName("카테고리 프록시 조회 실패: 없는 카테고리는 카테고리 없음 오류를 반환한다")
  void getProxy_shouldRejectUnknownCategory() {
    UUID categoryId = UUID.randomUUID();
    given(repository.existsById(categoryId)).willReturn(false);

    assertThatThrownBy(() -> service.getProxy(categoryId))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.CATEGORY_NOT_FOUND.getCode());
  }
}
