package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AmaazonException {

  public UserNotFoundException(UUID userId) {
    super(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", Map.of("userId", userId));
  }
}
