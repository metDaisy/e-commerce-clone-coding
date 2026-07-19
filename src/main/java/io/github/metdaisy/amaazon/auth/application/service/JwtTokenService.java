package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCreatedEvent;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenExpiredEvent;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenReissuedEvent;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class JwtTokenService {

  private final RefreshTokenRepository repository;
  private final AuthUserPort userPort;
  private final JwtTokenProvider provider;
  private final JwtProperties properties;
  private final ApplicationEventPublisher eventPublisher;

  public JwtLoginDto reissue(String token) {
    provider.validate(token);
    RefreshToken tokenEntity = repository.findByToken(token)
            .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
                    Map.of("refreshToken", token)));
    UUID userId = tokenEntity.getUserId();
    if (tokenEntity.isCompromised(token)) {
      eventPublisher.publishEvent(new JwtTokenCompromisedEvent(userId));
      throw new AuthException(AuthErrorCode.TOKEN_COMPROMISED,
              Map.of("userId", userId, "refreshToken", token, "device", tokenEntity.getDeviceId()));
    }
    if (!tokenEntity.isCurrentToken(token)) {
      eventPublisher.publishEvent(new JwtTokenExpiredEvent(token));
      throw new AuthException(AuthErrorCode.TOKEN_EXPIRED,
              Map.of("userId", userId, "refreshToken", token, "device", tokenEntity.getDeviceId()));
    }
    AuthUserDto userDto = userPort.loadUser(userId);
    String accessToken = provider.generateAccessToken(userDto.userId(), userDto.role());
    String refreshToken = provider.generateRefreshToken(userDto.userId());
    Instant expiredAt = Instant.now().plusSeconds(properties.refreshTokenExpiration());
    tokenEntity.reissue(refreshToken, expiredAt);
    eventPublisher.publishEvent(new JwtTokenReissuedEvent(userId, refreshToken));
    return new JwtLoginDto(userDto.userId(), accessToken, refreshToken);
  }

  public JwtLoginDto create(UUID userId, String device) {
    AuthUserDto userDto = userPort.loadUser(userId);
    String accessToken = provider.generateAccessToken(userDto.userId(), userDto.role());
    String refreshToken = provider.generateRefreshToken(userId);
    Instant expiredAt = Instant.now().plusSeconds(properties.refreshTokenExpiration());
    RefreshToken tokenEntity = RefreshToken.of(userId, device, refreshToken, expiredAt);
    repository.save(tokenEntity);
    eventPublisher.publishEvent(new JwtTokenCreatedEvent(userId, refreshToken));
    return new JwtLoginDto(userId, accessToken, refreshToken);
  }
}
