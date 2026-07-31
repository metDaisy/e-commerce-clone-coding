package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.event.UserUpdatedEvent;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

  private final UserRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public User create(UserCreateRequest request) {
    validatePhoneNumber(request.phoneNumber(), false);
    User user = User.createUser(request.name(), request.phoneNumber(), request.address());
    eventPublisher.publishEvent(
        new UserCreatedEvent(user.getId(), request.email(), request.password()));
    return repository.save(user);
  }

  @Transactional
  public User update(UUID id, UserUpdateRequest request) {
    User user = repository.findById(id)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", id)));
    user.updateName(request.name());
    validatePhoneNumber(request.phoneNumber(), true);
    user.updatePhoneNumber(request.phoneNumber());
    user.updateAddress(request.address());
    eventPublisher.publishEvent(new UserUpdatedEvent(id, request.email(), request.password()));
    return user;
  }

  private void validatePhoneNumber(String phoneNumber, boolean isUpdated) {
    if (!StringUtils.hasText(phoneNumber)) {
      return;
    }
    int count = repository.countByPhoneNumber(phoneNumber);
    if (isUpdated) {
      if (count > 1) {
        throw new UserException(UserErrorCode.PHONE_ALREADY_EXISTS,
            Map.of("phoneNumber", phoneNumber));
      }
    }
    if (count > 0) {
      throw new UserException(UserErrorCode.PHONE_ALREADY_EXISTS,
          Map.of("phoneNumber", phoneNumber));
    }
  }

}
