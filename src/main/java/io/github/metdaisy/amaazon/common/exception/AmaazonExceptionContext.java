package io.github.metdaisy.amaazon.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Separates client-visible details from server-side logging details.
 *
 * @param clientDetails safe details that may be returned to the client
 * @param logDetails internal details for server logs
 * @param systemMessage concrete internal cause for server logs
 */
public record AmaazonExceptionContext(
    Map<String, Object> clientDetails,
    Map<String, Object> logDetails,
    String systemMessage
) {

  public AmaazonExceptionContext {
    clientDetails = immutableOrEmpty(clientDetails);
    logDetails = immutableOrEmpty(logDetails);
  }

  public static AmaazonExceptionContext empty() {
    return new AmaazonExceptionContext(Collections.emptyMap(), Collections.emptyMap(), null);
  }

  public static AmaazonExceptionContext logDetails(Map<String, Object> details) {
    return new AmaazonExceptionContext(Collections.emptyMap(), details, null);
  }

  public static AmaazonExceptionContext systemMessage(String systemMessage) {
    return new AmaazonExceptionContext(Collections.emptyMap(), Collections.emptyMap(), systemMessage);
  }

  private static Map<String, Object> immutableOrEmpty(Map<String, Object> details) {
    return details == null || details.isEmpty() ? Collections.emptyMap() : Map.copyOf(details);
  }
}
