package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.request.SignUpRequest;
import io.github.metdaisy.amaazon.auth.application.dto.request.UserCredentialUpdateRequest;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.event.SignUpTask;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

  private final UserCredentialRepository repository;
  private final SocialCredentialRepository socialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthUserPort userPort;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void create(SignUpRequest request) {
    validateEmail(request.email());
    UserCredential credential = UserCredential.of(request.email(),
        passwordEncoder.encode(request.password()));
    eventPublisher.publishEvent(
        new SignUpTask(credential.getId(), request.name(), request.phoneNumber(),
            request.address()));
    repository.save(credential);
  }

  @Transactional
  public void createSocial(UUID userId, String provider, String providerId) {
    validateUserId(userId);
    SocialCredential credential = SocialCredential.of(userId, provider, providerId);
    socialRepository.save(credential);
  }

  @Transactional
  public void update(UUID userId, UserCredentialUpdateRequest request) {
    validateEmail(request.email());
    UserCredential credential = repository.findById(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND,
            Map.of("userId", userId)));
    credential.updateEmail(request.email());
    credential.updatePassword(passwordEncoder.encode(request.password()));
    eventPublisher.publishEvent(new JwtTokenCompromisedEvent(userId, Instant.now()));
  }

  public void verifyPassword(UUID id, String password) {
    UserCredential credential = repository.findById(id).orElseThrow(
        () -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND, Map.of("userId", id)));
    credential.matchPassword(passwordEncoder.encode(password));
  }

  private void validateEmail(String email) {
    if (!StringUtils.hasText(email)) {
      return;
    }
    if (repository.existsByEmail(email)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }
  }

  private void validateUserId(UUID userId) {
    if (!userPort.existsUser(userId)) {
      throw new AuthException(AuthErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
  }

}
