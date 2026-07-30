package io.github.metdaisy.amaazon.user.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.mapper.UserApiMapper;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserQueryApiTest {

  @Mock
  private UserRepository repository;

  @Mock
  private UserApiMapper mapper;

  @InjectMocks
  private UserQueryApi userQueryApi;

  @Test
  @DisplayName("findById")
  void findById() {
    UUID userId = UUID.randomUUID();
    User user = User.createUser("tester", "01012345678");
    UserDto userDto = new UserDto(userId, "USER");
    
    given(repository.findById(userId)).willReturn(Optional.of(user));
    given(mapper.toDto(user)).willReturn(userDto);

    Optional<UserDto> result = userQueryApi.findById(userId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(userId);
  }

  @Test
  @DisplayName("existsByUserId")
  void existsByUserId() {
    UUID userId = UUID.randomUUID();
    given(repository.existsById(userId)).willReturn(true);

    boolean exists = userQueryApi.existsByUserId(userId);
    assertThat(exists).isTrue();
  }
}
