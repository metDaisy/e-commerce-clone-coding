package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 상품 조회 명세")
class CatalogProductSpecificationTest {

  @Mock
  private Root<CatalogProduct> root;

  @Mock
  private CriteriaQuery<?> query;

  @Mock
  private CriteriaBuilder criteriaBuilder;

  @Mock
  private Path<Object> path;

  @Mock
  private Predicate predicate;

  @Test
  @DisplayName("상품 ID 명세: ID 필드 일치 조건을 생성한다")
  void hasId_shouldBuildIdPredicate() {
    UUID id = UUID.randomUUID();
    given(root.get("id")).willReturn(path);
    given(criteriaBuilder.equal(path, id)).willReturn(predicate);

    Predicate result = CatalogProductSpecification.hasId(id)
        .toPredicate(root, query, criteriaBuilder);

    assertThat(result).isSameAs(predicate);
    then(root).should().get("id");
  }

  @ParameterizedTest
  @DisplayName("상품 식별자 명세: 식별자 유형별 필드 일치 조건을 생성한다")
  @MethodSource("identifierFields")
  void hasIdentifier_shouldUseIdentifierField(CatalogIdentifierType type, String field) {
    given(root.get(field)).willReturn(path);
    given(criteriaBuilder.equal(path, "value")).willReturn(predicate);

    Specification<CatalogProduct> specification =
        CatalogProductSpecification.hasIdentifier(type, "value");

    assertThat(specification.toPredicate(root, query, criteriaBuilder)).isSameAs(predicate);
    then(root).should().get(field);
  }

  private static Stream<Arguments> identifierFields() {
    return Stream.of(
        Arguments.of(CatalogIdentifierType.ASIN, "asin"),
        Arguments.of(CatalogIdentifierType.GTIN, "gtin"),
        Arguments.of(CatalogIdentifierType.UPC, "upc"),
        Arguments.of(CatalogIdentifierType.EAN, "ean"),
        Arguments.of(CatalogIdentifierType.ISBN, "isbn"));
  }
}
