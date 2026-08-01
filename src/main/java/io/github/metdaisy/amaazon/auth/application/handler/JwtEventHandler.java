package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtEventHandler {

  private final BlacklistRegistry registry;

  @ApplicationModuleListener
  public void handle(JwtTokenCompromisedEvent event) {
    registry.blacklistUser(event.userId(), event.compromisedAt());
  }

}
