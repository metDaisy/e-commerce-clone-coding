package io.github.metdaisy.amaazon.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.event.SignUpTask;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 서비스 테스트")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;


  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("사용자 생성 성공: 신규 전화번호로 사용자를 저장한다")
  void create_success() {
    // given
    SignUpTask task = createTask("01012345678");
    User savedUser = User.createUser(task.id(), task.name(), task.phoneNumber(), task.address());
    given(userRepository.existsByPhoneNumber(task.phoneNumber())).willReturn(false);
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    userService.create(task);

    // then
    verify(userRepository).save(any(User.class));
  }

  @ParameterizedTest(name = "[{index}] 전화번호={0}")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @DisplayName("사용자 생성 성공: 전화번호가 없으면 중복 조회를 생략한다")
  void create_success_whenPhoneNumberHasNoText(String phoneNumber) {
    // given
    SignUpTask task = createTask(phoneNumber);
    User savedUser = User.createUser(task.id(), task.name(), phoneNumber, task.address());
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    userService.create(task);

    // then
    verify(userRepository, never()).existsByPhoneNumber(any());
  }

  @ParameterizedTest(name = "[{index}] 존재하는 전화번호")
  @ValueSource(booleans = {true})
  @DisplayName("사용자 생성 실패: 중복 전화번호면 예외를 던진다")
  void create_failure_whenPhoneNumberAlreadyExists(boolean duplicateCount) {
    // given
    SignUpTask task = createTask("01012345678");
    given(userRepository.existsByPhoneNumber(task.phoneNumber())).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> userService.create(task))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("사용자 수정 성공: 사용자 정보를 변경한다")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser(userId, "기존이름", "01011112222", "기존주소");
    UserUpdateRequest request = new UserUpdateRequest(
        "변경이름", "01033334444", "변경주소");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(false);

    // when
    User result = userService.update(userId, request);

    // then
    assertThat(result).isSameAs(user);
    assertThat(result)
        .extracting(User::getName, User::getPhoneNumber, User::getAddress)
        .containsExactly(request.name(), request.phoneNumber(), request.address());
  }

  @Test
  @DisplayName("사용자 수정 실패: 존재하지 않는 사용자면 예외를 던진다")
  void update_failure_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest(null, null, null);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND.getCode());
  }

  @ParameterizedTest(name = "[{index}] 존재하는 전화번호")
  @ValueSource(booleans = {true})
  @DisplayName("사용자 수정 실패: 중복 전화번호면 예외를 던진다")
  void update_failure_whenPhoneNumberAlreadyExists(boolean duplicateCount) {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser(userId, "기존이름", "01011112222", "기존주소");
    UserUpdateRequest request = new UserUpdateRequest(null, "01033334444", null);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
  }

  private SignUpTask createTask(String phoneNumber) {
    return new SignUpTask(UUID.randomUUID(), "홍길동", phoneNumber, "서울시 강남구");
  }
}
