package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEventHandler {

  private final AuthService service;

  @EventListener
  public void handle(UserCreatedEvent event) {
    service.create(event.id(), event.email(), event.password());
  }
}
