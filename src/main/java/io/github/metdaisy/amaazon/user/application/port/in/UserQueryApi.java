package io.github.metdaisy.amaazon.user.application.port.in;

import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.mapper.UserApiMapper;
import io.github.metdaisy.amaazon.user.domain.exception.UserNotFoundException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("user-api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryApi {

  private final UserRepository repository;
  private final UserApiMapper mapper;

  public UserDto findById(UUID userId) {
    return repository.findById(userId).map(mapper::toDto)
            .orElseThrow(() -> new UserNotFoundException(userId));
  }
}
