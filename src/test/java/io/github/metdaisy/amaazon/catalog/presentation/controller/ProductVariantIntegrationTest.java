package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.infra.repository.ProductVariantJpaRepository;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import io.github.metdaisy.amaazon.seller.domain.entity.Seller;
import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@DisplayName("상품 옵션 통합 테스트: 관리자·구매자 API와 영속 상태를 검증한다")
class ProductVariantIntegrationTest extends BaseIntegrationTest {

  private static final String ADMIN_URL = WebConstants.SERVLET_PREFIX + "/admin";
  private static final String PUBLIC_URL = WebConstants.SERVLET_PREFIX + "/product-variants";
  private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000021");
  private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000022");
  private static final UUID SELLER_ID = UUID.fromString("30000000-0000-0000-0000-000000000023");

  @Autowired
  private ProductVariantJpaRepository variantRepository;

  @Test
  @DisplayName("상품 옵션 생성 성공: 관리자는 활성 상품 아래에 옵션을 저장한다")
  void create_persistsVariantForActiveProduct() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    clear();
    ProductVariantCreateRequest request = ProductVariantFixture.createRequest();

    mockMvc.perform(post(ADMIN_URL + "/catalog-products/" + product.getId() + "/variants")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.displayName").value("Black / 256GB"))
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));

    flushAndClear();
    ProductVariant saved = variantRepository.findAll().get(0);
    assertThat(saved.getCatalogProduct().getId()).isEqualTo(product.getId());
    assertThat(saved.getAttributes()).containsEntry("storage", "256GB");
  }

  @Test
  @DisplayName("상품 옵션 공개 조회: 구매자는 활성 옵션의 공개 필드만 조회한다")
  void findForCatalogManager_returnsManagerFields() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();

    mockMvc.perform(get(PUBLIC_URL + "/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Black / 256GB"))
        .andExpect(jsonPath("$.attributes.color").value("BLACK"))
        .andExpect(jsonPath("$.id").value(variant.getId().toString()))
        .andExpect(jsonPath("$.catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));
  }

  @Test
  @DisplayName("상품 옵션 공개 조회: Seller도 활성 옵션의 공개 필드만 조회한다")
  void findForCatalogManager_returnsManagerFieldsForSeller() throws Exception {
    persistSeller();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();

    mockMvc.perform(get(PUBLIC_URL + "/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsSeller())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Black / 256GB"))
        .andExpect(jsonPath("$.attributes.storage").value("256GB"))
        .andExpect(jsonPath("$.id").value(variant.getId().toString()))
        .andExpect(jsonPath("$.catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));
  }

  @Test
  @DisplayName("상품 옵션 공개 조회 실패: 인증되지 않은 요청은 401을 반환한다")
  void findPublic_requiresAuthentication() throws Exception {
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();

    mockMvc.perform(get(PUBLIC_URL + "/" + variant.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("상품 옵션 수정: 관리자는 표시명과 attributes 병합 결과를 저장한다")
  void update_persistsMergedVariant() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();

    mockMvc.perform(patch(ADMIN_URL + "/product-variants/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ProductVariantFixture.updateRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(variant.getId().toString()))
        .andExpect(jsonPath("$.catalogProductId").value(product.getId().toString()))
        .andExpect(jsonPath("$.displayName").value("Black / 512GB"))
        .andExpect(jsonPath("$.attributes.storage").value("512GB"))
        .andExpect(jsonPath("$.attributes.color").doesNotExist())
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));

    flushAndClear();
    ProductVariant updated = variantRepository.findWithCatalogProductById(variant.getId())
        .orElseThrow();
    assertThat(updated.getDisplayName()).isEqualTo("Black / 512GB");
    assertThat(updated.getAttributes()).containsOnlyKeys("storage")
        .containsEntry("storage", "512GB");
  }

  @Test
  @DisplayName("상품 옵션 수정 실패: 보관된 옵션은 409와 CATALOG-033을 반환한다")
  void update_rejectsArchivedVariant() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    persistAndFlush(variant);
    clear();

    mockMvc.perform(patch(ADMIN_URL + "/product-variants/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ProductVariantFixture.updateRequest())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-033"));
  }

  @Test
  @DisplayName("상품 옵션 보관 실패: 이미 보관된 옵션은 409와 CATALOG-035를 반환한다")
  void archive_rejectsAlreadyArchivedVariant() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = ProductVariantFixture.variant(product);
    variant.archive();
    persistAndFlush(variant);
    clear();

    mockMvc.perform(post(ADMIN_URL + "/product-variants/" + variant.getId() + "/archive")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-035"));
  }

  @Test
  @DisplayName("상품 옵션 보관: 공개 조회에서는 404, 관리자 조회에서는 ARCHIVED를 반환한다")
  void archive_excludesVariantFromPublicLookupButKeepsAdminLookup() throws Exception {
    persistAdmin();
    persistSeller();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    ProductVariant variant = persistAndFlush(ProductVariantFixture.variant(product));
    clear();

    mockMvc.perform(post(ADMIN_URL + "/product-variants/" + variant.getId() + "/archive")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicationStatus").value("ARCHIVED"));
    em.flush();
    clear();

    mockMvc.perform(get(PUBLIC_URL + "/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsSeller())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-031"));

    mockMvc.perform(get(ADMIN_URL + "/product-variants/" + variant.getId())
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicationStatus").value("ARCHIVED"));
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 보관된 상품은 404와 CATALOG-019를 반환한다")
  void create_rejectsArchivedCatalogProduct() throws Exception {
    persistAdmin();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    product.archive();
    em.flush();
    clear();

    mockMvc.perform(post(ADMIN_URL + "/catalog-products/" + product.getId() + "/variants")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsAdmin()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ProductVariantFixture.createRequest())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-019"));
    assertThat(variantRepository.count()).isZero();
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 일반 사용자는 관리자 API를 호출할 수 없다")
  void create_rejectsNonAdminUser() throws Exception {
    persistUser();
    Category category = persistAndFlush(CategoryFixture.category());
    CatalogProduct product = persistAndFlush(CatalogProductFixture.persistedProduct(category));
    clear();

    mockMvc.perform(post(ADMIN_URL + "/catalog-products/" + product.getId() + "/variants")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAsUser()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ProductVariantFixture.createRequest())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionCode").value("ADMIN-001"));
    assertThat(variantRepository.count()).isZero();
  }

  private void persistAdmin() {
    User admin = User.createUser(ADMIN_ID, "vadmin", "01099990021");
    admin.updateRoles(EnumSet.of(UserRole.USER, UserRole.ADMIN));
    persistAndFlush(admin);
  }

  private void persistUser() {
    persistAndFlush(User.createUser(USER_ID, "vuser", "01099990022"));
  }

  private void persistSeller() {
    User seller = User.createUser(SELLER_ID, "vseller", "01099990023");
    seller.updateRoles(EnumSet.of(UserRole.USER, UserRole.PRODUCT_MANAGER));
    persistAndFlush(seller);
    persistAndFlush(new Seller(SELLER_ID, "seller", "seller business", "hash",
        "seller@example.com", "01012345678", SellerStatus.ACTIVE, null, Instant.now(), null));
  }

  private UsernamePasswordAuthenticationToken authenticationAsAdmin() {
    JwtPrincipal principal = new JwtPrincipal(ADMIN_ID, "ADMIN");
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private UsernamePasswordAuthenticationToken authenticationAsUser() {
    JwtPrincipal principal = new JwtPrincipal(USER_ID, "USER");
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private UsernamePasswordAuthenticationToken authenticationAsSeller() {
    JwtPrincipal principal = new JwtPrincipal(SELLER_ID, "PRODUCT_MANAGER");
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }
}
