package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.createRequest;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.updateRequest;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogIdentifierUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.service.CatalogProductService;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CatalogProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 카탈로그 상품 컨트롤러")
class CatalogProductControllerTest extends RestControllerTest {

  private static final String PRODUCTS_URL = API_PREFIX + "/admin/catalog-products";

  @MockitoBean
  private CatalogProductService service;

  @Test
  @DisplayName("상품 생성: 요청 본문을 서비스에 전달하고 201 응답을 반환한다")
  void create_shouldReturnCreatedProductAndDelegateRequest() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = createRequest(categoryId, Set.of("office"), null,
        Map.of(CatalogIdentifierType.GTIN, "4006381333931"));
    CatalogProductResponse response = CatalogProductResponse.builder()
        .id(UUID.randomUUID())
        .categoryId(categoryId)
        .name("Laptop")
        .tags(List.of("office"))
        .build();
    given(service.create(request)).willReturn(response);

    mockMvc.perform(postJson(PRODUCTS_URL, request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.catalogProductId").value(response.id().toString()))
        .andExpect(jsonPath("$.name").value("Laptop"));

    then(service).should().create(request);
  }

  @Test
  @DisplayName("상품 생성 실패: 필수 입력이 없으면 서비스 호출 없이 400으로 거절한다")
  void create_shouldRejectInvalidRequest() throws Exception {
    CatalogProductCreateRequest request = new CatalogProductCreateRequest(
        null, "", "", null, Set.of(), Map.of(), null);

    mockMvc.perform(postJson(PRODUCTS_URL, request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.categoryId").isArray())
        .andExpect(jsonPath("$.details.name").isArray())
        .andExpect(jsonPath("$.details.brand").isArray());

    then(service).should(never()).create(any(CatalogProductCreateRequest.class));
  }

  @Test
  @DisplayName("상품 생성 실패: 여러 identifier 오류를 모두 응답에 포함한다")
  void create_shouldReturnAllIdentifierFailures() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = createRequest(categoryId, Set.of(), null,
        Map.of(CatalogIdentifierType.ASIN, "invalid-asin",
            CatalogIdentifierType.GTIN, "invalid-gtin"));
    CatalogProductException exception = new CatalogProductException(
        CatalogProductErrorCode.IDENTIFIER_INVALID,
        new AmaazonExceptionContext(
            Map.of("fields", List.of(
                Map.of("field", "asin", "reason", "invalid_format"),
                Map.of("field", "gtin", "reason", "invalid_format"))),
            Map.of(), null));
    willThrow(exception).given(service).create(request);

    mockMvc.perform(postJson(PRODUCTS_URL, request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-014"))
        .andExpect(jsonPath("$.details.fields[0].field").value("asin"))
        .andExpect(jsonPath("$.details.fields[1].field").value("gtin"));
  }

  @Test
  @DisplayName("상품 수정: 경로 ID와 모든 요청 필드를 서비스에 전달하고 200 응답을 반환한다")
  void update_shouldReturnUpdatedProductAndDelegateAllFields() throws Exception {
    UUID productId = UUID.randomUUID();
    CatalogProductUpdateRequest request = updateRequest();
    CatalogProductResponse response = CatalogProductResponse.builder()
        .id(productId)
        .name("Updated laptop")
        .build();
    given(service.update(productId, request)).willReturn(response);

    mockMvc.perform(patch(PRODUCTS_URL + "/" + productId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.catalogProductId").value(productId.toString()));

    ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<CatalogProductUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(CatalogProductUpdateRequest.class);
    then(service).should().update(idCaptor.capture(), requestCaptor.capture());
    assertThat(idCaptor.getValue()).isEqualTo(productId);
    assertThat(requestCaptor.getValue()).isEqualTo(request);
  }

  @Test
  @DisplayName("상품 수정 실패: 빈 상품명은 서비스 호출 없이 400으로 거절한다")
  void update_shouldRejectBlankName() throws Exception {
    UUID productId = UUID.randomUUID();
    CatalogProductUpdateRequest request = new CatalogProductUpdateRequest(
        " ", null, null, null, null);

    mockMvc.perform(patch(PRODUCTS_URL + "/" + productId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.name").isArray());

    then(service).should(never()).update(any(UUID.class), any(CatalogProductUpdateRequest.class));
  }

  @Test
  @DisplayName("상품 식별자 수정: 식별자 요청을 서비스에 전달하고 200 응답을 반환한다")
  void updateIdentifier_shouldReturnUpdatedIdentifiers() throws Exception {
    UUID productId = UUID.randomUUID();
    Map<CatalogIdentifierType, String> identifiers =
        Map.of(CatalogIdentifierType.ASIN, "B000123456");
    CatalogIdentifierUpdateRequest request = new CatalogIdentifierUpdateRequest(identifiers);
    CatalogIdentifierUpdateResponse response =
        new CatalogIdentifierUpdateResponse(productId, "B000123456", null, null, null,
            null);
    given(service.updateIdentifier(productId, identifiers)).willReturn(response);

    mockMvc.perform(patch(PRODUCTS_URL + "/" + productId + "/identifiers")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asin").value("B000123456"));

    then(service).should().updateIdentifier(productId, identifiers);
  }

  @Test
  @DisplayName("상품 식별자 수정 실패: 식별자 값이 50자를 초과하면 서비스 호출 없이 400으로 거절한다")
  void updateIdentifier_shouldRejectValueLongerThan50Characters() throws Exception {
    UUID productId = UUID.randomUUID();
    Map<CatalogIdentifierType, String> identifiers =
        Map.of(CatalogIdentifierType.ASIN, "A".repeat(51));
    CatalogIdentifierUpdateRequest request = new CatalogIdentifierUpdateRequest(identifiers);

    mockMvc.perform(patch(PRODUCTS_URL + "/" + productId + "/identifiers")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details['identifiers[ASIN]']").isArray());

    then(service).should(never()).updateIdentifier(any(UUID.class), any());
  }

  @Test
  @DisplayName("상품 보관: 경로 ID를 서비스에 전달하고 보관된 상품 ID를 반환한다")
  void archive_shouldReturnArchivedProductId() throws Exception {
    UUID productId = UUID.randomUUID();
    CatalogArchivedResponse response = new CatalogArchivedResponse(productId,
        CatalogStatus.ARCHIVED, Instant.now(), Instant.now());
    given(service.archive(productId)).willReturn(response);

    mockMvc.perform(post(PRODUCTS_URL + "/" + productId + "/archive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.catalogProductId").value(productId.toString()))
        .andExpect(jsonPath("$.publicationStatus").value("ARCHIVED"));

    then(service).should().archive(productId);
  }
}
