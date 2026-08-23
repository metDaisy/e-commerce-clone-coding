package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.infra.repository.CategoryJpaRepository;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import io.github.metdaisy.amaazon.support.BaseCacheIntegrationTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@DisplayName("관리자 카테고리 컨트롤러 통합 테스트: HTTP 요청의 DB 반영을 검증한다")
class AdminCategoryControllerIntegrationTest extends BaseCacheIntegrationTest {

  private static final String CATEGORIES_URL = WebConstants.SERVLET_PREFIX + "/admin/categories";
  private static final String PUBLIC_CATEGORIES_URL = WebConstants.SERVLET_PREFIX + "/categories";
  private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

  @Autowired
  private CategoryJpaRepository categoryRepository;

  @Test
  @DisplayName("카테고리 생성 성공: 부모 ID가 포함된 POST 요청은 자식 카테고리를 저장하고 응답한다")
  void create_persistsChildCategory() throws Exception {
    // given
    persistAdmin();
    Category parent = persistAndFlush(Category.of("Electronics", null));
    clear();
    CategoryCreateRequest request = new CategoryCreateRequest("Computers", parent.getId());

    // when
    mockMvc.perform(post(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Computers"))
        .andExpect(jsonPath("$.parentId").value(parent.getId().toString()))
        .andExpect(jsonPath("$.depth").value(2));
    em.flush();
    mockMvc.perform(get(PUBLIC_CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(
            containsInAnyOrder("Electronics", "Computers")));

    // then
    flushAndClear();
    Category saved = categoryRepository.findById(parent.getId()).orElseThrow()
        .getChildren().stream()
        .filter(category -> category.getName().equals("Computers"))
        .findFirst()
        .orElseThrow();
    assertThat(saved.getDepth()).isEqualTo(2);
  }

  @Test
  @DisplayName("카테고리 생성 실패: 이미 존재하는 이름의 POST 요청은 409 응답을 반환한다")
  void createFailure_rejectsDuplicateName() throws Exception {
    // given
    persistAdmin();
    persistAndFlush(Category.of("Electronics", null));
    clear();

    // when
    mockMvc.perform(post(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new CategoryCreateRequest("Electronics", null))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("CATEGORY-004"));
    // then
    assertThat(categoryRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("카테고리 생성 검증 실패: 이름이 공백인 POST 요청은 400 응답을 반환하고 DB에 저장하지 않는다")
  void create_rejectsBlankNameAndDoesNotPersist() throws Exception {
    // given
    persistAdmin();
    clear();
    CategoryCreateRequest request = new CategoryCreateRequest(" ", null);

    // when & then
    mockMvc.perform(post(CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"));
    assertThat(categoryRepository.count()).isZero();
  }

  @Test
  @DisplayName("카테고리 수정 성공: 이름과 부모를 변경한 PATCH 요청은 DB와 계층 정보를 갱신한다")
  void update_movesCategoryAndReturnsResponse() throws Exception {
    // given
    persistAdmin();
    Category oldParent = persistAndFlush(Category.of("Electronics", null));
    Category category = persistAndFlush(Category.of("Computers", oldParent));
    Category newParent = persistAndFlush(Category.of("Accessories", null));
    clear();
    CategoryUpdateRequest request = new CategoryUpdateRequest("Laptops", newParent.getId());

    // when
    mockMvc.perform(patch(CATEGORIES_URL + "/" + category.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Laptops"))
        .andExpect(jsonPath("$.parentId").value(newParent.getId().toString()))
        .andExpect(jsonPath("$.depth").value(2));
    em.flush();
    mockMvc.perform(get(PUBLIC_CATEGORIES_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(
            containsInAnyOrder("Electronics", "Laptops", "Accessories")));

    // then
    flushAndClear();
    Category updated = categoryRepository.findById(category.getId()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("Laptops");
    assertThat(updated.getParent().getId()).isEqualTo(newParent.getId());
    assertThat(updated.getDepth()).isEqualTo(2);
  }

  private void persistAdmin() {
    User admin = User.createUser(ADMIN_ID, "catadmin", "01099990000");
    admin.updateRoles(EnumSet.of(UserRole.USER, UserRole.ADMIN));
    persistAndFlush(admin);
  }

  private UsernamePasswordAuthenticationToken authenticationAsAdmin() {
    JwtPrincipal principal = new JwtPrincipal(ADMIN_ID, "ADMIN");
    return new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
  }
}
