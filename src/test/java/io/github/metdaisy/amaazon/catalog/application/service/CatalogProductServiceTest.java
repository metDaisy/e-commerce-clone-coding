package io.github.metdaisy.amaazon.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductIdentifierUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ProductPublicationStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 상품 서비스")
class CatalogProductServiceTest {

  @Mock
  private CatalogProductRepository repository;

  @Mock
  private CatalogProductMapper mapper;

  @Mock
  private TagService tagService;

  @Mock
  private CategoryQueryService categoryQueryService;

  @Mock
  private CatalogProductIdentifierVerifier unsupportedVerifier;

  @Mock
  private CatalogProductIdentifierVerifier asinVerifier;

  private CatalogProductService service;

  @BeforeEach
  void setUp() {
    service = new CatalogProductService(repository, mapper, tagService, categoryQueryService,
        List.of(unsupportedVerifier, asinVerifier));
  }

  @Test
  @DisplayName("상품 생성: 카테고리와 태그를 연결해 저장하고 응답으로 변환한다")
  void create_shouldSaveProductWithTags_whenRequestIsValid() {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = new CatalogProductCreateRequest(
        categoryId, "Laptop", "Portable computer", "Brand", Set.of("office"), Map.of());
    Category category = Category.of("Computers", null);
    Tag tag = new Tag("office");
    CatalogProduct product = product(category);
    CatalogProductResponse response = CatalogProductResponse.builder()
        .name("Laptop")
        .build();
    given(categoryQueryService.getProxy(categoryId)).willReturn(category);
    given(mapper.toEntity(category, request)).willReturn(product);
    given(tagService.findAndCreate(request.tags())).willReturn(List.of(tag));
    given(mapper.toDto(product)).willReturn(response);

    CatalogProductResponse result = service.create(request);

    assertThat(result).isSameAs(response);
    then(repository).should().save(product);
    then(mapper).should().toDto(product);
    assertThat(product.getTags()).hasSize(1);
  }

  @Test
  @DisplayName("상품 수정: 활성 상품의 변경 요청과 태그를 매퍼에 전달한다")
  void update_shouldUpdateActiveProduct() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    CatalogProductUpdateRequest request = new CatalogProductUpdateRequest(
        "Updated laptop", "Updated description", "New brand", List.of("office"), Map.of());
    Tag tag = new Tag("office");
    CatalogProductResponse response = CatalogProductResponse.builder().name("Updated laptop")
        .build();
    given(repository.findById(productId)).willReturn(Optional.of(product));
    given(tagService.findAndCreate(request.tags())).willReturn(List.of(tag));
    given(mapper.toDto(product)).willReturn(response);

    assertThat(service.update(productId, request)).isSameAs(response);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CatalogProductTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
    then(mapper).should().update(eq(product), tagsCaptor.capture(), eq(request));
    assertThat(tagsCaptor.getValue()).hasSize(1);
    then(mapper).should().toDto(product);
  }

  @Test
  @DisplayName("상품 수정: tags가 null이면 기존 태그를 유지하고 예외 없이 수정한다")
  void update_shouldHandleNullTags() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    CatalogProductUpdateRequest request = new CatalogProductUpdateRequest(
        "Updated laptop", null, null, null, null);
    CatalogProductResponse response = CatalogProductResponse.builder().name("Updated laptop")
        .build();
    given(repository.findById(productId)).willReturn(Optional.of(product));
    given(mapper.toDto(product)).willReturn(response);

    assertThat(service.update(productId, request)).isSameAs(response);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CatalogProductTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
    then(mapper).should().update(eq(product), tagsCaptor.capture(), eq(request));
    assertThat(tagsCaptor.getValue()).isNull();
    then(tagService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("식별자 수정: 지원하는 식별자 검증기를 호출하고 수정 결과를 반환한다")
  void updateIdentifier_shouldVerifySupportedIdentifierAndReturnResponse() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    CatalogProductIdentifierUpdateRequest request =
        new CatalogProductIdentifierUpdateRequest("B000123456", null, null, null, null);
    CatalogProductIdentifierUpdateResponse response =
        new CatalogProductIdentifierUpdateResponse(productId, "B000123456", null, null, null,
            null);
    given(repository.findById(productId)).willReturn(Optional.of(product));
    given(unsupportedVerifier.support(CatalogProductIdentifierType.ASIN)).willReturn(false);
    given(asinVerifier.support(CatalogProductIdentifierType.ASIN)).willReturn(true);
    given(mapper.toIdentifierResponse(product)).willReturn(response);

    assertThat(service.updateIdentifier(productId, request)).isSameAs(response);

    then(asinVerifier).should().verify(productId, "B000123456");
    then(mapper).should().update(product, request);
  }

  @Test
  @DisplayName("식별자 수정 실패: 중복 식별자는 상품 정보를 변경하지 않는다")
  void updateIdentifier_shouldRejectDuplicateIdentifier() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    CatalogProductIdentifierUpdateRequest request =
        new CatalogProductIdentifierUpdateRequest("B000123456", null, null, null, null);
    given(repository.findById(productId)).willReturn(Optional.of(product));
    given(unsupportedVerifier.support(CatalogProductIdentifierType.ASIN)).willReturn(false);
    given(asinVerifier.support(CatalogProductIdentifierType.ASIN)).willReturn(true);
    willThrow(new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
        Map.of("ASIN", "B000123456")))
        .given(asinVerifier)
        .verify(productId, "B000123456");

    assertThatThrownBy(() -> service.updateIdentifier(productId, request))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode());

    then(mapper).should(never()).update(product, request);
  }

  @Test
  @DisplayName("상품 수정 실패: 보관된 상품은 수정할 수 없다")
  void update_shouldRejectArchivedProduct() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    product.setPublicationStatus(ProductPublicationStatus.ARCHIVED);
    given(repository.findById(productId)).willReturn(Optional.of(product));

    assertThatThrownBy(() -> service.update(productId,
        new CatalogProductUpdateRequest("name", null, null, null, null)))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code",
            CatalogProductErrorCode.CATALOG_PRODUCT_ARCHIVED.getCode());
  }

  @Test
  @DisplayName("상품 보관: 영속 상태와 동일한 보관 정보를 반환한다")
  void archive_shouldReturnPersistedArchiveState() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(Category.of("Computers", null));
    given(repository.findById(productId)).willReturn(Optional.of(product));

    CatalogProductArchivedResponse response = service.archive(productId);

    assertThat(response.id()).isEqualTo(product.getId());
    assertThat(response.publicationStatus()).isEqualTo(ProductPublicationStatus.ARCHIVED);
    assertThat(response.archivedAt()).isNotNull();
    assertThat(response.updatedAt()).isEqualTo(response.archivedAt());
    then(repository).should(never()).delete(product);
  }

  @Test
  @DisplayName("상품 보관 실패: 존재하지 않는 상품은 카탈로그 없음 오류를 반환한다")
  void archive_shouldRejectUnknownProduct() {
    UUID productId = UUID.randomUUID();
    given(repository.findById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.archive(productId))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.CATALOG_NOT_FOUND.getCode());
  }

  private CatalogProduct product(Category category) {
    return CatalogProduct.builder()
        .category(category)
        .name("Laptop")
        .description("Portable computer")
        .brand("Brand")
        .build();
  }
}
