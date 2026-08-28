package io.github.metdaisy.amaazon.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.CatalogProductTagMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapper;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.mapper.TagMapperImpl;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.entity.ProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import io.github.metdaisy.amaazon.catalog.support.fixture.CatalogProductFixture;
import io.github.metdaisy.amaazon.catalog.support.fixture.CategoryFixture;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.common.mapper.UtilMapperImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogProduct 조회 서비스")
class CatalogProductQueryServiceTest {

  @Mock
  private CatalogProductRepository repository;

  @Mock
  private ProductVariantRepository variantRepository;

  @Mock
  private CategoryQueryService categoryQueryService;

  @Spy
  private CatalogProductMapper mapper = new CatalogProductMapperImpl(
      new CategoryMapperImpl(new UtilMapperImpl()),
      new CatalogProductTagMapperImpl(new TagMapperImpl()), new UtilMapperImpl());

  @Spy
  private ProductVariantMapper variantMapper = new ProductVariantMapperImpl(mapper,
      new UtilMapperImpl());

  @InjectMocks
  private CatalogProductQueryService service;

  @Test
  @DisplayName("Product Manager 조회: 요청 상태와 관계없이 ACTIVE CatalogProduct와 Variant만 조회한다")
  void findPage_productManagerAlwaysUsesActiveStatus() {
    CatalogProductPageRequest request = new CatalogProductPageRequest(
        0, 20, "office", null, "sale", ArchiveStatus.ARCHIVED, ArchiveStatus.ARCHIVED,
        CatalogProductSort.NAME_ASC.name());
    Category category = CategoryFixture.category();
    CatalogProduct product = CatalogProductFixture.product(category);
    ProductVariant variant = ProductVariant.of(product, "Black", Map.of("color", "BLACK"));
    PageResult<CatalogProduct> page = new PageResult<>(List.of(product), 0, 20, 1, 1);
    given(repository.findPage(Set.of(), "office", "sale", CatalogProductSort.NAME_ASC,
        ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE, new PageQuery(0, 20))).willReturn(page);
    given(variantRepository.findByCatalogProductIdsAndPublicationStatus(
        List.of(product.getId()), ArchiveStatus.ACTIVE)).willReturn(List.of(variant));

    PageResult<CatalogProductDto> result = service.findPage(request, false);

    assertThat(result.content()).singleElement()
        .extracting(CatalogProductDto::name)
        .isEqualTo(product.getName());
    assertThat(result.content().get(0).variants()).singleElement()
        .extracting(variantResponse -> variantResponse.displayName())
        .isEqualTo("Black");
    then(repository).should().findPage(Set.of(), "office", "sale", CatalogProductSort.NAME_ASC,
        ArchiveStatus.ACTIVE, ArchiveStatus.ACTIVE, new PageQuery(0, 20));
  }

  @Test
  @DisplayName("관리자 조회: 요청한 CatalogProduct와 Variant 상태 필터를 전달한다")
  void findPage_adminUsesRequestedStatuses() {
    CatalogProductPageRequest request = new CatalogProductPageRequest(
        0, 20, null, null, null, ArchiveStatus.ARCHIVED, ArchiveStatus.ARCHIVED,
        CatalogProductSort.LATEST.name());
    CatalogProduct product = CatalogProductFixture.product(CategoryFixture.category());
    PageResult<CatalogProduct> page = new PageResult<>(List.of(product), 0, 20, 1, 1);
    given(repository.findPage(Set.of(), null, null, CatalogProductSort.LATEST,
        ArchiveStatus.ARCHIVED, ArchiveStatus.ARCHIVED, new PageQuery(0, 20))).willReturn(page);
    given(variantRepository.findByCatalogProductIdsAndPublicationStatus(
        List.of(product.getId()), ArchiveStatus.ARCHIVED)).willReturn(List.of());

    service.findPage(request, true);

    then(repository).should().findPage(Set.of(), null, null, CatalogProductSort.LATEST,
        ArchiveStatus.ARCHIVED, ArchiveStatus.ARCHIVED, new PageQuery(0, 20));
    then(variantRepository).should().findByCatalogProductIdsAndPublicationStatus(
        List.of(product.getId()), ArchiveStatus.ARCHIVED);
  }
}
