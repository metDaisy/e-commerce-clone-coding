package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import org.springframework.http.HttpStatus;

public class JwtVerificationException extends AmaazonException {

  public JwtVerificationException() {
    super(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다.");
  }
}
