package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import org.springframework.http.HttpStatus;

public class JwtExpiredException extends AmaazonException {

  public JwtExpiredException() {
    super(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.");
  }
}
