package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthRefreshTokenNotFoundException;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtProperties;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

  public JwtLoginDto reissue(String token) {
    provider.validate(token);
    RefreshToken tokenEntity = repository.findByToken(token)
            .orElseThrow(() -> new AuthRefreshTokenNotFoundException(token));

    AuthUserDto userDto = userPort.loadUser(tokenEntity.getUserId());
    String accessToken = provider.generateAccessToken(userDto.userId(), userDto.role());
    String refreshToken = provider.generateRefreshToken(userDto.userId());
    Instant expiredAt = Instant.now().plusSeconds(properties.refreshTokenExpiration());
    tokenEntity.reissue(refreshToken, expiredAt);
    return new JwtLoginDto(userDto.userId(), accessToken, refreshToken);
  }

  public JwtLoginDto create(UUID userId, String device) {
    AuthUserDto userDto = userPort.loadUser(userId);
    String accessToken = provider.generateAccessToken(userDto.userId(), userDto.role());
    String refreshToken = provider.generateRefreshToken(userId);
    Instant expiredAt = Instant.now().plusSeconds(properties.refreshTokenExpiration());
    RefreshToken tokenEntity = RefreshToken.of(userId, device, refreshToken, expiredAt);
    repository.save(tokenEntity);
    return new JwtLoginDto(userId, accessToken, refreshToken);
  }
}
