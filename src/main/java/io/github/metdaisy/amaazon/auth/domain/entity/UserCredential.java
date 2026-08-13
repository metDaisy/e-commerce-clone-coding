package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_credentials")
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
public class UserCredential extends MutableEntity {

  @Size(max = 255)
  @NotNull
  @Column(name = "email", nullable = false)
  private String email;

  @Size(max = 255)
  @NotNull
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "violation_count", nullable = false)
  private int violationCount;

  @Column(name = "until_locked")
  private Instant untilLocked;

  @Builder(access = AccessLevel.PRIVATE)
  private UserCredential(String email, String passwordHash, int violationCount) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.violationCount = violationCount;
  }

  public static UserCredential of(String email, String password) {
    return UserCredential.builder()
        .email(email)
        .passwordHash(password)
        .violationCount(0)
        .build();
  }

  public void updatePassword(String password) {
    updateIfChanged(this.passwordHash, password, value -> this.passwordHash = value);
  }

  public void updateEmail(String email) {
    updateIfChanged(this.email, email, value -> this.email = value);
  }

  public void validatePassword(Predicate<String> validator) {
    if (!validator.test(passwordHash)) {
      throw new AuthException(AuthErrorCode.INCORRECT_PASSWORD);
    }
  }

  public boolean isLocked(int maxAttempt, Instant now) {
    if (untilLocked == null) {
      return false;
    }
    return violationCount >= maxAttempt && untilLocked.isAfter(now);
  }

  public void increaseViolationCount(int maxAttempt, Duration lockedDuration) {
    Instant now = Instant.now();
    if (isLocked(maxAttempt, now)) {
      return;
    }
    resetViolationOrNot(now);
    violationCount++;
    if (violationCount >= maxAttempt) {
      untilLocked = now.plus(lockedDuration);
    }
  }

  public void resetViolationOrNot(Instant now) {
    if (untilLocked != null && untilLocked.isBefore(now)) {
      violationCount = 0;
      untilLocked = null;
    }
  }
}
