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
@Table(name = "user_credentials")
public class UserCredential extends MutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Size(max = 255)
  @NotNull
  @Column(name = "email", nullable = false)
  private String email;

  @Size(max = 255)
  @NotNull
  @Column(name = "password", nullable = false)
  private String password;

  @Size(max = 512)
  @NotNull
  @Column(name = "refresh_token", nullable = false)
  private String refreshToken;

  @Size(max = 512)
  @Column(name = "pre_refresh_token")
  private String preRefreshToken;

  @Builder
  private UserCredential(UUID userId, String email, String password, String refreshToken) {
    this.userId = userId;
    this.email = email;
    this.password = password;
    this.refreshToken = refreshToken;
  }

  public static UserCredential of(UUID userId, String email, String password, String refreshToken) {
    return UserCredential.builder()
            .userId(userId)
            .email(email)
            .password(password)
            .refreshToken(refreshToken)
            .build();
  }

  public void updatePassword(String password, Consumer<String> validator) {
    if (shouldUpdate(this.password, password, validator)) {
      this.password = password;
    }
  }

  public void updateRefreshToken(String refreshToken, Consumer<String> validator) {
    if (shouldUpdate(this.refreshToken, refreshToken, validator)) {
      this.preRefreshToken = this.refreshToken;
      this.refreshToken = refreshToken;
    }
  }
}
