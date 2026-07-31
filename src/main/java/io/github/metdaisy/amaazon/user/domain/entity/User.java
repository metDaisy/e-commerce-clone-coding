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
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
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

  @Builder(access = AccessLevel.PRIVATE)
  private User(String name, String phoneNumber, UserRole role, int pointBalance, String address) {
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.role = role;
    this.pointBalance = pointBalance;
    this.address = address;
  }

  public static User createUser(String name, String phoneNumber, String address) {
    return User.builder()
        .name(name)
        .phoneNumber(phoneNumber)
        .role(UserRole.USER)
        .pointBalance(0)
        .address(address)
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
