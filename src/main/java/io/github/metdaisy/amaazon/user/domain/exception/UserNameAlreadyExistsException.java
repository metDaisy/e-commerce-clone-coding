package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserNameAlreadyExistsException extends AmaazonException {

  public UserNameAlreadyExistsException(String name) {
    super(HttpStatus.CONFLICT, "이미 동일한 이름이 존재합니다.", Map.of("name", name));
  }
}
