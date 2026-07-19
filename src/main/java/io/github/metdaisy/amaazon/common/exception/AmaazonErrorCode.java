package io.github.metdaisy.amaazon.common.exception;

public interface AmaazonErrorCode {
  String getCode();
  String getMessage();
  AmaazonErrorType getErrorType();
}
