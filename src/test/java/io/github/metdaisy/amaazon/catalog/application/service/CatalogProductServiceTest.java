package io.github.metdaisy.amaazon.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.createRequest;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.createRequestWithoutIdentifiers;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.product;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture.updateRequest;
import static io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture.category;
import static io.github.metdaisy.amaazon.catalog.support.fixture.TagFixture.tag;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogArchivedResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.TagMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.common.mapper.UtilMapperImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 상품 서비스")
class CatalogProductServiceTest {

  @Mock
  private CatalogProductRepository repository;

  @Mock
  private TagService tagService;

  @Mock
  private CategoryQueryService categoryQueryService;

  @Mock
  private CatalogProductIdentifierVerifier unsupportedVerifier;

  @Mock
  private CatalogProductIdentifierVerifier asinVerifier;

  @Mock
  private CatalogProductIdentifierVerifier gtinVerifier;

  @Spy
  private CatalogProductMapper mapper = new CatalogProductMapperImpl(
      new TagMapperImpl(), new UtilMapperImpl());

  @Spy
  private List<CatalogProductIdentifierVerifier> verifiers = new ArrayList<>();

  @InjectMocks
  private CatalogProductService service;

  @BeforeEach
  void setUpVerifiers() {
    verifiers.addAll(List.of(unsupportedVerifier, asinVerifier, gtinVerifier));
  }

  @Test
  @DisplayName("상품 생성: 카테고리와 태그를 연결해 저장하고 응답으로 변환한다")
  void create_shouldSaveProductWithTags_whenRequestIsValid() {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = createRequest(categoryId,
        Map.of(CatalogIdentifierType.ASIN, "B000123456"));
    Category category = category();
    Tag tag = tag();
    given(categoryQueryService.getProxy(categoryId)).willReturn(category);
    given(tagService.findAndCreate(request.tags())).willReturn(List.of(tag));

    CatalogProductResponse result = service.create(request);

    assertThat(result.name()).isEqualTo("Laptop");
    assertThat(result.tags()).containsExactly("office");
    then(repository).should().save(any(CatalogProduct.class));
  }

  @Test
  @DisplayName("상품 생성 실패: 외부 식별자가 없으면 저장하지 않고 식별자 오류를 반환한다")
  void create_shouldRejectMissingIdentifier() {
    CatalogProductCreateRequest request = createRequestWithoutIdentifiers(UUID.randomUUID());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());

    then(repository).shouldHaveNoInteractions();
    then(categoryQueryService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("상품 생성 실패: 존재하지 않는 카테고리는 상품을 저장하지 않고 카테고리 오류를 반환한다")
  void create_shouldRejectUnknownCategory() {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = createRequest(categoryId);
    given(categoryQueryService.getProxy(categoryId)).willThrow(new CategoryException(
        CategoryErrorCode.CATEGORY_NOT_FOUND));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CategoryException.class)
        .hasFieldOrPropertyWithValue("code", CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());

    then(repository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("상품 생성 실패: 중복 식별자는 상품을 저장하지 않고 충돌 오류를 반환한다")
  void create_shouldRejectDuplicateIdentifier() {
    UUID categoryId = UUID.randomUUID();
    CatalogProductCreateRequest request = createRequest(categoryId,
        Map.of(CatalogIdentifierType.ASIN, "B000123456"));
    given(unsupportedVerifier.support(CatalogIdentifierType.ASIN)).willReturn(false);
    given(asinVerifier.support(CatalogIdentifierType.ASIN)).willReturn(true);
    willThrow(new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR))
        .given(asinVerifier).verify(null, "B000123456");

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.IDENTIFIER_DUPLICATE.getCode());

    then(repository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("상품 생성 실패: 여러 식별자를 모두 검증하고 모든 실패 필드를 반환한다")
  void create_shouldCollectAllIdentifierFailures() {
    UUID categoryId = UUID.randomUUID();
    Map<CatalogIdentifierType, String> identifiers = new LinkedHashMap<>();
    identifiers.put(CatalogIdentifierType.ASIN, "invalid-asin");
    identifiers.put(CatalogIdentifierType.GTIN, "invalid-gtin");
    CatalogProductCreateRequest request = createRequest(categoryId, identifiers);
    given(asinVerifier.support(CatalogIdentifierType.ASIN)).willReturn(true);
    given(gtinVerifier.support(CatalogIdentifierType.GTIN)).willReturn(true);
    willThrow(invalidIdentifier("asin")).given(asinVerifier)
        .verify(null, "invalid-asin");
    willThrow(invalidIdentifier("gtin")).given(gtinVerifier)
        .verify(null, "invalid-gtin");

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOfSatisfying(CatalogProductException.class, exception -> {
          assertThat(exception.getCode())
              .isEqualTo(CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());
          assertThat(exception.getClientDetails().get("fields"))
              .isEqualTo(List.of(
                  Map.of("field", "asin", "reason", "invalid_format"),
                  Map.of("field", "gtin", "reason", "invalid_format")));
        });

    then(asinVerifier).should().verify(null, "invalid-asin");
    then(gtinVerifier).should().verify(null, "invalid-gtin");
  }

  @Test
  @DisplayName("상품 수정: 활성 상품의 변경 요청과 태그를 매퍼에 전달한다")
  void update_shouldUpdateActiveProduct() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(category());
    CatalogProductUpdateRequest request = updateRequest();
    Tag tag = tag();
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));
    given(tagService.findAndCreate(request.tags())).willReturn(List.of(tag));

