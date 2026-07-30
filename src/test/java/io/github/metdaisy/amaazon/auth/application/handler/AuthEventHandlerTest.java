package io.github.metdaisy.amaazon.auth.application.handler;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;

@ExtendWith(MockitoExtension.class)
class AuthEventHandlerTest {

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthEventHandler handler;

  @Test
  @DisplayName("handle UserCreatedEvent successfully")
  void handle() {
    UUID userId = UUID.randomUUID();
    UserCreatedEvent event = new UserCreatedEvent(userId, "test@test.com", "password");

    handler.handle(event);

    verify(authService).create(userId, "test@test.com", "password");
  }
}
