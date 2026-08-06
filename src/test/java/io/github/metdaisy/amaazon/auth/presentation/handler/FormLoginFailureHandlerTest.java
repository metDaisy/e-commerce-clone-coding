package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.event.IncorrectPasswordEvent;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class FormLoginFailureHandlerTest {

  @Spy
  private ObjectMapper objectMapper;

  @InjectMocks
  private FormLoginFailureHandler formLoginFailureHandler;

  @Test
  @DisplayName("onAuthenticationFailure_success")
  void onAuthenticationFailure_success() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new AuthenticationException("test") {
    };

    // when
    formLoginFailureHandler.onAuthenticationFailure(request, response, exception);

    // then
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getContentAsString()).contains("로그인 실패하였습니다.");
  }

  @Test
  @DisplayName("onAuthenticationFailure_failure (serialization error)")
  void onAuthenticationFailure_failure() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new AuthenticationException("test") {
    };
    willThrow(new IOException("serialize error"))
        .given(objectMapper).writeValue(any(Writer.class), any());

    // when & then
    assertThatThrownBy(() ->
        formLoginFailureHandler.onAuthenticationFailure(request, response, exception))
        .isInstanceOf(IOException.class)
        .hasMessage("serialize error");

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
  }
  @Mock
  private org.springframework.context.ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("onAuthenticationFailure_badCredentials")
  void onAuthenticationFailure_badCredentials() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("username", "test@example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();
    BadCredentialsException exception = new BadCredentialsException("bad");

    // when
    formLoginFailureHandler.onAuthenticationFailure(request, response, exception);

    // then
    verify(eventPublisher).publishEvent(any(IncorrectPasswordEvent.class));
    assertThat(response.getStatus()).isEqualTo(401);
  }
}
