package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
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
public class AuthService {

  private final UserCredentialRepository repository;
  private final SocialCredentialRepository socialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthUserPort userPort;

  @Transactional
  public void create(UUID userId, String email, String password) {
    validateEmail(email, false);
    UserCredential credential = UserCredential.of(userId, email, passwordEncoder.encode(password));
    repository.save(credential);
  }

  @Transactional
  public void createSocial(UUID userId, String provider, String providerId) {
    validateUserId(userId);
    SocialCredential credential = SocialCredential.of(userId, provider, providerId);
    socialRepository.save(credential);
  }

  public void update(UUID userId, String email, String password) {
    UserCredential credential = repository.findByUserId(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_CREDENTIAL_NOT_FOUND, Map.of("userId", userId)));
    validateEmail(email, true);
    credential.updateEmail(email);
    credential.updatePassword(passwordEncoder.encode(password));
  }

  private void validateEmail(String email, boolean isUpdated) {
    if (!StringUtils.hasText(email)) {
      return;
    }
    int count = repository.countByEmail(email);
    if (isUpdated) {
      if (count > 1) {
        throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
      }
    }
    if (count > 0) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
    }
  }

  private void validateUserId(UUID userId) {
    if (userPort.existsUser(userId)) {
      throw new AuthException(AuthErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
  }

}
