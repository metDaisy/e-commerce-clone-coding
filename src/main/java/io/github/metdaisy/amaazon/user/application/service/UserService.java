package io.github.metdaisy.amaazon.user.application.service;

import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

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
      throw new UserException(UserErrorCode.NAME_ALREADY_EXISTS, Map.of("name", name));
    }
  }

  private void validatePhoneNumber(String phoneNumber) {
    if (repository.existsByPhoneNumber(phoneNumber)) {
      throw new UserException(UserErrorCode.PHONE_ALREADY_EXISTS,
          Map.of("phoneNumber", phoneNumber));
    }
  }
}
