package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialAuthenticationException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FormUserDetailsService implements UserDetailsService {

  private final AuthUserPort userPort;
  private final UserCredentialRepository repository;
  @Value("${amaazon.login.max-attempt}")
  private int maxAttempt;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) {
    UserCredential credential = repository.findByEmail(email)
        .orElseThrow(() -> new UserCredentialAuthenticationException(email));
    return userPort.loadUser(credential.getId())
        .map(userDto -> new FormUserDetails(userDto.id(),
            userDto.rolesCsv(),
            credential.getPasswordHash(),
            userDto.isEnabled(),
            credential.isLocked(maxAttempt, Instant.now())))
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("email", email))));
  }

}
