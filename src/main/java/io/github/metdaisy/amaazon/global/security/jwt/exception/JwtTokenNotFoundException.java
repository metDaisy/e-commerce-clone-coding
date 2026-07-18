package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtTokenNotFoundException extends AmaazonException {

  public JwtTokenNotFoundException(String token) {
    super(HttpStatus.NOT_FOUND, "토큰을 찾을 수 없습니다.",
            Map.of("refreshToken", token, "detailMessage", "cache 에 저장된 토큰을 찾을 수 없습니다."));
  }
}
