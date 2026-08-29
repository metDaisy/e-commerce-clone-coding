package io.github.metdaisy.amaazon.catalog.application.service.category;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.support.BaseCacheIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.SimpleKey;

@DisplayName("카테고리 서비스 캐시 통합 테스트: 실제 Spring 캐시 프록시 동작을 검증한다")
class CategoryCacheServiceIntegrationTest extends BaseCacheIntegrationTest {

  @Autowired
  private CategoryQueryService queryService;

  @Autowired
  private CategoryCommandService commandService;

  @Test
  @DisplayName("카테고리 전체 조회: 모든 필드에 접근한 뒤 첫 조회와 캐시 조회의 쿼리 수를 비교한다")
  void findAll_cachesResultAfterReadingAllFields() {
    // given
    assertThat(AopUtils.isAopProxy(queryService)).isTrue();
    Category root = persistAndFlush(Category.of("Electronics", null));
    Category child = persistAndFlush(Category.of("Computers", root));
    persistAndFlush(Category.of("Laptops", child));
    clear();

    // when
    List<CategoryDto> firstResult = queryService.findAll();
    int firstQueryCount = queryInspector.getCount();
    queryInspector.clear();
    List<CategoryDto> secondResult = queryService.findAll();
    int secondQueryCount = queryInspector.getCount();

    // then
    assertThat(firstQueryCount).isPositive();
    assertThat(secondQueryCount).isZero();
    assertThat(firstQueryCount).isGreaterThan(secondQueryCount);
    assertThat(secondResult).isEqualTo(firstResult);
    assertThat(categoriesCache().get(SimpleKey.EMPTY)).isNotNull();
  }

  @Test
  @DisplayName("카테고리 생성: 성공하면 전체 목록 캐시를 무효화한다")
  void create_evictsCategoryCache() {
    // given
    persistAndFlush(Category.of("Electronics", null));
    clear();
    queryService.findAll();
    assertThat(categoriesCache().get(SimpleKey.EMPTY)).isNotNull();

    // when
    commandService.create(new CategoryCreateRequest("Computers", null));

    // then
    assertThat(categoriesCache().get(SimpleKey.EMPTY)).isNull();
    queryInspector.clear();
    List<CategoryDto> refreshedResult = queryService.findAll();
    assertThat(refreshedResult).extracting(CategoryDto::name)
        .containsExactlyInAnyOrder("Electronics", "Computers");
    assertThat(queryInspector.getCount()).isPositive();
  }

  @Test
  @DisplayName("카테고리 수정: 성공하면 전체 목록 캐시를 무효화한다")
  void update_evictsCategoryCache() {
    // given
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    queryService.findAll();
    assertThat(categoriesCache().get(SimpleKey.EMPTY)).isNotNull();

    // when
    commandService.update(category.getId(), new CategoryUpdateRequest("Laptops", null));

    // then
    assertThat(categoriesCache().get(SimpleKey.EMPTY)).isNull();
    queryInspector.clear();
    List<CategoryDto> refreshedResult = queryService.findAll();
    assertThat(refreshedResult).extracting(CategoryDto::name)
        .containsExactly("Laptops");
    assertThat(queryInspector.getCount()).isPositive();
  }
}
