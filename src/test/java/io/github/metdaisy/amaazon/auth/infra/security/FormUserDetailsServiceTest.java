package io.github.metdaisy.amaazon.auth.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.exception.UserCredentialAuthenticationException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class FormUserDetailsServiceTest {

  @Mock
  private UserQueryApi userQueryApi;

  @Mock
  private UserCredentialRepository repository;

  @InjectMocks
  private FormUserDetailsService formUserDetailsService;

  @Test
  @DisplayName("loadUserByUsername success")
  void loadUserByUsername() {
    UserCredential credential = UserCredential.of("test@test.com", "password");
    given(repository.findByEmail("test@test.com")).willReturn(Optional.of(credential));
    UUID userId = credential.getId();
    UserDto userDto = new UserDto(userId, List.of("USER"), true);
    given(userQueryApi.findById(userId)).willReturn(Optional.of(userDto));

    UserDetails result = formUserDetailsService.loadUserByUsername("test@test.com");

    assertThat(result.getUsername()).isEqualTo(userId.toString());
    assertThat(result.getPassword()).isEqualTo("password");
    assertThat(result.getAuthorities())
        .anyMatch(a -> a.getAuthority().equals("ROLE_USER") || a.getAuthority().equals("USER"));
  }

  @Test
  @DisplayName("loadUserByUsername fails when email not found")
  void loadUserByUsername_emailNotFound() {
    given(repository.findByEmail("test@test.com")).willReturn(Optional.empty());

    assertThatThrownBy(() -> formUserDetailsService.loadUserByUsername("test@test.com"))
        .isInstanceOf(UserCredentialAuthenticationException.class);
  }

  @Test
  @DisplayName("loadUserByUsername fails when user not found")
  void loadUserByUsername_userNotFound() {
    UserCredential credential = UserCredential.of("test@test.com", "password");
    given(repository.findByEmail("test@test.com")).willReturn(Optional.of(credential));
    given(userQueryApi.findById(credential.getId())).willReturn(Optional.empty());

    assertThatThrownBy(() -> formUserDetailsService.loadUserByUsername("test@test.com"))
        .isInstanceOf(AuthException.class);
  }

  @Test
  @DisplayName("loadUserByUsername returns disabled user when account is deactivated")
  void loadUserByUsername_accountDeactivated() {
    UserCredential credential = UserCredential.of("test@test.com", "password");
    given(repository.findByEmail("test@test.com")).willReturn(Optional.of(credential));
    UUID userId = credential.getId();
    // isEnabled = false
    UserDto userDto = new UserDto(userId, List.of("USER"), false);
    given(userQueryApi.findById(userId)).willReturn(Optional.of(userDto));

    UserDetails result = formUserDetailsService.loadUserByUsername("test@test.com");

    assertThat(result.getUsername()).isEqualTo(userId.toString());
    assertThat(result.isEnabled()).isFalse();
  }
}

