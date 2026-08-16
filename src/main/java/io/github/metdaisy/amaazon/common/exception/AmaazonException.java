package io.github.metdaisy.amaazon.common.exception;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public abstract class AmaazonException extends RuntimeException {

  private final String code;
  private final AmaazonErrorType errorType;
  private final String clientMessage;
  private final Map<String, Object> clientDetails;
  private final Map<String, Object> logDetails;
  private final String systemMessage;

  protected AmaazonException(AmaazonErrorCode errorCode) {
    this(errorCode, AmaazonExceptionContext.empty());
  }

  protected AmaazonException(
      AmaazonErrorCode errorCode,
      AmaazonExceptionContext context
  ) {
    super(requireErrorCode(errorCode).getMessage());
    this.errorType = errorCode.getErrorType();
    this.code = errorCode.getCode();
    this.clientMessage = errorCode.getMessage();
    this.clientDetails = sanitizeClientDetails(context.clientDetails());
    this.logDetails = redactLogDetails(context.logDetails());
    String baseSystemMessage = context.systemMessage() == null
        ? errorCode.getSystemMessage()
        : context.systemMessage();
    this.systemMessage = formatSystemMessage(baseSystemMessage);
  }

  private static AmaazonErrorCode requireErrorCode(AmaazonErrorCode errorCode) {
    return Objects.requireNonNull(errorCode, "errorCode must not be null");
  }

  private String formatSystemMessage(String baseMessage) {
    if (baseMessage == null && logDetails.isEmpty()) {
      return clientMessage;
    }

    String logDetailsMessage = logDetails.entrySet().stream()
        .map(entry -> entry.getKey() + ": " + entry.getValue())
        .collect(Collectors.joining(", ", "[", "]"));

    if (baseMessage == null) {
      return logDetailsMessage;
    }
    if (logDetails.isEmpty()) {
      return baseMessage;
    }
    return baseMessage + " " + logDetailsMessage;
  }

  private Map<String, Object> sanitizeClientDetails(Map<String, Object> details) {
    return details.entrySet().stream()
        .filter(entry -> !isSensitiveKey(entry.getKey()))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> redactLogDetails(Map<String, Object> details) {
    return details.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> redactIfSensitive(entry.getKey(), entry.getValue())));
  }

  private Object redactIfSensitive(String key, Object value) {
    if (isSensitiveKey(key)) {
      return "[REDACTED]";
    }
    return value;
  }

  private boolean isSensitiveKey(String key) {
    String normalizedKey = key.toLowerCase(Locale.ROOT);
    return normalizedKey.contains("token")
        || normalizedKey.contains("password")
        || normalizedKey.contains("secret")
        || normalizedKey.contains("credential")
        || normalizedKey.contains("authorization");
  }

}
