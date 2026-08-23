package io.github.metdaisy.amaazon.catalog.domain.verifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import java.util.UUID;

public interface CatalogProductIdentifierVerifier {

  boolean support(CatalogIdentifierType type);

  void verify(UUID id, String identifierValue);
}
