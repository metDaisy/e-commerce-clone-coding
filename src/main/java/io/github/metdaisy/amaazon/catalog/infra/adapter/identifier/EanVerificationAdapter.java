package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import org.springframework.stereotype.Component;

@Component
public class EanVerificationAdapter extends AbstractIdentifierVerificationAdapter {

  public EanVerificationAdapter(CatalogProductRepository repository) {
    super(repository, CatalogIdentifierType.EAN);
  }

  @Override
  protected boolean isValidFormat(String identifierValue) {
    return isValidNumericIdentifier(identifierValue, 8, 13);
  }
}
