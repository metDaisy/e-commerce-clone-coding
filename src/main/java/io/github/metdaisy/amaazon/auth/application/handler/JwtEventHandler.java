package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import io.github.metdaisy.amaazon.user.application.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class JwtEventHandler {

  private final BlacklistRegistry registry;

  @Async("outboxWorker")
  @TransactionalEventListener
  public void handle(JwtTokenCompromisedEvent event) {
    registry.blacklistUser(event.userId(), event.compromisedAt());
  }

  @Async("outboxWorker")
  @TransactionalEventListener
  public void handle(UserDeactivatedEvent event) {
    registry.blacklistUser(event.userId(), event.deactivatedAt());
  }

}
