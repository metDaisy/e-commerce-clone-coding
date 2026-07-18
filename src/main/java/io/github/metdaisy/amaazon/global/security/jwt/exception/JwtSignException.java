package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtSignException extends AmaazonException {

  public JwtSignException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 생성(서명) 중 오류가 발생했습니다.", Map.of("detail", message));
  }
}
