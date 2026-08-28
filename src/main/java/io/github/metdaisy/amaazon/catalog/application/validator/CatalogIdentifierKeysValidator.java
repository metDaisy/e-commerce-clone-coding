package io.github.metdaisy.amaazon.catalog.application.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CatalogIdentifierKeysValidator
    implements ConstraintValidator<ValidCatalogIdentifierKeys, Map<String, String>> {

  private static final Set<String> ALLOWED_KEYS = Set.of("asin", "gtin", "upc", "ean", "isbn");

  @Override
  public boolean isValid(Map<String, String> identifiers, ConstraintValidatorContext context) {
    if (identifiers == null || identifiers.isEmpty()) {
      return true;
    }
    List<String> invalidKeys = identifiers.keySet().stream()
        .filter(key -> key == null || !ALLOWED_KEYS.contains(key))
        .map(String::valueOf)
        .toList();
    if (invalidKeys.isEmpty()) {
      return true;
    }
    if (context != null) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "identifier key는 asin, gtin, upc, ean, isbn 중 하나의 소문자로 입력해야 합니다."
              + " 잘못된 key: " + String.join(", ", invalidKeys))
          .addConstraintViolation();
    }
    return false;
  }
}
