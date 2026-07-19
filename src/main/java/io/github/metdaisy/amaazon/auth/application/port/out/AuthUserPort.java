package io.github.metdaisy.amaazon.auth.application.port.out;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import java.util.UUID;

public interface AuthUserPort {

  AuthUserDto loadUser(UUID userId);
}
