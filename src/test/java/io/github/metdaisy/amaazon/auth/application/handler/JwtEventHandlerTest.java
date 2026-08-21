package io.github.metdaisy.amaazon.auth.application.handler;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import io.github.metdaisy.amaazon.user.application.event.UserDeactivatedEvent;

@ExtendWith(MockitoExtension.class)
class JwtEventHandlerTest {

  @Mock
  private BlacklistRegistry registry;

  @InjectMocks
  private JwtEventHandler handler;

  @Test
  @DisplayName("handle JwtTokenCompromisedEvent")
  void handle() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    JwtTokenCompromisedEvent event = new JwtTokenCompromisedEvent(userId, now);

    handler.handle(event);

    verify(registry).blacklistUser(userId, now);
  }

  @Test
  void handle_userDeactivatedEvent() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    UserDeactivatedEvent event = new UserDeactivatedEvent(UUID.randomUUID(), userId, now);

    handler.handle(event);

    verify(registry).blacklistUser(userId, now);
  }
}
