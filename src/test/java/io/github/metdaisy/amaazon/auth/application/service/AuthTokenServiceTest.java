package io.github.metdaisy.amaazon.auth.application.service;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

  @Mock
  private RefreshTokenRepository repository;

  @Mock
  private AuthUserPort userPort;

  @Mock
  private JwtTokenProvider provider;

  @Mock
  private JwtTokenExpiration properties;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private AuthTokenService authTokenService;

  @Test
  @DisplayName("reissue_success")
  void reissue_success() {
    // given
    String token = "valid-refresh-token";
    String jti = "test-jti";
    UUID userId = UUID.randomUUID();
    AuthUserDto userDto = new AuthUserDto(userId, "USER", true);
    JwtLoginDto loginDto = new JwtLoginDto(userId, "new-access-token", "new-refresh-token");

    RefreshToken refreshToken = RefreshToken.of(userId, "device-1", jti, Instant.now().plusSeconds(3600));

    doNothing().when(provider).validate(token);
    given(provider.parseJti(token)).willReturn(jti);
    given(repository.findByToken(jti)).willReturn(Optional.of(refreshToken));
    given(userPort.loadUser(userId)).willReturn(Optional.of(userDto));
    given(provider.generateAccessToken(userId, "USER")).willReturn("new-access-token");
    given(provider.generateRefreshToken(userId)).willReturn("new-refresh-token");
    given(provider.parseJti("new-refresh-token")).willReturn("new-jti");
    given(properties.refreshExpiration()).willReturn(Duration.ofSeconds(3600));

    // when
    JwtLoginDto result = authTokenService.reissue(token);

    // then
    assertThat(result).isEqualTo(loginDto);
    assertThat(result.accessToken()).isEqualTo("new-access-token");
    assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    verify(repository).findByToken(jti);
  }

  @Test
  @DisplayName("reissue_failure_invalidToken")
  void reissue_failure_invalidToken() {
    // given
    String token = "invalid-refresh-token";
    doThrow(new AuthException(AuthErrorCode.TOKEN_EXPIRED, Map.of("token", token))).when(provider).validate(token);

    // when & then
    assertThatThrownBy(() -> authTokenService.reissue(token))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("만료된 토큰입니다");
  }

  @Test
  @DisplayName("create_success")
  void create_success() {
    // given
    UUID userId = UUID.randomUUID();
    AuthUserDto userDto = new AuthUserDto(userId, "USER", true);
    given(userPort.loadUser(userId)).willReturn(Optional.of(userDto));
    given(provider.generateAccessToken(userId, "USER")).willReturn("access-token");
    given(provider.generateRefreshToken(userId)).willReturn("refresh-token");
    given(provider.parseJti("refresh-token")).willReturn("jti");
    given(properties.refreshExpiration()).willReturn(Duration.ofSeconds(3600));

    // when
    JwtLoginDto result = authTokenService.create(userId, "device-1");

    // then
    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    verify(repository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("delete_success")
  void delete_success() {
    // given
    String token = "refresh-token";
    given(provider.parseJti(token)).willReturn("jti");

    // when
    authTokenService.delete(token);

    // then
    verify(repository).deleteByTokenDirectly("jti");
  }
}

