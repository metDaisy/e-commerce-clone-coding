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
import io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn.IsbnExternalVerificationPort;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ISBN 식별자 검증 adapter")
class IsbnVerificationAdapterTest {

  @Mock
  private CatalogProductRepository repository;

  @Mock
  private IsbnExternalVerificationPort externalVerificationPort;

  private CatalogProductIdentifierVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new IsbnVerificationAdapter(repository, externalVerificationPort);
  }

  @Test
  @DisplayName("ISBN 타입만 지원한다")
  void verify_shouldSupportOnlyIsbn() {
    assertThat(verifier.support(CatalogIdentifierType.ISBN)).isTrue();
    assertThat(verifier.support(CatalogIdentifierType.ASIN)).isFalse();
  }

  @Test
  @DisplayName("중복된 ISBN을 거부한다")
  void verify_shouldRejectDuplicateIsbn() {
    UUID productId = UUID.randomUUID();
    given(repository.existsIdentifier(productId, CatalogIdentifierType.ISBN,
        "9780306406157")).willReturn(true);

    assertThatThrownBy(() -> verifier.verify(productId, "978-0-306-40615-7"))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.PRODUCT_CODE_ERROR.getCode());

    then(repository).should().existsIdentifier(productId, CatalogIdentifierType.ISBN,
        "9780306406157");
  }

  @ParameterizedTest(name = "[{index}] 유효한 ISBN={0}")
  @ValueSource(strings = {
      "9780306406157",
      "978-0-306-40615-7",
      "978 0 306 40615 7",
      "0306406152",
      "0-8044-2957-X",
      "080442957x"
  })
  @DisplayName("유효한 ISBN-10·ISBN-13과 구분자를 허용한다")
  void verify_shouldAcceptValidIsbnFormats(String value) {
    String normalized = value.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);

    assertThat(verifier.verify(null, value)).isEqualTo(normalized);
    then(externalVerificationPort).should().verify(normalized);
  }

  @ParameterizedTest(name = "[{index}] 잘못된 ISBN={0}")
  @ValueSource(strings = {
      "9780306406156",
      "978-0-306-40615-6",
      "0306406153",
      "123456789012",
      "030640615A"
  })
  @DisplayName("체크디지트·길이·문자 형식이 잘못된 ISBN을 거부한다")
  void verify_shouldRejectInvalidIsbnFormats(String value) {
    assertThatThrownBy(() -> verifier.verify(null, value))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode.IDENTIFIER_INVALID.getCode());

    then(externalVerificationPort).shouldHaveNoInteractions();
    then(repository).shouldHaveNoInteractions();
  }
}
