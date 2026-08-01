package io.github.metdaisy.amaazon.auth.infra.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;

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
    UUID userId = credential.getId();
    AuthUserDto userDto = userPort.loadUser(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
            Map.of("userId", userId)));
    return new FormUserDetails(userDto.id(), userDto.role(), credential.getPassword());
  }
}
