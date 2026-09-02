package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CatalogIdentifierKeysValidator
    implements ConstraintValidator<ValidCatalogIdentifierKeys, Map<String, String>> {

  @Override
  public boolean isValid(Map<String, String> identifiers, ConstraintValidatorContext context) {
    if (identifiers == null || identifiers.isEmpty()) {
      return true;
    }
    Set<String> invalidKeys = new LinkedHashSet<>(identifiers.keySet());
    invalidKeys.removeAll(CatalogIdentifierType.types);
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
