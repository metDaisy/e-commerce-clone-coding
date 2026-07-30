package io.github.metdaisy.amaazon.auth.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserCredentialRepository repository;
  @Mock
  private SocialCredentialRepository socialRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private AuthUserPort userPort;

  @InjectMocks
  private AuthService authService;

  @Test
  @DisplayName("create successfully")
  void create() {
    UUID userId = UUID.randomUUID();
    given(repository.existsByEmail("test@test.com")).willReturn(false);
    given(passwordEncoder.encode("password")).willReturn("encoded");

    authService.create(userId, "test@test.com", "password");

    verify(repository).save(any(UserCredential.class));
  }

  @Test
  @DisplayName("create fails when email exists")
  void create_fail_emailExists() {
    UUID userId = UUID.randomUUID();
    given(repository.existsByEmail("test@test.com")).willReturn(true);

    assertThrows(AuthException.class, () -> authService.create(userId, "test@test.com", "password"));
    verify(repository, never()).save(any(UserCredential.class));
  }

  @Test
  @DisplayName("createSocial successfully")
  void createSocial() {
    UUID userId = UUID.randomUUID();
    given(userPort.existsUser(userId)).willReturn(false);

    authService.createSocial(userId, "google", "123");

    verify(socialRepository).save(any(SocialCredential.class));
  }

  @Test
  @DisplayName("createSocial fails when user not found")
  void createSocial_fail_userNotFound() {
    UUID userId = UUID.randomUUID();
    given(userPort.existsUser(userId)).willReturn(true);

    assertThrows(AuthException.class, () -> authService.createSocial(userId, "google", "123"));
    verify(socialRepository, never()).save(any(SocialCredential.class));
  }
}
