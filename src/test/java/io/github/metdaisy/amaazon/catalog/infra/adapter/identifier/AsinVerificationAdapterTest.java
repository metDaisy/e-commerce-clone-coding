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
@DisplayName("ASIN 식별자 검증 adapter")
class AsinVerificationAdapterTest {

  @Mock
  private CatalogProductRepository repository;

  private CatalogProductIdentifierVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new AsinVerificationAdapter(repository);
  }

  @Test
  @DisplayName("ASIN 타입만 지원한다")
  void verify_shouldSupportOnlyAsin() {
    assertThat(verifier.support(CatalogIdentifierType.ASIN)).isTrue();
    assertThat(verifier.support(CatalogIdentifierType.GTIN)).isFalse();
  }

  @Test
  @DisplayName("중복된 ASIN을 거부한다")
  void verify_shouldRejectDuplicateAsin() {
    UUID productId = UUID.randomUUID();
    given(repository.existsIdentifier(productId, CatalogIdentifierType.ASIN,
        "B000123456")).willReturn(true);

    IdentifierVerificationResult result = verifier.verify(productId, "B000123456");

    assertThat(result.valid()).isFalse();
    assertThat(result.code()).isEqualTo(CatalogProductErrorCode.IDENTIFIER_DUPLICATE.getCode());

    then(repository).should().existsIdentifier(productId, CatalogIdentifierType.ASIN,
        "B000123456");
  }

  @Test
  @DisplayName("잘못된 형식의 ASIN을 거부한다")
  void verify_shouldRejectInvalidAsin() {
    IdentifierVerificationResult result = verifier.verify(null, "invalid");

    assertThat(result.valid()).isFalse();
    assertThat(result.code()).isEqualTo(CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());
  }
}
