package io.github.metdaisy.amaazon.auth.application.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

  private final UserCredentialRepository repository;
  private final SocialCredentialRepository socialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthUserPort userPort;

  public void create(UUID userId, String email, String password) {
    validateEmail(email);
    UserCredential credential = UserCredential.of(userId, email, passwordEncoder.encode(password));
    repository.save(credential);
  }

  public void createSocial(UUID userId, String provider, String providerId) {
    validateUserId(userId);
    SocialCredential credential = SocialCredential.of(userId, provider, providerId);
    socialRepository.save(credential);
  }

  private void validateEmail(String email) {
    if (repository.existsByEmail(email)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }
  }

  private void validateUserId(UUID userId) {
    if (userPort.existsUser(userId)) {
      throw new AuthException(AuthErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
  }
}
