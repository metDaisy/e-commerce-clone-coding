package io.github.metdaisy.amaazon.user.application.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("비밀번호 검증기 테스트")
class PasswordValidatorTest {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConstraintValidatorContext context;

  private PasswordValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PasswordValidator();
  }

  @ParameterizedTest(name = "[{index}] 비밀번호={0}")
  @ValueSource(strings = {"Password1", "abcdefgh!", "12345678!"})
  @DisplayName("비밀번호 검증 성공: 8자 이상이며 문자 유형을 두 종류 이상 포함한다")
  void isValid_success(String password) {
    // when
    boolean result = validator.isValid(password, context);

    // then
    assertThat(result).isTrue();
  }

  @ParameterizedTest(name = "[{index}] 비밀번호={0}")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @DisplayName("비밀번호 검증 실패: 필수 비밀번호가 비어 있다")
  void isValid_failure_whenRequiredPasswordHasNoText(String password) {
    // when
    boolean result = validator.isValid(password, context);

    // then
    assertThat(result).isFalse();
  }

  @ParameterizedTest(name = "[{index}] 비밀번호={0}")
  @ValueSource(strings = {"Pass 123!", "Pass1!", "abcdefgh", "12345678", "!!!!!!!!"})
  @DisplayName("비밀번호 검증 실패: 공백, 길이, 문자 조합 조건을 충족하지 않는다")
  void isValid_failure_whenPasswordPolicyIsViolated(String password) {
    // when
    boolean result = validator.isValid(password, context);

    // then
    assertThat(result).isFalse();
  }

  @ParameterizedTest(name = "[{index}] 비밀번호={0}")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @DisplayName("선택 비밀번호 검증 성공: 비밀번호가 비어 있어도 허용한다")
  void isValid_success_whenOptionalPasswordHasNoText(String password) throws Exception {
    // given
    Field field = OptionalPassword.class.getDeclaredField("value");
    validator.initialize(field.getAnnotation(ValidPassword.class));

    // when
    boolean result = validator.isValid(password, context);

    // then
    assertThat(result).isTrue();
  }

  private static class OptionalPassword {

    @ValidPassword(required = false)
    private String value;
  }
}
