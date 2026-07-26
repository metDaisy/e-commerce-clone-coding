package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.ImmutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "social_credentials")
@Immutable
public class SocialCredential extends ImmutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Size(max = 20)
  @NotNull
  @Column(name = "provider", nullable = false, length = 20)
  private String provider;

  @Size(max = 128)
  @NotNull
  @Column(name = "provider_id", nullable = false, length = 128)
  private String providerId;

  private SocialCredential(UUID userId, String provider, String providerId) {
    this.userId = userId;
    this.provider = provider;
    this.providerId = providerId;
  }

  public static SocialCredential of(UUID userId, String provider, String providerId) {
    return new SocialCredential(userId, provider, providerId);
  }

}
