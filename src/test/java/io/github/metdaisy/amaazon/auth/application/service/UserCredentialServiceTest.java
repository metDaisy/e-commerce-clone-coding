package io.github.metdaisy.amaazon.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialAuthenticationException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import io.github.metdaisy.amaazon.global.security.config.LoginPolicyProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 자격증명 서비스 테스트")
class UserCredentialServiceTest {

  @Mock
  private LoginPolicyProperties properties;
  @Mock
  private UserCredentialRepository repository;
  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserCredentialService userCredentialService;

  @Test
  @DisplayName("create - 성공")
  void create_success() {
    // given
    given(repository.existsByEmail("test@example.com")).willReturn(false);
    given(passwordEncoder.encode("password")).willReturn("encoded-password");

    // when
    UserCredential credential = userCredentialService.create("test@example.com", "password");

    // then
    verify(repository).save(credential);
    assertThat(credential.getEmail()).isEqualTo("test@example.com");
    assertThat(credential.getPasswordHash()).isEqualTo("encoded-password");
  }

  @Test
  @DisplayName("create - 실패: 이메일 중복")
  void create_failure_email_exists() {
    given(repository.existsByEmail("test@example.com")).willReturn(true);

    assertThatThrownBy(() -> userCredentialService.create("test@example.com", "password"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.EMAIL_ALREADY_EXISTS.getCode());
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("update - 성공")
  void update_success() {
    UUID id = UUID.randomUUID();
    UserCredential credential = UserCredential.of("old@example.com", "old-password");
    given(repository.existsByEmail("new@example.com")).willReturn(false);
    given(repository.findById(id)).willReturn(Optional.of(credential));
    given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

    userCredentialService.update(id, "new@example.com", "new-password");

    assertThat(credential.getEmail()).isEqualTo("new@example.com");
    assertThat(credential.getPasswordHash()).isEqualTo("new-encoded-password");
  }

  @Test
  @DisplayName("verifyPassword - 성공")
  void verifyPassword_success() {
    UUID id = UUID.randomUUID();
    UserCredential credential = UserCredential.of("test@example.com", "encoded-password");
    given(repository.findById(id)).willReturn(Optional.of(credential));
    given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);

    userCredentialService.verifyPassword(id, "password");
  }

  @Test
  @DisplayName("increaseViolationCount - 성공")
  void increaseViolationCount_success() {
    String email = "test@example.com";
    UserCredential credential = mock(UserCredential.class);
    given(repository.findByEmail(email)).willReturn(Optional.of(credential));
    given(properties.maxAttempt()).willReturn(5);
    given(properties.lockedDuration()).willReturn(Duration.ofMinutes(30));

    userCredentialService.increaseViolationCount(email);

    verify(credential).increaseViolationCount(5, Duration.ofMinutes(30));
  }

  @Test
  @DisplayName("increaseViolationCount - 실패: 유저 없음")
  void increaseViolationCount_failure_not_found() {
    given(repository.findByEmail("test@example.com")).willReturn(Optional.empty());

    assertThatThrownBy(() -> userCredentialService.increaseViolationCount("test@example.com"))
        .isInstanceOf(UserCredentialAuthenticationException.class);
  }

  @Test
  @DisplayName("resetViolationOrNot - 성공")
  void resetViolationOrNot_success() {
    UUID id = UUID.randomUUID();
    UserCredential credential = mock(UserCredential.class);
    given(repository.findByIdForUpdate(id)).willReturn(Optional.of(credential));

    userCredentialService.resetViolationOrNot(id);

    verify(credential).resetViolationOrNot(any());
  }
  @Test
  @DisplayName("resetViolationOrNot - 실패: 유저 없음")
  void resetViolationOrNot_failure_not_found() {
    UUID id = UUID.randomUUID();
    given(repository.findByIdForUpdate(id)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userCredentialService.resetViolationOrNot(id))
        .isInstanceOf(UserCredentialAuthenticationException.class);
  }
}
