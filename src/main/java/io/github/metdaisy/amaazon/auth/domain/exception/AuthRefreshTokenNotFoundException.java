package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthRefreshTokenNotFoundException extends AmaazonException {

  public AuthRefreshTokenNotFoundException(String token) {
    super(HttpStatus.NOT_FOUND, "토큰을 찾을 수 없습니다.",
            Map.of("refreshToken", token, "detailMessage", "refreshToken DB 에서 해당 토큰을 찾을 수 없습니다."));
  }
}
