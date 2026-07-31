package io.github.metdaisy.amaazon.user.application.validator;

import java.util.regex.Pattern;
import org.flywaydb.core.internal.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

  private static final Pattern ALPHABET_PATTERN = Pattern.compile("[a-zA-Z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
  private static final int MIN_LENGTH = 8;

  private boolean required = true;

  @Override
  public void initialize(ValidPassword constraintAnnotation) {
    this.required = constraintAnnotation.required();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (!StringUtils.hasText(value)) {
      return !required;
    }

    if (value.contains(" ")) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("비밀번호에 공백이 포함되어있습니다.").addConstraintViolation();
      return false;
    }

    if (value.length() < MIN_LENGTH) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("비밀번호는 최소 8자 이상이어야 합니다.").addConstraintViolation();
      return false;
    }

    int typeCount = 0;
    if (hasPattern(value, ALPHABET_PATTERN))
      typeCount++;
    if (hasPattern(value, DIGIT_PATTERN))
      typeCount++;
    if (hasPattern(value, SPECIAL_CHAR_PATTERN))
      typeCount++;

    return typeCount >= 2;
  }

  private boolean hasPattern(String value, Pattern pattern) {
    return pattern.matcher(value).find();
  }
}
