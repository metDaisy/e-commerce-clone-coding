package io.github.metdaisy.amaazon.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.event.FormSignUpTask;
import io.github.metdaisy.amaazon.user.application.event.UserDeactivatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 서비스 테스트")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("사용자 생성 성공: 신규 전화번호로 사용자를 저장한다")
  void create_success() {
    // given
    FormSignUpTask task = createTask("01012345678");
    given(userRepository.existsByPhoneNumber(task.phoneNumber())).willReturn(false);

    // when
    userService.create(task);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    then(userRepository).should().save(userCaptor.capture());
    assertThat(userCaptor.getValue())
        .extracting(User::getId, User::getName, User::getPhoneNumber)
        .containsExactly(task.id(), task.name(), task.phoneNumber());
  }

  @ParameterizedTest(name = "[{index}] 전화번호={0}")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @DisplayName("사용자 생성 성공: 전화번호가 없으면 중복 조회를 생략하고 저장한다")
  void create_success_whenPhoneNumberHasNoText(String phoneNumber) {
    // given
    FormSignUpTask task = createTask(phoneNumber);

    // when
    userService.create(task);

    // then
    then(userRepository).should(never()).existsByPhoneNumber(any());
    then(userRepository).should().save(any(User.class));
  }

  @Test
  @DisplayName("사용자 생성 실패: 중복 전화번호면 예외를 던지고 저장하지 않는다")
  void create_failure_whenPhoneNumberAlreadyExists() {
    // given
    FormSignUpTask task = createTask("01012345678");
    given(userRepository.existsByPhoneNumber(task.phoneNumber())).willReturn(true);

    // when
    Throwable thrown = catchThrowable(() -> userService.create(task));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("사용자 수정 성공: 사용자 정보를 변경하고 변경된 사용자를 반환한다")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser(userId, "기존이름", "01011112222");
    UserUpdateRequest request = new UserUpdateRequest("변경이름", "01033334444");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(false);

    // when
    User result = userService.update(userId, request);

    // then
    assertThat(result).isSameAs(user);
    assertThat(result)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly(request.name(), request.phoneNumber());
    then(userRepository).should().findById(userId);
    then(userRepository).should().existsByPhoneNumber(request.phoneNumber());
  }

  @Test
  @DisplayName("사용자 수정 실패: 존재하지 않는 사용자면 예외를 던진다")
  void update_failure_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest(null, null);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(() -> userService.update(userId, request));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("사용자 수정 실패: 중복 전화번호면 예외를 던진다")
  void update_failure_whenPhoneNumberAlreadyExists() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser(userId, "기존이름", "01011112222");
    UserUpdateRequest request = new UserUpdateRequest(null, "01033334444");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(true);

    // when
    Throwable thrown = catchThrowable(() -> userService.update(userId, request));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
  }

  @Test
  @DisplayName("계정 비활성화 성공: 활성 사용자를 비활성화하고 이벤트를 발행한다")
  void deactivate_success_whenUserIsEnabled() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser(userId, "tester", "01011112222");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    userService.deactivate(userId);

    // then
    assertThat(user.isEnabled()).isFalse();
    ArgumentCaptor<UserDeactivatedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserDeactivatedEvent.class);
    then(eventPublisher).should().publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    assertThat(eventCaptor.getValue().eventId()).isNotNull();
    assertThat(eventCaptor.getValue().deactivatedAt()).isNotNull();
  }

  @Test
  @DisplayName("계정 비활성화 실패: 이미 비활성화된 사용자면 USER-010 예외를 던지고 이벤트를 발행하지 않는다")
  void deactivate_failure_whenUserIsAlreadyDisabled() {
    // given
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    given(user.isEnabled()).willReturn(false);
    willThrow(new UserException(UserErrorCode.USER_ALREADY_DISABLED)).given(user).deactivate();
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    Throwable thrown = catchThrowable(() -> userService.deactivate(userId));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_ALREADY_DISABLED.getCode());
    then(user).should().deactivate();
    then(eventPublisher).should(never()).publishEvent(any(UserDeactivatedEvent.class));
  }

  @Test
  @DisplayName("계정 비활성화 실패: 존재하지 않는 사용자면 USER-001 예외를 던진다")
  void deactivate_failure_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(() -> userService.deactivate(userId));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND.getCode());
  }

  private FormSignUpTask createTask(String phoneNumber) {
    return new FormSignUpTask(UUID.randomUUID(), "홍길동", phoneNumber);
  }
}
