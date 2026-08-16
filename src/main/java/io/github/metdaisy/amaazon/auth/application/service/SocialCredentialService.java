package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialCredentialService {

  private final SocialCredentialRepository repository;
  private final AuthUserPort userPort;

  @Transactional
  public void create(UUID userId, String provider, String providerId) {
    validateUserId(userId);
    SocialCredential credential = SocialCredential.of(userId, provider, providerId);
    repository.save(credential);
  }

  private void validateUserId(UUID userId) {
    if (!userPort.existsUser(userId)) {
      throw new AuthException(AuthErrorCode.USER_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of("userId", userId)));
    }
  }
}
