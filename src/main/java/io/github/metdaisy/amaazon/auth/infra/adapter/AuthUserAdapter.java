package io.github.metdaisy.amaazon.auth.infra.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort {

  private final UserQueryApi api;

  @Override
  public Optional<AuthUserDto> loadUser(UUID userId) {
    return api.findById(userId).map(dto -> new AuthUserDto(dto.id(), dto.role(), dto.isEnabled()));
  }

  @Override
  public boolean existsUser(UUID userId) {
    return api.existsByUserId(userId);
  }

}
