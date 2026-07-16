package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public User create(UserCreateRequest request) {

    User user = User.createUser(request.name(), request.phoneNumber());
    eventPublisher.publishEvent(
            new UserCreatedEvent(user.getId(), request.email(), request.password()));
    return repository.save(user);
  }
}
