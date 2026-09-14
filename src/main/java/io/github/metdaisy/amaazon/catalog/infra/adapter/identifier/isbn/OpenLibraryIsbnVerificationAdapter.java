package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.verifier.IdentifierVerificationResult;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class OpenLibraryIsbnVerificationAdapter implements IsbnExternalVerificationPort {

  private final RestClient restClient;

  public OpenLibraryIsbnVerificationAdapter(RestClient.Builder builder,
      @Value("${catalog.isbn.verification-base-url:https://openlibrary.org}") String baseUrl) {
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  @Override
  public IdentifierVerificationResult verify(String isbn) {
    try {
      restClient.get()
          .uri("/isbn/{isbn}.json", isbn)
          .retrieve()
          .toBodilessEntity();
      return IdentifierVerificationResult.success();
    } catch (RestClientResponseException exception) {
      return verificationFailed(isbn, exception.getStatusCode());
    } catch (RestClientException exception) {
      return verificationFailed(isbn, null);
    }
  }

  private IdentifierVerificationResult verificationFailed(String isbn, HttpStatusCode status) {
    Map<String, Object> logDetails = status == null
        ? Map.of("isbn", isbn, "reason", "external_verification_failed")
        : Map.of("isbn", isbn, "status", status.value());
    return IdentifierVerificationResult.failure(
        CatalogProductErrorCode.ISBN_EXTERNAL_VERIFICATION_FAILED, logDetails);
  }
}
