package io.github.metdaisy.amaazon.auth.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;

@ExtendWith(MockitoExtension.class)
class FormUserDetailsServiceTest {

  @Mock
  private AuthUserPort userPort;

  @Mock
  private UserCredentialRepository repository;

  @InjectMocks
  private FormUserDetailsService formUserDetailsService;

  @Test
  @DisplayName("loadUserByUsername success")
  void loadUserByUsername() {
    UUID userId = UUID.randomUUID();
    UserCredential credential = UserCredential.of(userId, "test@test.com", "password");
    given(repository.findByEmail("test@test.com")).willReturn(Optional.of(credential));
    
    AuthUserDto userDto = new AuthUserDto(userId, "USER");
    given(userPort.loadUser(userId)).willReturn(Optional.of(userDto));

    UserDetails result = formUserDetailsService.loadUserByUsername("test@test.com");
    
    assertEquals(userId.toString(), result.getUsername());
    assertEquals("password", result.getPassword());
  }

  @Test
  @DisplayName("loadUserByUsername fails when email not found")
  void loadUserByUsername_emailNotFound() {
    given(repository.findByEmail("test@test.com")).willReturn(Optional.empty());

    assertThrows(AuthException.class, () -> formUserDetailsService.loadUserByUsername("test@test.com"));
  }

  @Test
  @DisplayName("loadUserByUsername fails when user not found")
  void loadUserByUsername_userNotFound() {
    UUID userId = UUID.randomUUID();
    UserCredential credential = UserCredential.of(userId, "test@test.com", "password");
    given(repository.findByEmail("test@test.com")).willReturn(Optional.of(credential));
    given(userPort.loadUser(userId)).willReturn(Optional.empty());

    assertThrows(AuthException.class, () -> formUserDetailsService.loadUserByUsername("test@test.com"));
  }
}
