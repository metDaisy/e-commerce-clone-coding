package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class DevIsbnExternalVerificationAdapter implements IsbnExternalVerificationPort {

  @Override
  public void verify(String isbn) {
    // dev/test에서는 ISBN 형식·체크디지트 검증만 수행한다.
  }
}
