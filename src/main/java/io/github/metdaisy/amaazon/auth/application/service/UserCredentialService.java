package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialAuthenticationException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import io.github.metdaisy.amaazon.global.security.config.LoginPolicyProperties;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserCredentialService {

  private final LoginPolicyProperties properties;
  private final UserCredentialRepository repository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserCredential create(String email, String password) {
    validateEmail(email);
    UserCredential credential = UserCredential.of(email, passwordEncoder.encode(password));
    repository.save(credential);
    return credential;
  }

  @Transactional
  public void update(UUID id, String email, String password) {
    validateEmail(email);
    UserCredential credential = repository.findById(id)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND,
            Map.of("userId", id)));
    credential.updateEmail(email);
    credential.updatePassword(passwordEncoder.encode(password));
  }

  public void verifyPassword(UUID id, String password) {
    UserCredential credential = repository.findById(id).orElseThrow(
        () -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND, Map.of("userId", id)));
    credential.validatePassword(encoded -> passwordEncoder.matches(password, encoded));
  }

  @Transactional
  public void increaseViolationCount(String email) {
    UserCredential credential = repository.findByEmail(email)
        .orElseThrow(() -> new UserCredentialAuthenticationException(email));
    credential.increaseViolationCount(properties.maxAttempt(), properties.lockedDuration());
  }

  @Transactional
  public void resetViolationOrNot(UUID userId) {
    UserCredential credential = repository.findByIdForUpdate(userId)
        .orElseThrow(() -> new UserCredentialAuthenticationException(userId));
    credential.resetViolationOrNot(Instant.now());
  }

  private void validateEmail(String email) {
    if (!StringUtils.hasText(email)) {
      return;
    }
    if (repository.existsByEmail(email)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }
  }
}
