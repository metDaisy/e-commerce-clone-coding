package io.github.metdaisy.amaazon.global.exception.util;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class ErrorTypeResolver {
  public HttpStatus resolve(AmaazonErrorType errorType) {
    return switch (errorType) {
      case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
