package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.application.service.GuestTokenService;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class SocialLoginSuccessHandlerTest {

  @Mock
  private AuthTokenService authTokenService;
  @Mock
  private AuthCookieProvider authCookieProvider;
  @Mock
  private GuestTokenService guestTokenService;
  @Mock
  private Authentication authentication;

  @InjectMocks
  private SocialLoginSuccessHandler handler;

  @Test
  @DisplayName("onAuthenticationSuccess: guest user")
  void onAuthenticationSuccess_guest() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    Collection<? extends GrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_GUEST"));
    doReturn(authorities).when(authentication).getAuthorities();

    OAuth2User oauth2User = new DefaultOAuth2User(authorities,
        Map.of("provider", "google", "providerId", "123"), "provider");
    given(authentication.getPrincipal()).willReturn(oauth2User);
    given(guestTokenService.create("google", "123")).willReturn("guest-token");
    given(authCookieProvider.createGuestTokenCookie("guest-token"))
        .willReturn(
            ResponseCookie.from(AuthWebConstants.COOKIE_GUEST_TOKEN, "guest-token").build());

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    assertThat(response.getRedirectedUrl()).isEqualTo(AuthWebConstants.GUEST_REDIRECT_URL);
  }

  @Test
  @DisplayName("onAuthenticationSuccess: normal user")
  void onAuthenticationSuccess_normal() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(AuthWebConstants.COOKIE_DEVICE_ID, "device-123"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    Collection<? extends GrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_USER"));
    doReturn(authorities).when(authentication).getAuthorities();

    UUID userId = UUID.randomUUID();
    given(authentication.getName()).willReturn(userId.toString());

    JwtLoginDto loginDto = new JwtLoginDto(userId, "access", "refresh");
    given(authTokenService.create(userId, "device-123")).willReturn(loginDto);
    given(authCookieProvider.createRefreshTokenCookie("refresh"))
        .willReturn(ResponseCookie.from(AuthWebConstants.REFRESH_TOKEN, "refresh").build());
    given(authCookieProvider.createDeleteDeviceIdCookie())
        .willReturn(ResponseCookie.from(AuthWebConstants.COOKIE_DEVICE_ID, "").build());

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    assertThat(response.getRedirectedUrl()).isEqualTo(AuthWebConstants.DEFAULT_SUCCESS_URL);
  }

  @Test
  @DisplayName("onAuthenticationSuccess: normal user without device id throws exception")
  void onAuthenticationSuccess_noDeviceId() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    Collection<? extends GrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_USER"));
    doReturn(authorities).when(authentication).getAuthorities();
    given(authentication.getName()).willReturn(UUID.randomUUID().toString());

    // when & then
    assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
        .isInstanceOf(AuthException.class);
  }
}
