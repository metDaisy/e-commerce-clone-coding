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
import io.github.metdaisy.amaazon.auth.application.service.BlacklistService;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistTokenCreatedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistUserCreatedEvent;

@ExtendWith(MockitoExtension.class)
class BlacklistEventHandlerTest {

  @Mock
  private BlacklistService blacklistService;

  @InjectMocks
  private BlacklistEventHandler handler;

  @Test
  @DisplayName("handle BlacklistTokenCreatedEvent")
  void handleToken() {
    Instant now = Instant.now();
    BlacklistTokenCreatedEvent event = new BlacklistTokenCreatedEvent("jti-123", now);

    handler.handle(event);

    verify(blacklistService).createBlacklistToken("jti-123", now);
  }

  @Test
  @DisplayName("handle BlacklistUserCreatedEvent")
  void handleUser() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    BlacklistUserCreatedEvent event = new BlacklistUserCreatedEvent(userId, now);

    handler.handle(event);

    verify(blacklistService).createBlacklistUser(userId, now);
  }
}
