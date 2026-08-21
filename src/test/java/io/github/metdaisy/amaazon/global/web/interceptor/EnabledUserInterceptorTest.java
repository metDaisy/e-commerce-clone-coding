package io.github.metdaisy.amaazon.global.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
@DisplayName("활성 사용자 HandlerInterceptor 테스트")
class EnabledUserInterceptorTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock
  private UserQueryApi userQueryApi;

  @Mock
  private AmaazonPrincipal principal;

  private EnabledUserInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new EnabledUserInterceptor(userQueryApi);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("annotation이 붙은 Controller는 활성 사용자 검증을 통과한다")
  void preHandle_requiresEnabledUser() throws Exception {
    given(principal.getId()).willReturn(USER_ID);
    HandlerMethod handler = new HandlerMethod(new AnnotatedController(), "annotated");

    boolean result = interceptor.preHandle(null, null, handler);

    assertThat(result).isTrue();
    then(userQueryApi).should().requireEnabled(USER_ID);
  }

  @Test
  @DisplayName("annotation이 없는 Controller는 활성 사용자 검증을 수행하지 않는다")
  void preHandle_skipsUnannotatedController() throws Exception {
    HandlerMethod handler = new HandlerMethod(new AnnotatedController(), "unannotated");

    boolean result = interceptor.preHandle(null, null, handler);

    assertThat(result).isTrue();
    then(userQueryApi).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Controller class에 annotation이 붙으면 활성 사용자 검증을 수행한다")
  void preHandle_requiresEnabledUserForAnnotatedControllerClass() throws Exception {
    given(principal.getId()).willReturn(USER_ID);
    HandlerMethod handler = new HandlerMethod(new ClassAnnotatedController(), "annotatedByClass");

    boolean result = interceptor.preHandle(null, null, handler);

    assertThat(result).isTrue();
    then(userQueryApi).should().requireEnabled(USER_ID);
  }

  @Test
  @DisplayName("비활성 사용자면 UserException을 그대로 전달한다")
  void preHandle_propagatesDisabledUserException() throws Exception {
    given(principal.getId()).willReturn(USER_ID);
    willThrow(new UserException(UserErrorCode.USER_DISABLED))
        .given(userQueryApi).requireEnabled(USER_ID);
    HandlerMethod handler = new HandlerMethod(new AnnotatedController(), "annotated");

    assertThatThrownBy(() -> interceptor.preHandle(null, null, handler))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_DISABLED.getCode());
  }

  @Test
  @DisplayName("annotation이 붙었지만 인증 정보가 없으면 인증 예외를 던진다")
  void preHandle_throwsWhenAuthenticationIsMissing() throws Exception {
    SecurityContextHolder.clearContext();
    HandlerMethod handler = new HandlerMethod(new AnnotatedController(), "annotated");

    assertThatThrownBy(() -> interceptor.preHandle(null, null, handler))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @RequireEnabledUser
  public static class ClassAnnotatedController {

    public void annotatedByClass() {
    }
  }

  public static class AnnotatedController {

    @RequireEnabledUser
    public void annotated() {
    }

    public void unannotated() {
    }
  }
}
