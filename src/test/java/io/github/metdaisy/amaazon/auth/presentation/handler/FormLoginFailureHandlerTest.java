package io.github.metdaisy.amaazon.auth.presentation.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FormLoginFailureHandlerTest {

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private FormLoginFailureHandler formLoginFailureHandler;

  @Test
  @DisplayName("onAuthenticationFailure_success")
  void onAuthenticationFailure_success() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new AuthenticationException("test") {};

    // when
    formLoginFailureHandler.onAuthenticationFailure(request, response, exception);

    // then
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
  }

  @Test
  @DisplayName("onAuthenticationFailure_failure")
  void onAuthenticationFailure_failure() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new AuthenticationException("test") {};

    // when
    formLoginFailureHandler.onAuthenticationFailure(request, response, exception);

    // then
    assertThat(response.getStatus()).isEqualTo(401);
  }
}
