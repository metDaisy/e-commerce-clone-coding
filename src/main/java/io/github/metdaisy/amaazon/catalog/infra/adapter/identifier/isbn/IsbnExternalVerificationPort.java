package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import io.github.metdaisy.amaazon.catalog.domain.verifier.IdentifierVerificationResult;

public interface IsbnExternalVerificationPort {

  IdentifierVerificationResult verify(String isbn);
}
