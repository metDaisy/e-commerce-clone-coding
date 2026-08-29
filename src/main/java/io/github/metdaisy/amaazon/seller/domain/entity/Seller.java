package io.github.metdaisy.amaazon.seller.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sellers")
public class Seller extends MutableEntity {

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Size(max = 255)
  @NotNull
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Size(max = 255)
  @NotNull
  @Column(name = "business_name", nullable = false)
  private String businessName;

  @Size(max = 128)
  @NotNull
  @Column(name = "business_registration_hash", nullable = false, length = 128)
  private String businessRegistrationHash;

  @Size(max = 255)
  @NotNull
  @Column(name = "contact_email", nullable = false)
  private String contactEmail;

  @Size(max = 20)
  @NotNull
  @Column(name = "contact_phone", nullable = false, length = 20)
  private String contactPhone;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SellerStatus status;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Size(max = 500)
  @Column(name = "review_reason", length = 500)
  private String reviewReason;

  public Seller(UUID userId, String displayName, String businessName,
      String businessRegistrationHash,
      String contactEmail, String contactPhone, SellerStatus status, UUID reviewedBy,
      Instant reviewedAt, String reviewReason) {
    this.userId = userId;
    this.displayName = displayName;
    this.businessName = businessName;
    this.businessRegistrationHash = businessRegistrationHash;
    this.contactEmail = contactEmail;
    this.contactPhone = contactPhone;
    this.status = status;
    this.reviewedBy = reviewedBy;
    this.reviewedAt = reviewedAt;
    this.reviewReason = reviewReason;
  }

}
