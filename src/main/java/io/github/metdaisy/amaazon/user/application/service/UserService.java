package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.application.event.FormSignUpTask;
import io.github.metdaisy.amaazon.user.application.event.UserDeactivatedEvent;
import io.github.metdaisy.amaazon.user.application.mapper.UserMapper;
import io.github.metdaisy.amaazon.user.application.port.out.UserLoginEmailQuery;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
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
  private final UserLoginEmailQuery userLoginEmailQuery;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void create(FormSignUpTask task) {
    validatePhoneNumber(task.phoneNumber());
    User user = User.createUser(task.id(), task.name(), task.phoneNumber());
    repository.save(user);
  }

  @Transactional
  public User update(UUID id, UserUpdateRequest request) {
    User user = findById(id);
    user.updateName(request.name());
    validatePhoneNumber(request.phoneNumber());
    user.updatePhoneNumber(request.phoneNumber());
    return user;
  }

  @Transactional
  public UserResponse updateProfile(UUID id, UserUpdateRequest request) {
    return toProfileResponse(update(id, request));
  }

  public User find(UUID id) {
    return findById(id);
  }

  public UserResponse findProfile(UUID id) {
    return toProfileResponse(find(id));
  }

  @Transactional
  public void deactivate(UUID id) {
    User user = findById(id);
    Instant deactivatedAt = Instant.now();
    user.deactivate();
    eventPublisher.publishEvent(new UserDeactivatedEvent(UUID.randomUUID(), id, deactivatedAt));
  }

  private void validatePhoneNumber(String phoneNumber) {
    if (!StringUtils.hasText(phoneNumber)) {
      return;
    }
    if (repository.existsByPhoneNumber(phoneNumber)) {
      throw new UserException(UserErrorCode.PHONE_ALREADY_EXISTS);
    }
  }

  private User findById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("userId", id))));
  }

  private UserResponse toProfileResponse(User user) {
    String loginEmail = userLoginEmailQuery.findByUserId(user.getId()).orElse(null);
    return userMapper.toDto(user, loginEmail);
  }
}
