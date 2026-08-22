package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.infra.repository.CategoryJpaRepository;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;

@DisplayName("카테고리 조회 컨트롤러 통합 테스트: HTTP 응답과 계층 구조를 검증한다")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CategoryControllerIntegrationTest extends BaseIntegrationTest {

  private static final String CATEGORIES_URL = WebConstants.SERVLET_PREFIX + "/categories";
  private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  @Autowired
  private CategoryJpaRepository categoryRepository;

  @Test
  @DisplayName("카테고리 전체 조회 성공: 저장된 부모·자식 계층을 HTTP 응답으로 반환한다")
  void findAll_returnsPersistedCategoryHierarchy() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "catreader", "01077778888"));
    Category parent = persistAndFlush(Category.of("Electronics", null));
    persistAndFlush(Category.of("Computers", parent));
    clear();
    // when
    mockMvc.perform(get(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(
            containsInAnyOrder("Electronics", "Computers")))
        .andExpect(jsonPath("$[?(@.name == 'Electronics')].children[0].name")
            .value("Computers"));
    // then
    assertThat(categoryRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("공개 카테고리 조회 성공: 깊이와 이름 순서에 따라 계층 응답을 반환한다")
  void findAll_returnsCategoriesInHierarchyOrder() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "catreader", "01077778888"));
    persistAndFlush(Category.of("Books", null));
    Category electronics = persistAndFlush(Category.of("Electronics", null));
    persistAndFlush(Category.of("Keyboards", electronics));
    persistAndFlush(Category.of("Laptops", electronics));
    clear();

    // when & then
    mockMvc.perform(get(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Books"))
        .andExpect(jsonPath("$[0].depth").value(1))
        .andExpect(jsonPath("$[1].name").value("Electronics"))
        .andExpect(jsonPath("$[1].children[0].name").value("Keyboards"))
        .andExpect(jsonPath("$[1].children[1].name").value("Laptops"))
        .andExpect(jsonPath("$[1].children[0].parentId")
            .value(electronics.getId().toString()));
  }

  @Test
  @DisplayName("공개 카테고리 조회 성공: 인증되지 않은 요청도 200을 반환한다")
  void findAll_allowsUnauthenticatedRequest() throws Exception {
    // given
    persistAndFlush(Category.of("Electronics", null));
    clear();

    // when & then
    mockMvc.perform(get(CATEGORIES_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Electronics"));
  }

  @Test
  @DisplayName("카테고리 전체 조회 성공: 저장된 카테고리가 없으면 HTTP 200과 빈 목록을 반환한다")
  void findAll_returnsEmptyListWhenNoCategoryExists() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "catreader", "01077778888"));
    clear();

    // when & then
    mockMvc.perform(get(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  private UsernamePasswordAuthenticationToken authenticationAs(UUID userId) {
    JwtPrincipal principal = new JwtPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
  }
}
