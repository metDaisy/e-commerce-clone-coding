package io.github.metdaisy.amaazon.auth.presentation.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationExceptionEntryPoint 테스트")
class AuthenticationExceptionEntryPointTest {

  @Mock
  private ObjectMapper mapper;

  @InjectMocks
  private AuthenticationExceptionEntryPoint entryPoint;

  @Test
  @DisplayName("commence - 미인증 접근 시 401 응답과 에러 메시지를 반환한다")
  void commence_success() throws Exception {
    // given
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    PrintWriter writer = Mockito.mock(PrintWriter.class);
    AuthenticationException exception = Mockito.mock(AuthenticationException.class);

    Mockito.when(response.getWriter()).thenReturn(writer);

    // when
    entryPoint.commence(request, response, exception);

    // then
    verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    verify(mapper).writeValue(eq(writer), eq(Map.of("status", 401, "message", "인증이 필요합니다.")));
  }
}
