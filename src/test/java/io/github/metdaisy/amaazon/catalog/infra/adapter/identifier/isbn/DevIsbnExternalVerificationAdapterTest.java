package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("개발 환경 ISBN 외부 검증 adapter")
class DevIsbnExternalVerificationAdapterTest {

  private final IsbnExternalVerificationPort verificationPort =
      new DevIsbnExternalVerificationAdapter();

  @Test
  @DisplayName("개발 환경 ISBN 검증: 외부 API를 호출하지 않고 통과한다")
  void verify_skipsExternalApiCall() {
    assertThatCode(() -> verificationPort.verify("9780306406157"))
        .doesNotThrowAnyException();
  }
}
