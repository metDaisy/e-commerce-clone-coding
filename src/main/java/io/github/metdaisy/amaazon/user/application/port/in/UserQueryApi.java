package io.github.metdaisy.amaazon.user.application.port.in;

import java.util.Map;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.mapper.UserApiMapper;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@NamedInterface("user-api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryApi {

  private final UserRepository repository;
  private final UserApiMapper mapper;

  public UserDto findById(UUID userId) {
    return repository.findById(userId).map(mapper::toDto).orElseThrow(
        () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
  }
}
