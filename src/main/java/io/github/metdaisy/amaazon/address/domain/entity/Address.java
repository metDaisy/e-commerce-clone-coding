package io.github.metdaisy.amaazon.address.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "addresses")
public class Address extends MutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotNull
  @Size(max = 100)
  @Column(name = "recipient_name", nullable = false, length = 100)
  private String recipientName;

  @NotNull
  @Size(max = 20)
  @Column(name = "recipient_phone", nullable = false, length = 20)
  private String recipientPhone;

  @NotNull
  @Size(max = 20)
  @Column(name = "postal_code", nullable = false, length = 20)
  private String postalCode;

  @NotNull
  @Size(max = 255)
  @Column(name = "address_line", nullable = false, length = 255)
  private String addressLine;

  @NotNull
  @Column(name = "is_primary", nullable = false)
  private boolean isPrimary;

  private Address(UUID userId, String recipientName, String recipientPhone, String postalCode,
      String addressLine, boolean isPrimary) {
    this.userId = userId;
    this.recipientName = recipientName;
    this.recipientPhone = recipientPhone;
    this.postalCode = postalCode;
    this.addressLine = addressLine;
    this.isPrimary = isPrimary;
  }

  public static Address create(UUID userId, String recipientName, String recipientPhone,
      String postalCode, String addressLine, boolean isPrimary) {
    return new Address(userId, recipientName, recipientPhone, postalCode, addressLine, isPrimary);
  }

  public void makePrimary() {
    this.isPrimary = true;
  }

  public void updateRecipientName(String recipientName) {
    updateIfChanged(this.recipientName, recipientName, value -> this.recipientName = value);
  }

  public void updateRecipientPhone(String recipientPhone) {
    updateIfChanged(this.recipientPhone, recipientPhone, value -> this.recipientPhone = value);
  }

  public void updatePostalCode(String postalCode) {
    updateIfChanged(this.postalCode, postalCode, value -> this.postalCode = value);
  }

  public void updateAddressLine(String addressLine) {
    updateIfChanged(this.addressLine, addressLine, value -> this.addressLine = value);
  }
}
