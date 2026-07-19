package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.application.service.BlacklistService;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistTokenCreatedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistUserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlacklistEventHandler {

  private final BlacklistService service;

  @EventListener
  public void handle(BlacklistTokenCreatedEvent event) {
    service.createBlacklistToken(event.jti(), event.expiredAt());
  }

  @EventListener
  public void handle(BlacklistUserCreatedEvent event) {
    service.createBlacklistUser(event.userId(), event.compromisedAt());
  }
}
