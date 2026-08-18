package io.github.metdaisy.amaazon.user.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.mapper.UserApiMapper;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 조회 API 테스트")
class UserQueryApiTest {

  @Mock
  private UserRepository repository;

  @Mock
  private UserApiMapper mapper;

  @InjectMocks
  private UserQueryApi userQueryApi;

  @Test
  @DisplayName("사용자 조회 성공: 사용자와 DTO 매핑 결과를 반환한다")
  void findById() {
    UUID userId = UUID.randomUUID();
    User user = User.createUser(UUID.randomUUID(), "tester", "01012345678");
    UserDto userDto = new UserDto(userId, List.of("USER"), true);

    given(repository.findWithRolesById(userId)).willReturn(Optional.of(user));
    given(mapper.toDto(user)).willReturn(userDto);

    Optional<UserDto> result = userQueryApi.findById(userId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(userId);
  }

  @Test
  @DisplayName("사용자 조회 실패: 사용자가 없으면 빈 Optional을 반환한다")
  void findById_failure_whenUserDoesNotExist() {
    UUID userId = UUID.randomUUID();
    given(repository.findWithRolesById(userId)).willReturn(Optional.empty());

    Optional<UserDto> result = userQueryApi.findById(userId);

    assertThat(result).isEmpty();
  }

  @ParameterizedTest(name = "[{index}] 저장소 조회 결과={0}")
  @ValueSource(booleans = {true, false})
  @DisplayName("활성 사용자 존재 여부 조회: 저장소 결과를 그대로 반환한다")
  void existsEnabledUser(boolean expected) {
    UUID userId = UUID.randomUUID();
    given(repository.existsByIdAndIsEnabledTrue(userId)).willReturn(expected);

    boolean exists = userQueryApi.existsEnabledUser(userId);
    assertThat(exists).isEqualTo(expected);
  }

  @Test
  @DisplayName("활성 사용자 요구 실패: 비활성 User면 USER_DISABLED 예외를 던진다")
  void requireEnabled_throwsWhenUserIsDisabled() {
    UUID userId = UUID.randomUUID();
    given(repository.existsByIdAndIsEnabledTrue(userId)).willReturn(false);

    assertThatThrownBy(() -> userQueryApi.requireEnabled(userId))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", "USER-004");
  }
}

