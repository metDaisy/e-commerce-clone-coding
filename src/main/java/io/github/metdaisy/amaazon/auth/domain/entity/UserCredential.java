package io.github.metdaisy.amaazon.auth.domain.entity;

import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_credentials")
public class UserCredential extends MutableEntity {

  @Size(max = 255)
  @NotNull
  @Column(name = "email", nullable = false)
  private String email;

  @Size(max = 255)
  @NotNull
  @Column(name = "password", nullable = false)
  private String password;

  @Builder(access = AccessLevel.PRIVATE)
  private UserCredential(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public static UserCredential of(String email, String password) {
    return UserCredential.builder()
        .email(email)
        .password(password)
        .build();
  }

  public void updatePassword(String password) {
    updateIfChanged(this.password, password, value -> this.password = value);
  }

  public void updateEmail(String email) {
    updateIfChanged(this.email, email, value -> this.email = value);
  }

  public void matchPassword(String password) {
    if (!this.password.equals(password)) {
      throw new AuthException(AuthErrorCode.INCORRECT_PASSWORD);
    }
  }
}
