package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.request.SignUpRequest;
import io.github.metdaisy.amaazon.auth.application.dto.request.UserCredentialUpdateRequest;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.user.application.event.FormSignUpTask;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

  private final UserCredentialService userCredentialService;
  private final SocialCredentialService socialCredentialService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void create(SignUpRequest request) {
    UserCredential credential = userCredentialService.create(request.email(), request.password());
    eventPublisher.publishEvent(
        new FormSignUpTask(credential.getId(), request.name(), request.phoneNumber(),
            request.address()));
  }

  @Transactional
  public void createSocial(UUID userId, String provider, String providerId) {
    socialCredentialService.create(userId, provider, providerId);
  }

  @Transactional
  public void update(UUID userId, UserCredentialUpdateRequest request) {
    userCredentialService.update(userId, request.email(), request.password());
    eventPublisher.publishEvent(new JwtTokenCompromisedEvent(userId, Instant.now()));
  }

  public void verifyPassword(UUID id, String password) {
    userCredentialService.verifyPassword(id, password);
  }

}
