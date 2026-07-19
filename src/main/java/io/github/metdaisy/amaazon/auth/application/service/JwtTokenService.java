package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
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
import java.util.function.BiConsumer;
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
    String jti = provider.parseJti(token);
    RefreshToken tokenEntity = repository.findByToken(jti)
            .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
                    Map.of("refreshToken", token)));
    validateTokenEntity(tokenEntity, jti);
    AuthUserDto userDto = userPort.loadUser(tokenEntity.getUserId());
    return issueTokens(userDto, tokenEntity::reissue);
  }

  public JwtLoginDto create(UUID userId, String device) {
    AuthUserDto userDto = userPort.loadUser(userId);
    return issueTokens(userDto, (jti, expiredAt) -> {
      RefreshToken tokenEntity = RefreshToken.of(userId, device, jti, expiredAt);
      repository.save(tokenEntity);
    });
  }

  public void delete(String token) {
    String jti = provider.parseJti(token);
    repository.deleteByTokenDirectly(jti);
  }

  private void validateTokenEntity(RefreshToken tokenEntity, String jti) {
    UUID userId = tokenEntity.getUserId();
    if (tokenEntity.isCompromised(jti)) {
      eventPublisher.publishEvent(new JwtTokenCompromisedEvent(userId, Instant.now()));
      throw new AuthException(AuthErrorCode.TOKEN_COMPROMISED,
              Map.of("userId", userId, "jti", jti, "device", tokenEntity.getDeviceId()));
    }
    if (!tokenEntity.isCurrentToken(jti)) {
      throw new AuthException(AuthErrorCode.TOKEN_EXPIRED,
              Map.of("userId", userId, "jti", jti, "device", tokenEntity.getDeviceId()));
    }
  }

  private JwtLoginDto issueTokens(AuthUserDto userDto, BiConsumer<String, Instant> tokenAction) {
    String accessToken = provider.generateAccessToken(userDto.userId(), userDto.role());
    String refreshToken = provider.generateRefreshToken(userDto.userId());
    String jti = provider.parseJti(refreshToken);
    Instant expiredAt = Instant.now().plusSeconds(properties.refreshTokenExpiration());
    tokenAction.accept(jti, expiredAt);
    return new JwtLoginDto(userDto.userId(), accessToken, refreshToken);
  }
}
