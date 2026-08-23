package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier.isbn;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.List;
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
  public void verify(String isbn) {
    try {
      restClient.get()
          .uri("/isbn/{isbn}.json", isbn)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw verificationFailed(isbn, exception.getStatusCode());
    } catch (RestClientException exception) {
      throw verificationFailed(isbn, null);
    }
  }

  private CatalogProductException verificationFailed(String isbn, HttpStatusCode status) {
    Map<String, Object> clientDetails = Map.of(
        "fields", List.of(Map.of("field", "isbn", "reason", "external_verification_failed")));
    Map<String, Object> logDetails = status == null
        ? Map.of("isbn", isbn, "reason", "external_verification_failed")
        : Map.of("isbn", isbn, "status", status.value());
    return new CatalogProductException(CatalogProductErrorCode.ISBN_EXTERNAL_VERIFICATION_FAILED,
        new AmaazonExceptionContext(clientDetails, logDetails, null));
  }
}
