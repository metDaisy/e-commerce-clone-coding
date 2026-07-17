package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserPhoneNumberAlreadyExistsException extends AmaazonException {

  public UserPhoneNumberAlreadyExistsException(String phoneNumber) {
    super(HttpStatus.CONFLICT, "이미 동일한 핸드폰 번호가 존재합니다.", Map.of("phoneNumber", phoneNumber));
  }
}
