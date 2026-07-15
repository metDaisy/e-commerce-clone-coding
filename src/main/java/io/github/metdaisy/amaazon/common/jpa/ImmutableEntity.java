package io.github.metdaisy.amaazon.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class ImmutableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "external_id", unique = true, nullable = false)
  private UUID externalId;

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private Instant createdAt;

  @PrePersist
  protected void prePersist() {
    if (this.externalId == null) {
      this.externalId = UUID.randomUUID();
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }

    Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();

    if (thisEffectiveClass != oEffectiveClass) {
      return false;
    }

    ImmutableEntity that = (ImmutableEntity) o;
    return Objects.equals(getId(), that.getId()) &&
            Objects.equals(externalId, that.getExternalId());
  }

  @Override
  public final int hashCode() {
    if (this instanceof HibernateProxy) {
      return ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode();
    }
    return Objects.hash(getClass(), getId(), getExternalId());
  }

  @Override
  public final String toString() {
    return "ImmutableEntity{" +
            "id=" + id +
            ", externalId='" + externalId + '\'' +
            ", createdAt=" + createdAt +
            '}';
  }
}

