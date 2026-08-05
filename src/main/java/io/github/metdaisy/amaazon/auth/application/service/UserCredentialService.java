package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialAuthenticationException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

  @Value("${amaazon.login.locked-duration}")
  private Duration lockedDuration;
  @Value("${amaazon.login.max-attempt}")
  private int maxAttempt;
  private final UserCredentialRepository repository;

  @Transactional
  public void increaseViolationCount(String email) {
    UserCredential credential = repository.findByEmail(email)
        .orElseThrow(() -> new UserCredentialAuthenticationException(email));
    credential.increaseViolationCount(maxAttempt, lockedDuration);
  }

  @Transactional
  public void resetViolationOrNot(UUID userId) {
    UserCredential credential = repository.findByIdForUpdate(userId)
        .orElseThrow(() -> new UserCredentialAuthenticationException(userId));
    credential.resetViolationOrNot(Instant.now());
  }
}
