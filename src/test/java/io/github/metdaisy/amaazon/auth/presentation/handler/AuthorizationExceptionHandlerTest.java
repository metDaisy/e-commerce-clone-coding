package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationExceptionHandler 테스트")
class AuthorizationExceptionHandlerTest {

  @Mock
  private HandlerExceptionResolver resolver;

  @InjectMocks
  private AuthorizationExceptionHandler handler;

  @Test
  @DisplayName("handle - 권한 없는 리소스 접근 시 resolver.resolveException을 호출한다")
  void handle_success() throws Exception {
    // given
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    AccessDeniedException exception = new AccessDeniedException("Access Denied");

    // when
    handler.handle(request, response, exception);

    // then
    verify(resolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
  }
}
