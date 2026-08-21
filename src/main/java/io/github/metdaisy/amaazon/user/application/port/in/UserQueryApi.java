package io.github.metdaisy.amaazon.user.application.port.in;

import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.mapper.UserApiMapper;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@NamedInterface("api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryApi {

  private final UserRepository repository;
  private final UserApiMapper mapper;

  public Optional<UserDto> findById(UUID userId) {
    return repository.findWithRolesById(userId).map(mapper::toDto);
  }

  public boolean existsEnabledUser(UUID userId) {
    return repository.existsByIdAndIsEnabledTrue(userId);
  }

  public void requireEnabled(UUID userId) {
    if (!existsEnabledUser(userId)) {
      throw new UserException(UserErrorCode.USER_DISABLED,
          AmaazonExceptionContext.logDetails(Map.of("userId", userId)));
    }
  }
}
