package io.github.metdaisy.amaazon.user.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_enabled = false, updated_at = now() WHERE id = ?")
public class User extends MutableEntity {

  @Size(max = 10)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @Size(max = 11)
  @NotNull
  @Column(name = "phone_number", length = 11)
  private String phoneNumber;

  @NotNull
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role", nullable = false, length = 30)
  @Enumerated(value = EnumType.STRING)
  private Set<UserRole> roles = EnumSet.of(UserRole.USER);

  @NotNull
  @ColumnDefault("0")
  @Column(name = "point_balance", nullable = false)
  private int pointBalance;

  @Column(name = "is_enabled", nullable = false)
  private boolean isEnabled;

  @Builder(access = AccessLevel.PRIVATE)
  private User(UUID id, String name, String phoneNumber, Set<UserRole> roles, int pointBalance,
      boolean isEnabled) {
    super(id);
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.roles = roles;
    this.pointBalance = pointBalance;
    this.isEnabled = isEnabled;
  }

  public static User createUser(UUID id, String name, String phoneNumber) {
    return User.builder()
        .id(id)
        .name(name)
        .phoneNumber(phoneNumber)
        .roles(Set.of(UserRole.USER))
        .pointBalance(0)
        .isEnabled(true)
        .build();
  }

  public void updateName(String name) {
    updateIfChanged(this.name, name, value -> this.name = value);
  }

  public void updatePhoneNumber(String phoneNumber) {
    updateIfChanged(this.phoneNumber, phoneNumber, value -> this.phoneNumber = value);
  }

  public void updateRoles(Set<UserRole> roles) {
    if (roles == null || roles.isEmpty() || !roles.contains(UserRole.USER)) {
      throw new IllegalArgumentException("User roles must contain USER");
    }
    updateIfChanged(this.roles, roles, value -> this.roles = value);
  }

  public void deactivate() {
    if (!isEnabled) {
      throw new UserException(UserErrorCode.USER_ALREADY_DISABLED);
    }
    this.isEnabled = false;
  }

}
