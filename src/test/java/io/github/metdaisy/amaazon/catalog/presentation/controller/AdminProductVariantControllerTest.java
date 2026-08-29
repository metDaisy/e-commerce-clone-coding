package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.service.ProductVariantService;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.ProductVariantPresentationMapper;
import io.github.metdaisy.amaazon.catalog.support.fixture.ProductVariantFixture;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminProductVariantController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 상품 옵션 컨트롤러")
class AdminProductVariantControllerTest extends RestControllerTest {

  private static final String ADMIN_URL = API_PREFIX + "/admin";

  @MockitoBean
  private ProductVariantService service;

  @MockitoBean
  private ProductVariantPresentationMapper presentationMapper;

  @Test
  @DisplayName("상품 옵션 생성: 요청을 서비스에 전달하고 201 응답을 반환한다")
  void create_shouldDelegateRequestAndReturnCreated() throws Exception {
    UUID productId = UUID.randomUUID();
    ProductVariantCreateRequest request = ProductVariantFixture.createRequest();
    ProductVariantAdminResponse response = response(productId);
    ProductVariantDto dto = dto(response);
    given(service.create(productId, request)).willReturn(dto);
    given(presentationMapper.toAdminResponse(dto)).willReturn(response);

    mockMvc.perform(postJson(ADMIN_URL + "/catalog-products/" + productId + "/variants", request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.catalogProductId").value(productId.toString()))
        .andExpect(jsonPath("$.displayName").value("Black / 256GB"));

    then(service).should().create(productId, request);
  }

  @Test
  @DisplayName("상품 옵션 생성 실패: 공백 표시명은 서비스 호출 없이 400으로 거절한다")
  void create_shouldRejectBlankDisplayName() throws Exception {
    UUID productId = UUID.randomUUID();
    ProductVariantCreateRequest request = new ProductVariantCreateRequest(" ", Map.of());

    mockMvc.perform(postJson(ADMIN_URL + "/catalog-products/" + productId + "/variants", request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.displayName").isArray());

    then(service).should(never()).create(productId, request);
  }

  @Test
  @DisplayName("상품 옵션 수정: 경로 ID와 요청을 서비스에 전달하고 200 응답을 반환한다")
  void update_shouldDelegateRequestAndReturnOk() throws Exception {
    UUID variantId = UUID.randomUUID();
    ProductVariantUpdateRequest request = new ProductVariantUpdateRequest("Black / 512GB",
        Map.of("storage", "512GB"));
    ProductVariantAdminResponse response = response(UUID.randomUUID());
    ProductVariantDto dto = new ProductVariantDto(variantId, null, null, null,
        "Black / 256GB", Map.of(), "ARCHIVED", response.archivedAt());
    given(service.update(variantId, request)).willReturn(dto);
    given(presentationMapper.toAdminResponse(dto)).willReturn(response);

    mockMvc.perform(jsonBody(patch(ADMIN_URL + "/product-variants/" + variantId), request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id().toString()));

    then(service).should().update(variantId, request);
  }

  @Test
  @DisplayName("상품 옵션 보관: 보관 응답을 반환하고 서비스에 위임한다")
  void archive_shouldDelegateRequestAndReturnOk() throws Exception {
    UUID variantId = UUID.randomUUID();
    ProductVariantArchivedResponse response = new ProductVariantArchivedResponse(variantId,
        "ARCHIVED", Instant.now());
    ProductVariantDto dto = new ProductVariantDto(variantId, null, null, null,
        "Black / 256GB", Map.of(), "ARCHIVED", response.archivedAt());
    given(service.archive(variantId)).willReturn(dto);
    given(presentationMapper.toArchivedResponse(dto)).willReturn(response);

    mockMvc.perform(post(ADMIN_URL + "/product-variants/" + variantId + "/archive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productVariantId").value(variantId.toString()))
        .andExpect(jsonPath("$.publicationStatus").value("ARCHIVED"));

    then(service).should().archive(variantId);
  }

  private ProductVariantAdminResponse response(UUID productId) {
    return new ProductVariantAdminResponse(UUID.randomUUID(), productId, "Black / 256GB",
        Map.of("color", "BLACK"),
        "ACTIVE",
        null, null, null);
  }

  private ProductVariantDto dto(ProductVariantAdminResponse response) {
    return new ProductVariantDto(response.id(), null, null, null, response.displayName(),
        response.attributes(), response.publicationStatus(),
        response.archivedAt());
  }

  private MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder request,
      Object body) throws Exception {
    return request.contentType("application/json")
        .content(objectMapper.writeValueAsString(body));
  }
}
