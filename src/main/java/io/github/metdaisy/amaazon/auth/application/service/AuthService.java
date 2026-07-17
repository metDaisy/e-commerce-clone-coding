package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialEmailAlreadyExistsException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

  private final UserCredentialRepository repository;
  private final PasswordEncoder passwordEncoder;

  public void create(UUID userId, String email, String password) {
    validateEmail(email);
    UserCredential credential = UserCredential.of(userId, email, passwordEncoder.encode(password));
    repository.save(credential);
  }

  private void validateEmail(String email) {
    if (repository.existsByEmail(email)) {
      throw new UserCredentialEmailAlreadyExistsException(email);
    }
  }
}
