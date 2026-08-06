package io.github.metdaisy.amaazon.auth.application.handler;

import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.event.FormLoginSuccessEvent;
import io.github.metdaisy.amaazon.auth.application.event.IncorrectPasswordEvent;
import io.github.metdaisy.amaazon.auth.application.event.JwtLogoutSuccessEvent;
import io.github.metdaisy.amaazon.auth.application.service.UserCredentialService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCredentialEventHandler 테스트")
class UserCredentialEventHandlerTest {

  @Mock
  private UserCredentialService userCredentialService;

  @InjectMocks
  private UserCredentialEventHandler handler;

  @Test
  @DisplayName("handle(IncorrectPasswordEvent) - 로그인 실패 시 실패 횟수 증가를 호출한다")
  void handleIncorrectPasswordEvent_success() {
    // given
    String email = "test@example.com";
    IncorrectPasswordEvent event = new IncorrectPasswordEvent(email);

    // when
    handler.handle(event);

    // then
    verify(userCredentialService).increaseViolationCount(email);
  }

  @Test
  @DisplayName("handle(FormLoginSuccessEvent) - 폼 로그인 성공 시 잠금 상태 리셋을 호출한다")
  void handleFormLoginSuccessEvent_success() {
    // given
    UUID userId = UUID.randomUUID();
    FormLoginSuccessEvent event = new FormLoginSuccessEvent(userId);

    // when
    handler.handle(event);

    // then
    verify(userCredentialService).resetViolationOrNot(userId);
  }

  @Test
  @DisplayName("handle(JwtLogoutSuccessEvent) - 로그아웃 성공 시 잠금 상태 리셋을 호출한다")
  void handleJwtLogoutSuccessEvent_success() {
    // given
    UUID userId = UUID.randomUUID();
    JwtLogoutSuccessEvent event = new JwtLogoutSuccessEvent(userId);

    // when
    handler.handle(event);

    // then
    verify(userCredentialService).resetViolationOrNot(userId);
  }
}
