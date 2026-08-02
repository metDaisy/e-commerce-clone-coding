package io.github.metdaisy.amaazon.user.application.handler;

import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.event.SignUpTask;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventHandler 테스트")
class UserEventHandlerTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private UserEventHandler userEventHandler;

  @Test
  @DisplayName("handle: SignUpTask 발생 시 UserService.create 를 호출한다")
  void handle() {
    // given
    SignUpTask task = new SignUpTask(java.util.UUID.randomUUID(), "tester", "01012345678", "Seoul");

    // when
    userEventHandler.handle(task);

    // then
    verify(userService).create(task);
  }
}
