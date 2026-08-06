package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.application.event.FormLoginSuccessEvent;
import io.github.metdaisy.amaazon.auth.application.event.IncorrectPasswordEvent;
import io.github.metdaisy.amaazon.auth.application.event.JwtLogoutSuccessEvent;
import io.github.metdaisy.amaazon.auth.application.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCredentialEventHandler {

  private final UserCredentialService service;

  @EventListener
  public void handle(IncorrectPasswordEvent event) {
    service.increaseViolationCount(event.email());
  }

  @EventListener
  public void handle(FormLoginSuccessEvent event) {
    service.resetViolationOrNot(event.userId());
  }

  @EventListener
  public void handle(JwtLogoutSuccessEvent event) {
    service.resetViolationOrNot(event.userId());
  }
}
