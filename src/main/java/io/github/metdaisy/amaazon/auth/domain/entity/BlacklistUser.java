package io.github.metdaisy.amaazon.auth.domain.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import io.github.metdaisy.amaazon.common.jpa.ImmutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "blacklist_users")
@Immutable
public class BlacklistUser extends ImmutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotNull
  @Column(name = "compromised_at", nullable = false)
  private Instant compromisedAt;

  private BlacklistUser(UUID userId, Instant compromisedAt) {
    this.userId = userId;
    this.compromisedAt = compromisedAt;
  }

  public static BlacklistUser of(UUID userId, Instant compromisedAt) {
    return new BlacklistUser(userId, compromisedAt);
  }
}
