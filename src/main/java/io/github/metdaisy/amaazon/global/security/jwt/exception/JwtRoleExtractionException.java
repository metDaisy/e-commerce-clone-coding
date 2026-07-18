package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtRoleExtractionException extends AmaazonException {

  public JwtRoleExtractionException() {
    super(HttpStatus.BAD_REQUEST, "잘못된 형식의 토큰입니다.",
            Map.of("role", "토큰에서 role 을 parsing 할 수 없습니다."));
  }
}
