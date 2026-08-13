package io.github.metdaisy.amaazon.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.dto.request.SignUpRequest;
import io.github.metdaisy.amaazon.auth.application.dto.request.UserCredentialUpdateRequest;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.user.application.event.FormSignUpTask;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
class AuthServiceTest {

  @Mock
  private UserCredentialService userCredentialService;
  @Mock
  private SocialCredentialService socialCredentialService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private AuthService authService;

  @Test
  @DisplayName("일반 인증 생성 성공: UserCredentialService를 호출하고 이벤트를 발행한다")
  void create_success() {
    // given
    UUID userId = UUID.randomUUID();
    UserCredential credential = mock(UserCredential.class);
    given(credential.getId()).willReturn(userId);
    given(userCredentialService.create("test@example.com", "Password1!")).willReturn(credential);

    // when
    authService.create(new SignUpRequest("tester", "test@example.com", "Password1!", "01012345678"));

    // then
    ArgumentCaptor<FormSignUpTask> captor = ArgumentCaptor.forClass(FormSignUpTask.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().id()).isEqualTo(userId);
    assertThat(captor.getValue().name()).isEqualTo("tester");
    assertThat(captor.getValue().phoneNumber()).isEqualTo("01012345678");
  }

  @Test
  @DisplayName("소셜 인증 생성 성공: SocialCredentialService를 호출한다")
  void createSocial_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    authService.createSocial(userId, "google", "provider-id");

    // then
    verify(socialCredentialService).create(userId, "google", "provider-id");
  }

  @Test
  @DisplayName("일반 인증 수정 성공: UserCredentialService를 호출하고 이벤트를 발행한다")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    authService.update(userId, new UserCredentialUpdateRequest("new@example.com", "NewPassword1!"));

    // then
    verify(userCredentialService).update(userId, "new@example.com", "NewPassword1!");
    verify(eventPublisher).publishEvent(any(JwtTokenCompromisedEvent.class));
  }
  @Test
  @DisplayName("비밀번호 검증 성공: UserCredentialService를 호출한다")
  void verifyPassword_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    authService.verifyPassword(userId, "Password1!");

    // then
    verify(userCredentialService).verifyPassword(userId, "Password1!");
  }
}
