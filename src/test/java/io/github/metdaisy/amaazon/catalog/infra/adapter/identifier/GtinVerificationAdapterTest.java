package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GTIN 식별자 검증 adapter")
class GtinVerificationAdapterTest {

  @Mock
  private CatalogProductRepository repository;

  private CatalogProductIdentifierVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new GtinVerificationAdapter(repository);
  }

  @Test
  @DisplayName("GTIN 타입만 지원한다")
  void verify_shouldSupportOnlyGtin() {
    assertThat(verifier.support(CatalogIdentifierType.GTIN)).isTrue();
    assertThat(verifier.support(CatalogIdentifierType.ASIN)).isFalse();
  }

  @Test
  @DisplayName("중복된 GTIN을 거부한다")
  void verify_shouldRejectDuplicateGtin() {
    UUID productId = UUID.randomUUID();
    given(repository.existsIdentifier(productId, CatalogIdentifierType.GTIN,
        "4006381333931")).willReturn(true);

    assertThatThrownBy(() -> verifier.verify(productId, "4006381333931"))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode());

    then(repository).should().existsIdentifier(productId, CatalogIdentifierType.GTIN,
        "4006381333931");
  }

  @Test
  @DisplayName("잘못된 형식의 GTIN을 거부한다")
  void verify_shouldRejectInvalidGtin() {
    assertThatThrownBy(() -> verifier.verify(null, "4006381333930"))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());
  }
}
