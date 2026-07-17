package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.exception.UserNameAlreadyExistsException;
import io.github.metdaisy.amaazon.user.domain.exception.UserPhoneNumberAlreadyExistsException;
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
    validateName(request.name());
    validatePhoneNumber(request.phoneNumber());
    User user = User.createUser(request.name(), request.phoneNumber());
    eventPublisher.publishEvent(
            new UserCreatedEvent(user.getId(), request.email(), request.password()));
    return repository.save(user);
  }

  private void validateName(String name) {
    if (repository.existsByName(name)) {
      throw new UserNameAlreadyExistsException(name);
    }
  }

  private void validatePhoneNumber(String phoneNumber) {
    if (repository.existsByPhoneNumber(phoneNumber)) {
      throw new UserPhoneNumberAlreadyExistsException(phoneNumber);
    }
  }
}
