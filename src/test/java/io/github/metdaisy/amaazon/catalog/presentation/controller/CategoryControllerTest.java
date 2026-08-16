package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("공개 카테고리 컨트롤러")
class CategoryControllerTest extends RestControllerTest {

  private static final String CATEGORIES_URL = API_PREFIX + "/categories";

  @MockitoBean
  private CategoryQueryService service;

  @Test
  @DisplayName("카테고리 목록 조회: 서비스 결과를 200 응답으로 반환한다")
  void findAll_shouldReturnCategories() throws Exception {
    CategoryResponse response = new CategoryResponse(
        UUID.randomUUID(), "Computers", null, 1, List.of());
    given(service.findAll()).willReturn(List.of(response));

    mockMvc.perform(get(CATEGORIES_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Computers"));

    then(service).should().findAll();
  }

  @Test
  @DisplayName("카테고리 목록 조회 실패: 서비스의 카테고리 없음 오류를 404로 변환한다")
  void findAll_shouldReturnMappedCatalogError() throws Exception {
    UUID categoryId = UUID.randomUUID();
    given(service.findAll()).willThrow(new CatalogProductException(
        CatalogProductErrorCode.CATEGORY_NOT_FOUND,
        AmaazonExceptionContext.logDetails(Map.of("categoryId", categoryId))));

    mockMvc.perform(get(CATEGORIES_URL))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("CATALOG-001"));

  }
}
