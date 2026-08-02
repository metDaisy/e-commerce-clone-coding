package io.github.metdaisy.amaazon.user.application.handler;

import io.github.metdaisy.amaazon.auth.domain.event.SignUpTask;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventHandler {

  private final UserService service;

  @EventListener
  public void handle(SignUpTask task) {
    service.create(task);
  }
}
