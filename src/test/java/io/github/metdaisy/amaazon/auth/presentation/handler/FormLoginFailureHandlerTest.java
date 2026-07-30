package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
    assertThat(response.getContentAsString()).contains("login failed");
  }

  @Test
  @DisplayName("onAuthenticationFailure_failure (serialization error)")
  void onAuthenticationFailure_failure() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new AuthenticationException("test") {};
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
}
