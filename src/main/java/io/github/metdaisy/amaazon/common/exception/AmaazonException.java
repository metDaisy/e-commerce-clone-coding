package io.github.metdaisy.amaazon.common.exception;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.Getter;

public abstract class AmaazonException extends RuntimeException {

  @Getter
  private final String code;
  @Getter
  private final AmaazonErrorType errorType;
  private final String detailMessage; // logging 을 위한 message

  public AmaazonException(AmaazonErrorCode errorCode) {
    this(errorCode, Collections.emptyMap());
  }

  public AmaazonException(AmaazonErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.errorType = errorCode.getErrorType();
    this.code = errorCode.getCode();
    this.detailMessage = formatDetailMessage(errorCode.getMessage(), details);
  }

  private String formatDetailMessage(String defaultMessage, Map<String, Object> details) {
    if (details.isEmpty()) {
      return defaultMessage;
    }
    return details.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
            .collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", this.code + "[", "]")
            .add("ErrorType=" + errorType)
            .add("detailMessage='" + detailMessage + "'")
            .toString();
  }
}
