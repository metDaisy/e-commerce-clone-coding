package io.github.metdaisy.amaazon.auth.application.port.out;

import java.util.Optional;
import java.util.UUID;
import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;

public interface AuthUserPort {

  Optional<AuthUserDto> loadUser(UUID userId);

  boolean existsUser(UUID userId);
}
