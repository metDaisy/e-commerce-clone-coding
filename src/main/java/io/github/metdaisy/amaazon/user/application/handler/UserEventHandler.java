package io.github.metdaisy.amaazon.user.application.handler;

import io.github.metdaisy.amaazon.auth.application.event.FormSignUpTask;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventHandler {

  private final UserService service;

  @EventListener
  public void handle(FormSignUpTask task) {
    service.create(task);
  }
}
