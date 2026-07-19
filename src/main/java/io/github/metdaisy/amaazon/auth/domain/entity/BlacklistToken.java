package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.ImmutableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "blacklist_tokens")
@Immutable
public class BlacklistToken extends ImmutableEntity {

  @Size(max = 36)
  @NotNull
  @Column(name = "token", nullable = false, length = 36)
  private String token;

  @NotNull
  @Column(name = "expired_at", nullable = false)
  private Instant expiredAt;

  private BlacklistToken(String token, Instant expiredAt) {
    this.token = token;
    this.expiredAt = expiredAt;
  }

  public static BlacklistToken of(String token, Instant expiredAt) {
    return new BlacklistToken(token, expiredAt);
  }
}
