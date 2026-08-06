package io.github.metdaisy.amaazon.auth.application.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

  private PasswordValidator passwordValidator;

  @Mock
  private ValidPassword validPassword;

  @Mock
  private ConstraintValidatorContext context;
  
  @Mock
  private ConstraintViolationBuilder builder;

  @BeforeEach
  void setUp() {
    passwordValidator = new PasswordValidator();
    given(validPassword.required()).willReturn(true);
    passwordValidator.initialize(validPassword);
  }

  @Test
  @DisplayName("isValid: null이고 required가 true이면 false 반환")
  void isValid_null() {
    boolean result = passwordValidator.isValid(null, context);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isValid: null이고 required가 false이면 true 반환")
  void isValid_null_notRequired() {
    given(validPassword.required()).willReturn(false);
    passwordValidator.initialize(validPassword);
    
    boolean result = passwordValidator.isValid(null, context);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("isValid: 공백이 포함되어 있으면 false 반환")
  void isValid_withSpace() {
    given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(builder);
    
    boolean result = passwordValidator.isValid("pass word1!", context);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isValid: 8자 미만이면 false 반환")
  void isValid_tooShort() {
    given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(builder);
    
    boolean result = passwordValidator.isValid("pass1!", context);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isValid: 문자/숫자/특수문자 중 2가지 이상 조합이 아니면 false 반환 (문자만)")
  void isValid_onlyAlphabet() {
    boolean result = passwordValidator.isValid("passwordpassword", context);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isValid: 문자/숫자/특수문자 중 2가지 조합이면 true 반환")
  void isValid_twoCombinations() {
    boolean result = passwordValidator.isValid("password1234", context);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("isValid: 문자/숫자/특수문자 중 3가지 조합이면 true 반환")
  void isValid_threeCombinations() {
    boolean result = passwordValidator.isValid("password1234!", context);
    assertThat(result).isTrue();
  }
}
