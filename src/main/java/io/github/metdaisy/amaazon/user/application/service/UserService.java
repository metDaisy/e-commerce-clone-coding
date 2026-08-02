package io.github.metdaisy.amaazon.user.application.service;

import io.github.metdaisy.amaazon.auth.domain.event.SignUpTask;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

  private final UserRepository repository;

  @Transactional
  public void create(SignUpTask task) {
    validatePhoneNumber(task.phoneNumber());
    User user = User.createUser(task.id(), task.name(), task.phoneNumber(), task.address());
    repository.save(user);
  }

  @Transactional
  public User update(UUID id, UserUpdateRequest request) {
    User user = findById(id);
    user.updateName(request.name());
    validatePhoneNumber(request.phoneNumber());
    user.updatePhoneNumber(request.phoneNumber());
    user.updateAddress(request.address());
    return user;
  }

  public User find(UUID id) {
    return findById(id);
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
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", id)));
  }
}
