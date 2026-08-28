package io.github.metdaisy.amaazon.catalog.application.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog 식별자 key 검증기")
class CatalogIdentifierKeysValidatorTest {

  private final CatalogIdentifierKeysValidator validator = new CatalogIdentifierKeysValidator();

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder builder;

  @Test
  @DisplayName("허용된 식별자 key만 있으면 유효하다")
  void isValid_shouldAcceptSupportedIdentifierKeys() {
    assertThat(validator.isValid(Map.of(
        "asin", "B000123456",
        "gtin", "4006381333931",
        "upc", "036000291452",
        "ean", "4006381333931",
        "isbn", "9780306406157"), null)).isTrue();
  }

  @Test
  @DisplayName("허용되지 않은 식별자 key가 있으면 유효하지 않다")
  void isValid_shouldRejectUnsupportedIdentifierKey() {
    Map<String, String> identifiers = new LinkedHashMap<>();
    identifiers.put("sku", "SKU-1");
    identifiers.put("model", "MODEL-1");
    given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(builder);

    assertThat(validator.isValid(identifiers, context)).isFalse();

    then(context).should().buildConstraintViolationWithTemplate(
        "identifier key는 asin, gtin, upc, ean, isbn 중 하나의 소문자로 입력해야 합니다."
            + " 잘못된 key: sku, model");
  }

  @Test
  @DisplayName("식별자 key가 대문자이면 유효하지 않다")
  void isValid_shouldRejectUppercaseIdentifierKey() {
    assertThat(validator.isValid(Map.of("ASIN", "B000123456"), null)).isFalse();
  }
}
