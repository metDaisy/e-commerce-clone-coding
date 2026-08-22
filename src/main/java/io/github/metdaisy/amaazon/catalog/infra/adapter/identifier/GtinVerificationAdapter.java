package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import org.springframework.stereotype.Component;

@Component
public class GtinVerificationAdapter extends AbstractIdentifierVerificationAdapter {

  public GtinVerificationAdapter(CatalogProductRepository repository) {
    super(repository, CatalogProductIdentifierType.GTIN);
  }

  @Override
  protected boolean isValidFormat(String identifierValue) {
    return isValidNumericIdentifier(identifierValue, 8, 12, 13, 14);
  }
}
