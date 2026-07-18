package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtRoleNotFoundException extends AmaazonException {

  public JwtRoleNotFoundException() {
    super(HttpStatus.NOT_FOUND, "잘못된 형식의 토큰입니다.", Map.of("role", "토큰에 role 이 포함되지 않았습니다."));
  }
}
