package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.function.Consumer;
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

  @Builder(access = AccessLevel.PRIVATE)
  private RefreshToken(UUID userId, String deviceId, String token) {
    this.userId = userId;
    this.deviceId = deviceId;
    this.token = token;
  }

  public static RefreshToken of(UUID userId, String deviceId, String token) {
    return RefreshToken.builder()
            .userId(userId)
            .deviceId(deviceId)
            .token(token)
            .build();
  }

  public void reissue(String token, Consumer<String> validator) {
    if (shouldUpdate(this.token, token, validator)) {
      this.preToken = this.token;
      this.token = token;
    }
  }
}
