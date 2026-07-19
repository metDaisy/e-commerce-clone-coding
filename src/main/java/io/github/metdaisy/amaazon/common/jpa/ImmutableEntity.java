package io.github.metdaisy.amaazon.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class ImmutableEntity implements Persistable<UUID> {

  @Id
  private UUID id;

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private Instant createdAt;

  protected ImmutableEntity() {
    this.id = UUID.randomUUID();
  }

  @Override
  public boolean isNew() {
    return createdAt == null;
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
    return Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    if (this instanceof HibernateProxy) {
      return ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode();
    }
    return Objects.hash(getClass(), getId());
  }
}
