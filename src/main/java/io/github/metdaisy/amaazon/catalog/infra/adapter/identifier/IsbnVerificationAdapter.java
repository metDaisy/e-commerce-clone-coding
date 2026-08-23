package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn.IsbnExternalVerificationPort;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class IsbnVerificationAdapter extends AbstractIdentifierVerificationAdapter {

  private final IsbnExternalVerificationPort externalVerificationPort;

  public IsbnVerificationAdapter(CatalogProductRepository repository,
      IsbnExternalVerificationPort externalVerificationPort) {
    super(repository, CatalogIdentifierType.ISBN);
    this.externalVerificationPort = externalVerificationPort;
  }

  @Override
  protected boolean isValidFormat(String identifierValue) {
    if (identifierValue.matches("\\d{13}")) {
      return isValidNumericIdentifier(identifierValue, 13);
    }
    if (!identifierValue.matches("\\d{9}[0-9X]")) {
      return false;
    }
    int sum = 0;
    for (int index = 0; index < 10; index++) {
      char digit = identifierValue.charAt(index);
      int value = digit == 'X' ? 10 : digit - '0';
      sum += value * (10 - index);
    }
    return sum % 11 == 0;
  }

  @Override
  protected String normalize(String identifierValue) {
    return identifierValue.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
  }

  @Override
  protected void afterFormatValidation(String identifierValue) {
    externalVerificationPort.verify(identifierValue);
  }
}
