package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.event.JwtTokenCompromisedEvent;
import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.global.security.jwt.config.JwtTokenExpiration;
import io.github.metdaisy.amaazon.global.security.jwt.provider.JwtTokenProvider;
import io.github.metdaisy.amaazon.user.application.dto.UserDto;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthTokenService {

  private final RefreshTokenRepository repository;
  private final UserQueryApi userQueryApi;
  private final JwtTokenProvider provider;
  private final JwtTokenExpiration jwtTokenExpiration;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public JwtLoginDto reissue(String token) {
    provider.validate(token);
    String jti = provider.parseJti(token);
    RefreshToken tokenEntity = repository.findByToken(jti)
        .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("refreshToken", token))));
    validateTokenEntity(tokenEntity, jti);
    UserDto userDto = userQueryApi.findById(tokenEntity.getUserId())
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("userId", tokenEntity.getUserId()))));
    return issueTokens(userDto, tokenEntity::reissue);
  }

  @Transactional
  public JwtLoginDto create(UUID userId, String device) {
    UserDto userDto = userQueryApi.findById(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of("userId", userId))));
    return issueTokens(userDto, (jti, expiredAt) -> {
      RefreshToken tokenEntity = RefreshToken.of(userId, device, jti, expiredAt);
      repository.save(tokenEntity);
    });
  }

  @Transactional
  public void delete(String token) {
    String jti = provider.parseJti(token);
    repository.deleteByTokenDirectly(jti);
  }

  private void validateTokenEntity(RefreshToken tokenEntity, String jti) {
    UUID userId = tokenEntity.getUserId();
    if (tokenEntity.isCompromised(jti)) {
      eventPublisher.publishEvent(new JwtTokenCompromisedEvent(userId, Instant.now()));
      throw new AuthException(AuthErrorCode.TOKEN_COMPROMISED,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId, "jti", jti, "device", tokenEntity.getDeviceId(),
              "reason", "TOKEN_COMPROMISED")));
    }
    if (!tokenEntity.isCurrentToken(jti)) {
      throw new AuthException(AuthErrorCode.TOKEN_EXPIRED,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId, "jti", jti, "device", tokenEntity.getDeviceId(),
              "reason", "TOKEN_EXPIRED")));
    }
  }

  private JwtLoginDto issueTokens(UserDto userDto, BiConsumer<String, Instant> tokenAction) {
    String accessToken = provider.generateAccessToken(userDto.id(), userDto.rolesCsv());
    String refreshToken = provider.generateRefreshToken(userDto.id());
    String jti = provider.parseJti(refreshToken);
    Instant expiredAt = Instant.now().plus(jwtTokenExpiration.refreshExpiration());
    tokenAction.accept(jti, expiredAt);
    return new JwtLoginDto(userDto.id(), userDto.roles(), accessToken, refreshToken);
  }
}
