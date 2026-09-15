package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.catalog.domain.verifier.IdentifierVerificationResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EAN 식별자 검증 adapter")
class EanVerificationAdapterTest {

  @Mock
  private CatalogProductRepository repository;

  private CatalogProductIdentifierVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new EanVerificationAdapter(repository);
  }

  @Test
  @DisplayName("EAN 타입만 지원한다")
  void verify_shouldSupportOnlyEan() {
    assertThat(verifier.support(CatalogIdentifierType.EAN)).isTrue();
    assertThat(verifier.support(CatalogIdentifierType.ASIN)).isFalse();
  }

  @Test
  @DisplayName("중복된 EAN을 거부한다")
  void verify_shouldRejectDuplicateEan() {
    UUID productId = UUID.randomUUID();
    given(repository.existsIdentifier(productId, CatalogIdentifierType.EAN,
        "4006381333931")).willReturn(true);

    IdentifierVerificationResult result = verifier.verify(productId, "4006381333931");

    assertThat(result.valid()).isFalse();
    assertThat(result.code()).isEqualTo(CatalogProductErrorCode.IDENTIFIER_DUPLICATE.getCode());

    then(repository).should().existsIdentifier(productId, CatalogIdentifierType.EAN,
        "4006381333931");
  }

  @Test
  @DisplayName("잘못된 형식의 EAN을 거부한다")
  void verify_shouldRejectInvalidEan() {
    IdentifierVerificationResult result = verifier.verify(null, "4006381333930");

    assertThat(result.valid()).isFalse();
    assertThat(result.code()).isEqualTo(CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());
  }
}
