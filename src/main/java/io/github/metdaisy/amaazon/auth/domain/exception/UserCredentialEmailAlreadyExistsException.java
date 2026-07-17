package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserCredentialEmailAlreadyExistsException extends AmaazonException {

  public UserCredentialEmailAlreadyExistsException(String email) {
    super(HttpStatus.CONFLICT, "이미 동일한 이메일이 존재합니다.", Map.of("email", email));
  }
}
