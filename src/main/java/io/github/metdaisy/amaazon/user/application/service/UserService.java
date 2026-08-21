package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.application.event.FormSignUpTask;
import io.github.metdaisy.amaazon.user.application.event.UserDeactivatedEvent;
import io.github.metdaisy.amaazon.user.application.mapper.UserMapper;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.time.Instant;
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
  private final UserMapper userMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void create(FormSignUpTask task) {
    validateNameForSignup(task.id(), task.name());
    validatePhoneNumberForSignup(task.id(), task.phoneNumber());
    User user = User.createUser(task.id(), task.name(), task.phoneNumber());
    repository.save(user);
  }

  @Transactional
  public UserResponse update(UUID id, UserUpdateRequest request) {
    User user = findWithRolesById(id);

    if (StringUtils.hasText(request.name())) {
      validateNameForUpdate(id, request.name());
      user.updateName(request.name());
    }
    if (StringUtils.hasText(request.phoneNumber())) {
      validatePhoneNumberForUpdate(id, request.phoneNumber());
      user.updatePhoneNumber(request.phoneNumber());
    }
    return toProfileResponse(user);
  }

  public UserResponse findProfile(UUID id) {
    return toProfileResponse(findWithRolesById(id));
  }

  @Transactional
  public void deactivate(UUID id) {
    User user = findById(id);
    Instant deactivatedAt = Instant.now();
    user.deactivate();
    eventPublisher.publishEvent(new UserDeactivatedEvent(UUID.randomUUID(), id, deactivatedAt));
  }

  private void validateNameForSignup(UUID userId, String name) {
    if (repository.existsByNameAndIsEnabledTrue(name)) {
      throw duplicateException(UserErrorCode.NAME_ALREADY_EXISTS, userId);
    }
  }

  private void validatePhoneNumberForSignup(UUID userId, String phoneNumber) {
    if (!StringUtils.hasText(phoneNumber)) {
      return;
    }
    if (repository.existsByPhoneNumberAndIsEnabledTrue(phoneNumber)) {
      throw duplicateException(UserErrorCode.PHONE_ALREADY_EXISTS, userId);
    }
  }

  private User findById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("userId", id))));
  }

  private void validateNameForUpdate(UUID userId, String name) {
    if (repository.existsByNameAndIsEnabledTrueAndIdNot(name, userId)) {
      throw duplicateException(UserErrorCode.NAME_ALREADY_EXISTS, userId);
    }
  }

  private void validatePhoneNumberForUpdate(UUID userId, String phoneNumber) {
    if (repository.existsByPhoneNumberAndIsEnabledTrueAndIdNot(phoneNumber, userId)) {
      throw duplicateException(UserErrorCode.PHONE_ALREADY_EXISTS, userId);
    }
  }

  private UserException duplicateException(UserErrorCode errorCode, UUID userId) {
    return new UserException(errorCode, AmaazonExceptionContext.logDetails(Map.of("userId", userId)));
  }

  private User findWithRolesById(UUID id) {
    return repository.findWithRolesById(id)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("userId", id))));
  }

  private UserResponse toProfileResponse(User user) {
    return userMapper.toDto(user);
  }
}
