package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.catalog.domain.verifier.IdentifierVerificationResult;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractIdentifierVerificationAdapter implements
    CatalogProductIdentifierVerifier {

  private final CatalogProductRepository repository;
  private final String type;

  @Override
  public final IdentifierVerificationResult verify(UUID id, String identifierValue) {
    String normalizedValue = normalize(identifierValue);
    if (!isValidFormat(normalizedValue)) {
      return IdentifierVerificationResult.failure(CatalogProductErrorCode.IDENTIFIER_INVALID);
    }
    IdentifierVerificationResult additionalValidation =
        afterFormatValidation(normalizedValue);
    if (!additionalValidation.valid()) {
      return additionalValidation;
    }
    if (repository.existsIdentifier(id, type, normalizedValue)) {
      return IdentifierVerificationResult.failure(
          CatalogProductErrorCode.IDENTIFIER_DUPLICATE,
          Map.of(type, identifierValue));
    }
    return IdentifierVerificationResult.success();
  }

  @Override
  public final boolean support(String type) {
    return this.type.equals(type);
  }

  protected abstract boolean isValidFormat(String identifierValue);

  protected String normalize(String identifierValue) {
    return identifierValue;
  }

  protected IdentifierVerificationResult afterFormatValidation(String identifierValue) {
    // 식별자별 추가 검증이 필요한 adapter에서 확장한다.
    return IdentifierVerificationResult.success();
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

}
