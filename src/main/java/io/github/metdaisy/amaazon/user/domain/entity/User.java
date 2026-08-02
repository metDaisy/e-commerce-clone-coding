package io.github.metdaisy.amaazon.user.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

  @Size(max = 20)
  @NotNull
  @Column(name = "role", nullable = false, length = 50)
  @Enumerated(value = EnumType.STRING)
  private UserRole role;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "point_balance", nullable = false)
  private int pointBalance;

  @Size(max = 100)
  @Column(name = "address", length = 100)
  private String address;

  @Column(name = "is_enabled")
  private boolean isEnabled;

  @Builder(access = AccessLevel.PRIVATE)
  private User(UUID id, String name, String phoneNumber, UserRole role, int pointBalance,
      String address, boolean isEnabled) {
    super(id);
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.role = role;
    this.pointBalance = pointBalance;
    this.address = address;
    this.isEnabled = isEnabled;
  }

  public static User createUser(UUID id, String name, String phoneNumber, String address) {
    return User.builder()
        .id(id)
        .name(name)
        .phoneNumber(phoneNumber)
        .role(UserRole.USER)
        .pointBalance(0)
        .address(address)
        .isEnabled(true)
        .build();
  }

  public void updateName(String name) {
    updateIfChanged(this.name, name, value -> this.name = value);
  }

  public void updatePhoneNumber(String phoneNumber) {
    updateIfChanged(this.phoneNumber, phoneNumber, value -> this.phoneNumber = value);
  }

  public void updateRole(UserRole role) {
    updateIfChanged(this.role, role, value -> this.role = value);
  }

  public void updateAddress(String address) {
    updateIfChanged(this.address, address, value -> this.address = value);
  }
}
