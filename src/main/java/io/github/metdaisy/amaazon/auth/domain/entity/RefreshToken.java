package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends MutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Size(max = 100)
  @NotNull
  @Column(name = "device_id", nullable = false, length = 100)
  private String deviceId;

  @Size(max = 512)
  @NotNull
  @Column(name = "token", nullable = false, length = 512)
  private String token;

  @Size(max = 512)
  @Column(name = "pre_token", length = 512)
  private String preToken;

  @NotNull
  @Column(name = "expired_at")
  private Instant expiredAt;

  @Builder(access = AccessLevel.PRIVATE)
  private RefreshToken(UUID userId, String deviceId, String token, Instant expiredAt) {
    this.userId = userId;
    this.deviceId = deviceId;
    this.token = token;
    this.expiredAt = expiredAt;
  }

  public static RefreshToken of(UUID userId, String deviceId, String token, Instant expiredAt) {
    return RefreshToken.builder()
            .userId(userId)
            .deviceId(deviceId)
            .token(token)
            .expiredAt(expiredAt)
            .build();
  }

  public void reissue(String token, Instant expiredAt) {
    updateIfChanged(this.token, token, value -> this.preToken = this.token);
    this.token = token;
    this.expiredAt = expiredAt;
  }

  public boolean isExpired(Instant now) {
    return this.expiredAt.isBefore(now);
  }

  public boolean isCompromised(String token) {
    return Objects.equals(this.preToken, token);
  }

  public boolean isCurrentToken(String token) {
    return this.token.equals(token);
  }
}
