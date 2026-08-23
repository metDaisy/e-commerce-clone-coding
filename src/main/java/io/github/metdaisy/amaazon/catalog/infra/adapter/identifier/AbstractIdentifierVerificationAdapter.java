package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractIdentifierVerificationAdapter implements
    CatalogProductIdentifierVerifier {

  private final CatalogProductRepository repository;
  private final CatalogIdentifierType type;

  @Override
  public final void verify(UUID id, String identifierValue) {
    validateFormat(identifierValue);
    afterFormatValidation(identifierValue);
    validateUniqueness(id, identifierValue);
  }

  @Override
  public final boolean support(CatalogIdentifierType type) {
    return this.type.equals(type);
  }

  protected abstract boolean isValidFormat(String identifierValue);

  protected void afterFormatValidation(String identifierValue) {
    // 식별자별 추가 검증이 필요한 adapter에서 확장한다.
  }

  protected boolean isValidNumericIdentifier(String value, int... lengths) {
    if (!value.matches("\\d+")) {
      return false;
    }
    for (int length : lengths) {
      if (value.length() == length) {
        return isValidModuloTenCheckDigit(value);
      }
    }
    return false;
  }

  private void validateFormat(String identifierValue) {
    if (!isValidFormat(identifierValue)) {
      throw createInvalidFormatException(identifierValue);
    }
  }

  private void validateUniqueness(UUID id, String identifierValue) {
    if (repository.existsIdentifier(id, type, identifierValue)) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          AmaazonExceptionContext.logDetails(Map.of(type.name(), identifierValue)));
    }
  }

  private boolean isValidModuloTenCheckDigit(String value) {
    int sum = 0;
    int lastIndex = value.length() - 1;
    for (int index = 0; index < lastIndex; index++) {
      int digit = value.charAt(index) - '0';
      sum += (lastIndex - index) % 2 == 0 ? digit : digit * 3;
    }
    int expected = (10 - sum % 10) % 10;
    return expected == value.charAt(lastIndex) - '0';
  }

  private CatalogProductException createInvalidFormatException(String value) {
    String field = type.name().toLowerCase();
    return new CatalogProductException(CatalogProductErrorCode.IDENTIFIER_INVALID,
        new AmaazonExceptionContext(
            Map.of("fields", List.of(
                Map.of("field", field, "reason", "invalid_format"))),
            Map.of(type.name(), value), null));
  }
}
