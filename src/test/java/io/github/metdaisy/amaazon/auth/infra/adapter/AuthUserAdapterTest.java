package io.github.metdaisy.amaazon.auth.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUserAdapterTest {

  @Mock
  private UserQueryApi userQueryApi;

  @InjectMocks
  private AuthUserAdapter authUserAdapter;

  @Test
  @DisplayName("loadUser_success")
  void loadUser_success() {
    // given
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(userId, "USER", true);
    given(userQueryApi.findById(userId)).willReturn(Optional.of(userDto));

    // when
    var result = authUserAdapter.loadUser(userId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(userId);
    assertThat(result.get().role()).isEqualTo("USER");
    assertThat(result.get().isEnabled()).isTrue();
  }

  @Test
  @DisplayName("loadUser_failure_mappingError")
  void loadUser_failure_mappingError() {
    // given
    UUID userId = UUID.randomUUID();
    given(userQueryApi.findById(userId)).willReturn(Optional.empty());

    // when
    var result = authUserAdapter.loadUser(userId);

    // then
    assertThat(result).isEmpty();
  }
}

