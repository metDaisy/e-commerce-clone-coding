package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FormUserDetailsService implements UserDetailsService {

  private final AuthUserPort userPort;
  private final UserCredentialRepository repository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) {
    UserCredential credential = repository.findByEmail(email)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND,
            Map.of("email", email)));
    return userPort.loadUser(credential.getId())
        .map(userDto -> new FormUserDetails(userDto.id(), userDto.role(), credential.getPassword(),
            userDto.isEnabled()))
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
            Map.of("email", email)));
  }

}
