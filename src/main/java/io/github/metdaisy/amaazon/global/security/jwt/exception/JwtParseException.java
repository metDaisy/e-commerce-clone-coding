package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtParseException extends AmaazonException {

  public JwtParseException(String token) {
    super(HttpStatus.BAD_REQUEST, "잘못된 형식의 토큰입니다.", Map.of("token", token));
  }
}
