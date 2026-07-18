package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtClaimExtractionException extends AmaazonException {

  public JwtClaimExtractionException() {
    super(HttpStatus.BAD_REQUEST, "잘못된 형식의 토큰입니다.", Map.of("payload", "토큰에서 payload 를 파싱할 수 없습니다."));
  }
}
