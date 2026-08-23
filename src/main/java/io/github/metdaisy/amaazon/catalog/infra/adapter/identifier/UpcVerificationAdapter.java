package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import org.springframework.stereotype.Component;

@Component
public class UpcVerificationAdapter extends AbstractIdentifierVerificationAdapter {

  public UpcVerificationAdapter(CatalogProductRepository repository) {
    super(repository, CatalogIdentifierType.UPC);
  }

  @Override
  protected boolean isValidFormat(String identifierValue) {
    return isValidNumericIdentifier(identifierValue, 12);
  }
}
