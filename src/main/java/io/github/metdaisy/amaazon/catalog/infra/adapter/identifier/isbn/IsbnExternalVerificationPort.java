package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

public interface IsbnExternalVerificationPort {

  void verify(String isbn);
}
