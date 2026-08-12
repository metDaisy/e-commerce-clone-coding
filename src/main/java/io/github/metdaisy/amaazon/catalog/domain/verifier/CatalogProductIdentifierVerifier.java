package io.github.metdaisy.amaazon.catalog.domain.verifier;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import java.util.UUID;

public interface CatalogProductIdentifierVerifier {

  boolean support(CatalogProductIdentifierType type);

  void verify(UUID id, String identifierValue);
}
