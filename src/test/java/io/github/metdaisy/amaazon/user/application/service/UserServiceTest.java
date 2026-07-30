package io.github.metdaisy.amaazon.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.event.UserCreatedEvent;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @DisplayName("사용자 생성 - 성공: 유효한 이름과 전화번호로 신규 사용자를 생성한다_성공")
    @Test
    void create_user_success() {
        // given
        String name = "김철수";
        String phoneNumber = "01012345678";

        User mockUser = User.createUser(name, phoneNumber);
        given(userRepository.existsByName(name)).willReturn(false);
        given(userRepository.existsByPhoneNumber(phoneNumber)).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(mockUser);

        // when
        UserCreateRequest request = new UserCreateRequest(name, phoneNumber, "test@test.com", "password");
        User user = userService.create(request);

        // then
        assertThat(user).isEqualTo(mockUser);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        verify(eventPublisher, times(1)).publishEvent(any(UserCreatedEvent.class));
    }

    @DisplayName("사용자 생성 - 실패: 중복된 이름으로 인해 사용자 생성이 거부된다_실패")
    @Test
    void create_user_duplicate_name_fail() {
        // given
        String name = "김철수";
        String phoneNumber = "01012345678";

        given(userRepository.existsByName(name)).willReturn(true);

        // when & then
        UserCreateRequest request = new UserCreateRequest(name, phoneNumber, "test@test.com", "password");
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.NAME_ALREADY_EXISTS.getCode());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @DisplayName("사용자 생성 - 실패: 중복된 전화번호로 인해 사용자 생성이 거부된다_실패")
    @Test
    void create_user_duplicate_phone_number_fail() {
        // given
        String name = "김철수";
        String phoneNumber = "01012345678";

        given(userRepository.existsByName(name)).willReturn(false);
        given(userRepository.existsByPhoneNumber(phoneNumber)).willReturn(true);

        // when & then
        UserCreateRequest request = new UserCreateRequest(name, phoneNumber, "test@test.com", "password");
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.PHONE_ALREADY_EXISTS.getCode());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

