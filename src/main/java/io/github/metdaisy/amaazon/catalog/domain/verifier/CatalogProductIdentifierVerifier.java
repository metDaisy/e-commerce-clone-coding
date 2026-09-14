package io.github.metdaisy.amaazon.catalog.domain.verifier;

import java.util.UUID;

public interface CatalogProductIdentifierVerifier {

  boolean support(String type);

  IdentifierVerificationResult verify(UUID id, String identifierValue);
}
