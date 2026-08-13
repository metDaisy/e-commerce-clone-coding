package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 식별자 검증 어댑터")
class IdentifierVerificationAdapterTest {

  @Mock
  private CatalogProductRepository repository;

  @Test
  @DisplayName("식별자 검증: 각 어댑터는 자기 유형만 지원하고 중복 값은 거절한다")
  void adapters_shouldSupportTheirIdentifierAndRejectDuplicates() {
    List<AdapterCase> cases = List.of(
        new AdapterCase(CatalogProductIdentifierType.ASIN,
            new AsinVerificationAdapter(repository)),
        new AdapterCase(CatalogProductIdentifierType.GTIN,
            new GtinVerificationAdapter(repository)),
        new AdapterCase(CatalogProductIdentifierType.UPC,
            new UpcVerificationAdapter(repository)),
        new AdapterCase(CatalogProductIdentifierType.EAN,
            new EanVerificationAdapter(repository)),
        new AdapterCase(CatalogProductIdentifierType.ISBN,
            new IsbnVerificationAdapter(repository)));
    UUID productId = UUID.randomUUID();

    for (AdapterCase adapterCase : cases) {
      String value = "value-" + adapterCase.type();
      CatalogProductIdentifierVerifier verifier = adapterCase.verifier();
      assertThat(verifier.support(adapterCase.type())).isTrue();
      assertThat(verifier.support(otherType(adapterCase.type()))).isFalse();
      given(repository.existsIdentifier(productId, adapterCase.type(), value)).willReturn(false);
      verifier.verify(productId, value);
      given(repository.existsIdentifier(productId, adapterCase.type(), value)).willReturn(true);

      assertThatThrownBy(() -> verifier.verify(productId, value))
          .isInstanceOf(CatalogProductException.class)
          .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode());
    }
  }

  private CatalogProductIdentifierType otherType(CatalogProductIdentifierType type) {
    return type == CatalogProductIdentifierType.ASIN
        ? CatalogProductIdentifierType.GTIN
        : CatalogProductIdentifierType.ASIN;
  }

  private record AdapterCase(CatalogProductIdentifierType type,
                             CatalogProductIdentifierVerifier verifier) {
  }
}
