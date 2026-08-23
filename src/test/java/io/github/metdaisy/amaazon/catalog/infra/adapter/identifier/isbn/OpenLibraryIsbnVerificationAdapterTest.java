package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest
@ActiveProfiles("prod")
@Import(OpenLibraryIsbnVerificationAdapter.class)
@TestPropertySource(properties = "catalog.isbn.verification-base-url=http://isbn.test")
@DisplayName("운영 ISBN 외부 검증 adapter")
class OpenLibraryIsbnVerificationAdapterTest {

  @Autowired
  private IsbnExternalVerificationPort verificationPort;

  @Autowired
  private MockRestServiceServer server;

  @Test
  @DisplayName("ISBN 외부 검증 성공: 도서 API가 200을 반환하면 통과한다")
  void verify_passesWhenExternalApiReturnsSuccess() {
    // given
    server.expect(requestTo("http://isbn.test/isbn/9780306406157.json"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    // when & then
    assertThatCode(() -> verificationPort.verify("9780306406157"))
        .doesNotThrowAnyException();
    server.verify();
  }

  @Test
  @DisplayName("ISBN 외부 검증 실패: 도서 API가 404를 반환하면 CATALOG-015를 반환한다")
  void verify_rejectsWhenExternalApiReturnsNotFound() {
    // given
    server.expect(requestTo("http://isbn.test/isbn/9780306406157.json"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    // when & then
    assertThatThrownBy(() -> verificationPort.verify("9780306406157"))
        .hasFieldOrPropertyWithValue("code", CatalogProductErrorCode
            .ISBN_EXTERNAL_VERIFICATION_FAILED.getCode());
    server.verify();
  }
}
