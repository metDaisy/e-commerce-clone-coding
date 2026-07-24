package io.github.metdaisy.amaazon.auth.infra.adapter;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort {

  private final UserQueryApi api;

  @Override
  public Optional<AuthUserDto> loadUser(UUID userId) {
    return api.findById(userId).map(dto -> new AuthUserDto(dto.id(), dto.role()));
  }
}
