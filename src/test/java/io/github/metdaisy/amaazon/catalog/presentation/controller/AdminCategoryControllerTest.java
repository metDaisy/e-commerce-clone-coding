package io.github.metdaisy.amaazon.catalog.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryCommandService;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 카테고리 컨트롤러")
class AdminCategoryControllerTest extends RestControllerTest {

  private static final String CATEGORIES_URL = API_PREFIX + "/admin/categories";

  @MockitoBean
  private CategoryCommandService categoryCommandService;

  @Test
  @DisplayName("카테고리 생성: 요청 본문을 서비스에 전달하고 201 응답을 반환한다")
  void create_returnsCreatedCategory() throws Exception {
    // given
    UUID categoryId = UUID.randomUUID();
    CategoryCreateRequest request = new CategoryCreateRequest("노트북", null);
    CategoryResponse response = new CategoryResponse(
        categoryId, "노트북", null, 1, List.of());
    given(categoryCommandService.create(any(CategoryCreateRequest.class))).willReturn(response);

    // when & then
    mockMvc.perform(postJson(CATEGORIES_URL, request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.depth").value(1));

    ArgumentCaptor<CategoryCreateRequest> captor =
        ArgumentCaptor.forClass(CategoryCreateRequest.class);
    then(categoryCommandService).should().create(captor.capture());
    assertThat(captor.getValue()).isEqualTo(request);
  }

  @Test
  @DisplayName("카테고리 수정: 경로 ID와 요청 본문을 서비스에 전달하고 200 응답을 반환한다")
  void update_returnsUpdatedCategory() throws Exception {
    // given
    UUID categoryId = UUID.randomUUID();
    CategoryUpdateRequest request = new CategoryUpdateRequest("노트북·태블릿", UUID.randomUUID());
    CategoryResponse response = new CategoryResponse(
        categoryId, "노트북·태블릿", null, 1, List.of());
    given(categoryCommandService.update(any(UUID.class), any(CategoryUpdateRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(patch(CATEGORIES_URL + "/" + categoryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("노트북·태블릿"));

    ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<CategoryUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(CategoryUpdateRequest.class);
    then(categoryCommandService).should().update(idCaptor.capture(), requestCaptor.capture());
    assertThat(idCaptor.getValue()).isEqualTo(categoryId);
    assertThat(requestCaptor.getValue()).isEqualTo(request);
  }

  @Test
  @DisplayName("카테고리 생성 실패: 빈 이름 요청은 서비스 호출 없이 400으로 거절한다")
  void create_rejectsBlankName() throws Exception {
    // when & then
    mockMvc.perform(postJson(CATEGORIES_URL, new CategoryCreateRequest("", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.name").isArray());

    then(categoryCommandService).should(never()).create(any(CategoryCreateRequest.class));
  }

  @Test
  @DisplayName("카테고리 생성 실패: 255자를 초과한 이름은 서비스 호출 없이 400으로 거절한다")
  void create_rejectsNameLongerThan255Characters() throws Exception {
    // given
    CategoryCreateRequest request = new CategoryCreateRequest("x".repeat(256), null);

    // when & then
    mockMvc.perform(postJson(CATEGORIES_URL, request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.name").isArray());

    then(categoryCommandService).should(never()).create(any(CategoryCreateRequest.class));
  }

  @Test
  @DisplayName("카테고리 수정 실패: 빈 이름 요청은 서비스 호출 없이 400으로 거절한다")
  void update_rejectsBlankName() throws Exception {
    // given
    UUID categoryId = UUID.randomUUID();
    CategoryUpdateRequest request = new CategoryUpdateRequest(" ", null);

    // when & then
    mockMvc.perform(patch(CATEGORIES_URL + "/" + categoryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.details.name").isArray());

    then(categoryCommandService).should(never())
        .update(any(UUID.class), any(CategoryUpdateRequest.class));
  }

  @Test
  @DisplayName("카테고리 생성 실패: 서비스의 이름 중복 오류를 409와 오류 코드로 반환한다")
  void create_mapsDuplicateNameError() throws Exception {
    // given
    CategoryCreateRequest request = new CategoryCreateRequest("Computers", null);
    given(categoryCommandService.create(request)).willThrow(new CategoryException(
        CategoryErrorCode.CATEGORY_NAME_DUPLICATE,
        AmaazonExceptionContext.logDetails(Map.of("name", request.name()))));

    // when & then
    mockMvc.perform(postJson(CATEGORIES_URL, request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("CATEGORY-012"));
  }

}
