package io.github.metdaisy.amaazon.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class GuestTokenServiceTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @InjectMocks
  private GuestTokenService guestTokenService;

  @Test
  @DisplayName("create guest token")
  void create() {
    given(jwtTokenProvider.generateGuestToken("google", "123")).willReturn("guest-token");
    String result = guestTokenService.create("google", "123");
    assertThat(result).isEqualTo("guest-token");
  }

  @Test
  @DisplayName("getProvider")
  void getProvider() {
    given(jwtTokenProvider.parseProvider("guest-token", "provider")).willReturn("google");
    String result = guestTokenService.getProvider("guest-token");
    assertThat(result).isEqualTo("google");
  }

  @Test
  @DisplayName("getProviderId")
  void getProviderId() {
    given(jwtTokenProvider.parseProvider("guest-token", "providerId")).willReturn("123");
    String result = guestTokenService.getProviderId("guest-token");
    assertThat(result).isEqualTo("123");
  }

  @Test
  @DisplayName("validate")
  void validate() {
    guestTokenService.validate("guest-token");
    verify(jwtTokenProvider).validate("guest-token");
  }
}
