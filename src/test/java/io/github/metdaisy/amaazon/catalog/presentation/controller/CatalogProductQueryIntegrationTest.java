package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import io.github.metdaisy.amaazon.seller.domain.entity.Seller;
import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@DisplayName("CatalogProduct·ProductVariant 관리자·판매자 조회 통합 테스트")
class CatalogProductQueryIntegrationTest extends BaseIntegrationTest {

  private static final String URL = WebConstants.SERVLET_PREFIX + "/catalog-products";
  private static final UUID ADMIN_ID = UUID.randomUUID();

  @Test
  @DisplayName("관리자 목록 조회: CatalogProduct와 연결된 Variant를 함께 반환한다")
  void findAll_returnsCatalogProductWithVariants() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    Tag tag = persistAndFlush(new Tag("office-" + UUID.randomUUID()));
    CatalogProduct product = product(category, "Office device", "Portable computer", "Brand A");
    product.setTags(List.of(CatalogProductTag.of(product, tag)));
    persistAndFlush(product);
    persistAndFlush(ProductVariant.of(product, "Wireless edition", Map.of("color", "BLACK")));
    clear();

    mockMvc.perform(get(URL).with(authentication("ADMIN", ADMIN_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.data[0].catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.data[0].tags[0]").value(tag.getName()))
        .andExpect(jsonPath("$.data[0].variants[0].variantId").exists())
        .andExpect(jsonPath("$.data[0].variants[0].displayName")
            .value("Wireless edition"));
  }

  @Test
  @DisplayName("목록 검색: 키워드와 태그 조건을 함께 적용한다")
  void findAll_filtersByKeywordAndTag() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    Tag matchingTag = persistAndFlush(new Tag("office-" + UUID.randomUUID()));
    Tag otherTag = persistAndFlush(new Tag("kitchen-" + UUID.randomUUID()));
    CatalogProduct matching = product(category, "Office device", "Portable computer", "Brand");
    matching.setTags(List.of(CatalogProductTag.of(matching, matchingTag)));
    persistAndFlush(matching);
    persistAndFlush(ProductVariant.of(matching, "Wireless edition", Map.of()));
    CatalogProduct other = product(category, "Kitchen device", "Home appliance", "Brand");
    other.setTags(List.of(CatalogProductTag.of(other, otherTag)));
    persistAndFlush(other);
    clear();

    mockMvc.perform(get(URL)
            .queryParam("keyword", "wireless")
            .queryParam("tag", matchingTag.getName())
            .with(authentication("ADMIN", ADMIN_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.data[0].name").value("Office device"));
  }

  @Test
  @DisplayName("판매자 목록 조회: 활성 Seller인 PRODUCT_MANAGER만 ACTIVE 데이터에 접근한다")
  void findAll_allowsActiveProductManager() throws Exception {
    UUID userId = UUID.randomUUID();
    persistProductManager(userId);
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(product(category, "Seller product", "Description",
        "Brand"));
    persistAndFlush(ProductVariant.of(product, "Variant", Map.of()));
    clear();

    mockMvc.perform(get(URL).with(authentication("PRODUCT_MANAGER", userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.data[0].name").value("Seller product"));
  }

  @Test
  @DisplayName("일반 사용자 목록 조회: PRODUCT_MANAGER 또는 ADMIN 권한이 없으면 거부한다")
  void findAll_rejectsRegularUser() throws Exception {
    mockMvc.perform(get(URL).with(authentication("USER", UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("페이지 조회: 이름 정렬과 페이지 크기를 적용한다")
  void findAll_appliesSortingAndPagination() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    persistAndFlush(product(category, "Z device", "Description", "Brand"));
    persistAndFlush(product(category, "A device", "Description", "Brand"));
    clear();

    mockMvc.perform(get(URL)
            .queryParam("sort", "NAME_ASC")
            .queryParam("page", "0")
            .queryParam("size", "1")
            .with(authentication("ADMIN", ADMIN_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.data[0].name").value("A device"));
  }

  @Test
  @DisplayName("상세 조회: CatalogProduct와 Variant를 함께 반환한다")
  void find_returnsCatalogProductWithVariants() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(product(category, "Office device", "Description",
        "Brand"));
    persistAndFlush(ProductVariant.of(product, "Black", Map.of("color", "BLACK")));
    clear();

    mockMvc.perform(get(URL + "/" + product.getId())
            .with(authentication("ADMIN", ADMIN_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.variants[0].displayName").value("Black"));
  }

  @Test
  @DisplayName("인증 없이 목록 조회: 인증되지 않은 요청은 거부한다")
  void findAll_requiresAuthentication() throws Exception {
    mockMvc.perform(get(URL))
        .andExpect(status().isUnauthorized());
  }

  private RequestPostProcessor authentication(String role, UUID userId) {
    JwtPrincipal principal = new JwtPrincipal(userId, role);
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
    return SecurityMockMvcRequestPostProcessors.authentication(authentication);
  }

  private CatalogProduct product(Category category, String name, String description,
      String brand) {
    return CatalogProduct.builder()
        .category(category)
        .name(name)
        .description(description)
        .brand(brand)
        .asin("B" + String.format("%09d",
            Integer.toUnsignedLong(name.hashCode()) % 1_000_000_000L))
        .build();
  }

  private void persistAdmin() {
    User admin = User.createUser(ADMIN_ID, "queryadmin", "01099990031");
    admin.updateRoles(EnumSet.of(UserRole.USER, UserRole.ADMIN));
    persistAndFlush(admin);
  }

  private void persistProductManager(UUID userId) {
    User manager = User.createUser(userId, "qseller", "01099990032");
    manager.updateRoles(EnumSet.of(UserRole.USER, UserRole.PRODUCT_MANAGER));
    persistAndFlush(manager);
    persistAndFlush(new Seller(userId, "seller", "seller business", "hash",
        "seller@example.com", "01012345678", SellerStatus.ACTIVE, null, Instant.now(), null));
  }
}