    CatalogProductResponse response = service.update(productId, request);

    assertThat(response.name()).isEqualTo("Updated laptop");
    assertThat(product.getTags()).hasSize(1);
  }

  @Test
  @DisplayName("상품 수정: tags가 null이면 기존 태그를 유지하고 예외 없이 수정한다")
  void update_shouldHandleNullTags() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(category());
    CatalogProductUpdateRequest request = new CatalogProductUpdateRequest(
        "Updated laptop", null, null, null, null);
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));

    CatalogProductResponse response = service.update(productId, request);

    assertThat(response.name()).isEqualTo("Updated laptop");
    assertThat(product.getTags()).isEmpty();
    then(tagService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("식별자 수정: 지원하는 식별자 검증기를 호출하고 수정 결과를 반환한다")
  void updateIdentifier_shouldVerifySupportedIdentifierAndReturnResponse() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(category());
    Map<CatalogIdentifierType, String> request =
        Map.of(CatalogIdentifierType.ASIN, "B000123456");
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));
    given(unsupportedVerifier.support(CatalogIdentifierType.ASIN)).willReturn(false);
    given(asinVerifier.support(CatalogIdentifierType.ASIN)).willReturn(true);

    CatalogIdentifierUpdateResponse response = service.updateIdentifier(productId, request);

    then(asinVerifier).should().verify(productId, "B000123456");
    assertThat(response.asin()).isEqualTo("B000123456");
  }

  @Test
  @DisplayName("식별자 수정 실패: 중복 식별자는 상품 정보를 변경하지 않는다")
  void updateIdentifier_shouldRejectDuplicateIdentifier() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(category());
    Map<CatalogIdentifierType, String> request =
        Map.of(CatalogIdentifierType.ASIN, "B000123456");
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));
    given(unsupportedVerifier.support(CatalogIdentifierType.ASIN)).willReturn(false);
    given(asinVerifier.support(CatalogIdentifierType.ASIN)).willReturn(true);
    willThrow(new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
        AmaazonExceptionContext.logDetails(Map.of("ASIN", "B000123456"))))
        .given(asinVerifier)
        .verify(productId, "B000123456");

    assertThatThrownBy(() -> service.updateIdentifier(productId, request))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.IDENTIFIER_DUPLICATE.getCode());

    assertThat(product.getAsin()).isNull();
  }

  @Test
  @DisplayName("상품 수정 실패: 보관된 상품은 수정할 수 없다")
  void update_shouldRejectArchivedProduct() {
    UUID productId = UUID.randomUUID();
    CatalogProduct product = product(category());
    product.setPublicationStatus(CatalogStatus.ARCHIVED);
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));

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
    given(repository.findWithDetailsById(productId)).willReturn(Optional.of(product));

    CatalogArchivedResponse response = service.archive(productId);

    assertThat(response.id()).isEqualTo(product.getId());
    assertThat(response.publicationStatus()).isEqualTo(CatalogStatus.ARCHIVED);
    assertThat(response.archivedAt()).isNotNull();
    assertThat(response.updatedAt()).isEqualTo(response.archivedAt());
    then(repository).should(never()).delete(product);
  }

  @Test
  @DisplayName("상품 보관 실패: 존재하지 않는 상품은 카탈로그 없음 오류를 반환한다")
  void archive_shouldRejectUnknownProduct() {
    UUID productId = UUID.randomUUID();
    given(repository.findWithDetailsById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.archive(productId))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.CATALOG_NOT_FOUND.getCode());
  }

  private CatalogProductException invalidIdentifier(String field) {
    return new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID,
        new AmaazonExceptionContext(
            Map.of("fields", List.of(Map.of("field", field, "reason", "invalid_format"))),
            Map.of(field, "invalid"), null));
  }

}
