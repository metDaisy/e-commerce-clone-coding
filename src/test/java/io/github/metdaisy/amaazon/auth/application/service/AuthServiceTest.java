package io.github.metdaisy.amaazon.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
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
  @DisplayName("일반 인증 생성 성공: 비밀번호를 암호화해 자격 증명을 저장한다")
  void create_success() {
    // given
    UUID userId = UUID.randomUUID();
    given(repository.countByEmail("test@example.com")).willReturn(0);
    given(passwordEncoder.encode("Password1!")).willReturn("encoded-password");

    // when
    authService.create(userId, "test@example.com", "Password1!");

    // then
    ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue())
        .extracting(UserCredential::getUserId, UserCredential::getEmail,
            UserCredential::getPassword)
        .containsExactly(userId, "test@example.com", "encoded-password");
  }

  @ParameterizedTest(name = "[{index}] 중복 개수={0}")
  @ValueSource(ints = {1, 2})
  @DisplayName("일반 인증 생성 실패: 중복 이메일이면 예외를 던진다")
  void create_failure_whenEmailAlreadyExists(int duplicateCount) {
    // given
    UUID userId = UUID.randomUUID();
    given(repository.countByEmail("test@example.com")).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> authService.create(userId, "test@example.com", "Password1!"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.EMAIL_ALREADY_EXISTS.getCode());
    verify(repository, never()).save(any(UserCredential.class));
  }

  @Test
  @DisplayName("소셜 인증 생성 성공: 연결 가능한 사용자면 자격 증명을 저장한다")
  void createSocial_success() {
    // given
    UUID userId = UUID.randomUUID();
    given(userPort.existsUser(userId)).willReturn(false);

    // when
    authService.createSocial(userId, "google", "provider-id");

    // then
    verify(socialRepository).save(any(SocialCredential.class));
  }

  @Test
  @DisplayName("소셜 인증 생성 실패: 연결할 사용자를 찾지 못하면 예외를 던진다")
  void createSocial_failure_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    given(userPort.existsUser(userId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.createSocial(userId, "google", "provider-id"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.USER_NOT_FOUND.getCode());
    verify(socialRepository, never()).save(any(SocialCredential.class));
  }

  @Test
  @DisplayName("일반 인증 수정 성공: 이메일과 암호화된 비밀번호를 변경한다")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();
    UserCredential credential = UserCredential.of(userId, "old@example.com", "old-password");
    given(repository.findByUserId(userId)).willReturn(Optional.of(credential));
    given(repository.countByEmail("new@example.com")).willReturn(0);
    given(passwordEncoder.encode("NewPassword1!")).willReturn("new-encoded-password");

    // when
    authService.update(userId, "new@example.com", "NewPassword1!");

    // then
    assertThat(credential)
        .extracting(UserCredential::getEmail, UserCredential::getPassword)
        .containsExactly("new@example.com", "new-encoded-password");
  }

  @Test
  @DisplayName("일반 인증 수정 실패: 자격 증명을 찾지 못하면 예외를 던진다")
  void update_failure_whenCredentialDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    given(repository.findByUserId(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.update(userId, "new@example.com", "Password1!"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.USER_CREDENTIAL_NOT_FOUND.getCode());
    verify(passwordEncoder, never()).encode(any());
  }

  @ParameterizedTest(name = "[{index}] 중복 개수={0}")
  @ValueSource(ints = {1, 2})
  @DisplayName("일반 인증 수정 실패: 중복 이메일이면 예외를 던진다")
  void update_failure_whenEmailAlreadyExists(int duplicateCount) {
    // given
    UUID userId = UUID.randomUUID();
    UserCredential credential = UserCredential.of(userId, "old@example.com", "old-password");
    given(repository.findByUserId(userId)).willReturn(Optional.of(credential));
    given(repository.countByEmail("new@example.com")).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> authService.update(userId, "new@example.com", "Password1!"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.EMAIL_ALREADY_EXISTS.getCode());
    verify(passwordEncoder, never()).encode(any());
  }
}
