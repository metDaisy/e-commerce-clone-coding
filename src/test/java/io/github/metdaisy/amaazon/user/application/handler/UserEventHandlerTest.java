package io.github.metdaisy.amaazon.user.application.handler;

import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.application.event.FormSignUpTask;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import java.util.UUID;
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
    FormSignUpTask task = new FormSignUpTask(UUID.randomUUID(), "tester", "01012345678", "Seoul");

    // when
    userEventHandler.handle(task);

    // then
    verify(userService).create(task);
  }
}
