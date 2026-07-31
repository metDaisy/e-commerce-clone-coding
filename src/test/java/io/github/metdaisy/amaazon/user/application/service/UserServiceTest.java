package io.github.metdaisy.amaazon.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.event.UserUpdatedEvent;
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
  @DisplayName("사용자 생성 성공: 신규 전화번호로 사용자를 저장하고 생성 이벤트를 발행한다")
  void create_success() {
    // given
    UserCreateRequest request = createRequest("01012345678");
    User savedUser = User.createUser(request.name(), request.phoneNumber(), request.address());
    given(userRepository.countByPhoneNumber(request.phoneNumber())).willReturn(0);
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    User result = userService.create(request);

    // then
    assertThat(result).isSameAs(savedUser);
    ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue())
        .extracting(UserCreatedEvent::email, UserCreatedEvent::password)
        .containsExactly(request.email(), request.password());
  }

  @ParameterizedTest(name = "[{index}] 전화번호={0}")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @DisplayName("사용자 생성 성공: 전화번호가 없으면 중복 조회를 생략한다")
  void create_success_whenPhoneNumberHasNoText(String phoneNumber) {
    // given
    UserCreateRequest request = createRequest(phoneNumber);
    User savedUser = User.createUser(request.name(), phoneNumber, request.address());
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    User result = userService.create(request);

    // then
    assertThat(result).isSameAs(savedUser);
    verify(userRepository, never()).countByPhoneNumber(any());
  }

  @ParameterizedTest(name = "[{index}] 중복 개수={0}")
  @ValueSource(ints = {1, 2})
  @DisplayName("사용자 생성 실패: 중복 전화번호면 예외를 던진다")
  void create_failure_whenPhoneNumberAlreadyExists(int duplicateCount) {
    // given
    UserCreateRequest request = createRequest("01012345678");
    given(userRepository.countByPhoneNumber(request.phoneNumber())).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> userService.create(request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("사용자 수정 성공: 사용자 정보를 변경하고 수정 이벤트를 발행한다")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser("기존이름", "01011112222", "기존주소");
    UserUpdateRequest request = new UserUpdateRequest(
        "변경이름", "changed@example.com", "Password1!", "01033334444", "변경주소");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.countByPhoneNumber(request.phoneNumber())).willReturn(0);

    // when
    User result = userService.update(userId, request);

    // then
    assertThat(result).isSameAs(user);
    assertThat(result)
        .extracting(User::getName, User::getPhoneNumber, User::getAddress)
        .containsExactly(request.name(), request.phoneNumber(), request.address());
    verify(eventPublisher).publishEvent(
        new UserUpdatedEvent(userId, request.email(), request.password()));
  }

  @Test
  @DisplayName("사용자 수정 실패: 존재하지 않는 사용자면 예외를 던진다")
  void update_failure_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND.getCode());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @ParameterizedTest(name = "[{index}] 중복 개수={0}")
  @ValueSource(ints = {1, 2})
  @DisplayName("사용자 수정 실패: 중복 전화번호면 예외를 던진다")
  void update_failure_whenPhoneNumberAlreadyExists(int duplicateCount) {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser("기존이름", "01011112222", "기존주소");
    UserUpdateRequest request = new UserUpdateRequest(null, null, null, "01033334444", null);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.countByPhoneNumber(request.phoneNumber())).willReturn(duplicateCount);

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
    verify(eventPublisher, never()).publishEvent(any());
  }

  private UserCreateRequest createRequest(String phoneNumber) {
    return new UserCreateRequest(
        "홍길동", "user@example.com", "Password1!", phoneNumber, "서울시 강남구");
  }
}
