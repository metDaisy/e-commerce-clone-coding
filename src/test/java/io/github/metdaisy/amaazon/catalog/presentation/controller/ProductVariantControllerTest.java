package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.application.service.ProductVariantService;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.ProductVariantPresentationMapper;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ProductVariantController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("공개 상품 옵션 컨트롤러")
class ProductVariantControllerTest extends RestControllerTest {

  private static final String VARIANTS_URL = API_PREFIX + "/product-variants";

  @MockitoBean
  private ProductVariantService service;

  @MockitoBean
  private ProductVariantPresentationMapper presentationMapper;

  @Test
  @DisplayName("상품 옵션 조회: 공개 응답에서 내부 식별자와 상태를 제외한다")
  void find_shouldReturnPublicFieldsOnly() throws Exception {
    UUID variantId = UUID.randomUUID();
    ProductVariantAdminResponse response = new ProductVariantAdminResponse(variantId,
        UUID.randomUUID(), "Black / 256GB", Map.of("color", "BLACK"), "ACTIVE",
        null, null, null);
    ProductVariantDto dto = new ProductVariantDto(variantId, null, null, null,
        response.displayName(), response.attributes(), "ACTIVE", null);
    given(service.findForCatalogManager(variantId)).willReturn(dto);
    given(presentationMapper.toAdminResponse(dto)).willReturn(response);

    mockMvc.perform(get(VARIANTS_URL + "/" + variantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Black / 256GB"))
        .andExpect(jsonPath("$.attributes.color").value("BLACK"))
        .andExpect(jsonPath("$.id").value(variantId.toString()))
        .andExpect(jsonPath("$.catalogProductId").exists())
        .andExpect(jsonPath("$.publicationStatus").value("ACTIVE"));

    then(service).should().findForCatalogManager(variantId);
  }

  @Test
  @DisplayName("상품 옵션 조회 실패: 존재하지 않는 옵션은 404와 CATALOG-031을 반환한다")
  void find_shouldReturnNotFoundWhenVariantDoesNotExist() throws Exception {
    UUID variantId = UUID.randomUUID();
    given(service.findForCatalogManager(variantId)).willThrow(
        new ProductVariantException(ProductVariantErrorCode.VARIANT_NOT_FOUND));

    mockMvc.perform(get(VARIANTS_URL + "/" + variantId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-031"));
  }
}
