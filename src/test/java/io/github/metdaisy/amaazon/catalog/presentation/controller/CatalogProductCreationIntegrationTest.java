package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.createRequest;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.createRequestWithoutIdentifiers;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.infra.repository.CatalogProductJpaRepository;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@DisplayName("CatalogProduct 생성 통합 테스트: 관리자 API와 영속 상태를 검증한다")
class CatalogProductCreationIntegrationTest extends BaseIntegrationTest {

  private static final String PRODUCTS_URL =
      WebConstants.SERVLET_PREFIX + "/admin/catalog-products";
  private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000011");
  private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000012");

  @Autowired
  private CatalogProductJpaRepository productRepository;

  @Test
  @DisplayName("상품 생성 성공: 유효한 식별자와 Category를 가진 요청은 상품을 저장한다")
  void create_persistsCatalogProduct() throws Exception {
    // given
    persistAdmin();
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProductCreateRequest request = createRequest(category.getId(), Set.of(),
        Map.of("screenSize", "15"),
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));

    // when
    mockMvc.perform(post(PRODUCTS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.catalogProductId").exists())
        .andExpect(jsonPath("$.categoryId").value(category.getId().toString()))
        .andExpect(jsonPath("$.name").value("Laptop"))
        .andExpect(jsonPath("$.description").value("Portable computer"))
        .andExpect(jsonPath("$.brand").value("Brand"))
        .andExpect(jsonPath("$.gtin").value("4006381333931"))
        .andExpect(jsonPath("$.attributes.screenSize").value("15"))
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));

    // then
    flushAndClear();
    assertThat(productRepository.count()).isEqualTo(1);
    CatalogProduct saved = productRepository.findAll().get(0);
    assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
    assertThat(saved.getName()).isEqualTo("Laptop");
    assertThat(saved.getDescription()).isEqualTo("Portable computer");
    assertThat(saved.getBrand()).isEqualTo("Brand");
    assertThat(saved.getGtin()).isEqualTo("4006381333931");
    assertThat(saved.getAttributes()).containsEntry("screenSize", "15");
    assertThat(saved.getPublicationStatus()).isEqualTo(ArchiveStatus.ACTIVE);
  }

  @Test
  @DisplayName("상품 생성 실패: 외부 식별자가 없으면 400과 식별자 오류를 반환한다")
  void create_rejectsMissingIdentifier() throws Exception {
    // given
    persistAdmin();
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProductCreateRequest request = createRequestWithoutIdentifiers(category.getId());

    // when & then
    mockMvc.perform(post(PRODUCTS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-014"));
    assertThat(productRepository.count()).isZero();
  }

  @Test
  @DisplayName("상품 생성 실패: 존재하지 않는 Category는 404와 Category 오류를 반환한다")
  void create_rejectsUnknownCategory() throws Exception {
    // given
    persistAdmin();
    clear();
    CatalogProductCreateRequest request = createRequest(UUID.randomUUID(),
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));

    // when & then
    mockMvc.perform(post(PRODUCTS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("CATEGORY-003"));
    assertThat(productRepository.count()).isZero();
  }

  @Test
  @DisplayName("상품 생성 실패: 같은 식별자를 가진 상품은 409로 거절한다")
  void create_rejectsDuplicateIdentifier() throws Exception {
    // given
    persistAdmin();
    Category category = persistAndFlush(Category.of("Computers", null));
    persistAndFlush(CatalogProduct.builder()
        .category(category)
        .name("Existing laptop")
        .description("Existing product")
        .brand("Brand")
        .gtin("4006381333931")
        .build());
    clear();
    CatalogProductCreateRequest request = createRequest(category.getId(),
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));

    // when & then
    mockMvc.perform(post(PRODUCTS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-017"));
    assertThat(productRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("상품 생성 실패: 일반 사용자는 관리자 생성 API를 호출할 수 없다")
  void create_rejectsNonAdminUser() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "produser", "01011112222"));
    Category category = persistAndFlush(Category.of("Computers", null));
    clear();
    CatalogProductCreateRequest request = createRequest(category.getId(),
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));

    // when & then
    mockMvc.perform(post(PRODUCTS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsUser()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionCode").value("ADMIN-001"));
    assertThat(productRepository.count()).isZero();
  }

  private void persistAdmin() {
    User admin = User.createUser(ADMIN_ID, "prodadmin", "01099991111");
    admin.updateRoles(EnumSet.of(UserRole.USER, UserRole.ADMIN));
    persistAndFlush(admin);
  }

  private UsernamePasswordAuthenticationToken authenticationAsAdmin() {
    JwtPrincipal principal = new JwtPrincipal(ADMIN_ID, "ADMIN");
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private UsernamePasswordAuthenticationToken authenticationAsUser() {
    JwtPrincipal principal = new JwtPrincipal(USER_ID, "USER");
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }
}
