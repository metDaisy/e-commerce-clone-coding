package io.github.metdaisy.amaazon.auth.domain.exception;

import java.util.UUID;
import org.springframework.security.core.AuthenticationException;

public class UserCredentialAuthenticationException extends AuthenticationException {

  public UserCredentialAuthenticationException(String email) {
    super("email(%s) 을 찾을 수 없습니다.".formatted(email));
  }

  public UserCredentialAuthenticationException(UUID userId) {
    super("userId(%s) 을 찾을 수 없습니다.".formatted(userId));
  }
}
