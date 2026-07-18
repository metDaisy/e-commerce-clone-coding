package io.github.metdaisy.amaazon.auth.infra.adapter;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort {

  private final UserQueryApi api;

  @Override
  public AuthUserDto loadUser(UUID userId) {
    UserDto userDto = api.findById(userId);
    return new AuthUserDto(userId, userDto.role());
  }
}
