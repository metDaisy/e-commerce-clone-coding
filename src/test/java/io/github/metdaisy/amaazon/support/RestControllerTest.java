package io.github.metdaisy.amaazon.support;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.global.exception.ApiExceptionHandler;
import io.github.metdaisy.amaazon.global.exception.SecurityExceptionHandler;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import io.github.metdaisy.amaazon.global.web.config.WebMvcConfig;
import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import({ApiExceptionHandler.class, SecurityExceptionHandler.class, ExceptionStrategyFactory.class,
    WebMvcConfig.class})
public abstract class RestControllerTest {

  protected static final String API_PREFIX = WebConstants.SERVLET_PREFIX;
  protected static final UUID USER_ID =
      UUID.fromString("2bb8df7f-9478-4d51-b055-496016dd421f");

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @BeforeEach
  void setUpRestControllerTest() {
    authenticateAs(USER_ID);
  }

  @AfterEach
  void tearDownRestControllerTest() {
    SecurityContextHolder.clearContext();
  }

  protected void authenticateAs(UUID userId) {
    AmaazonPrincipal principal = mock(AmaazonPrincipal.class);
    given(principal.getId()).willReturn(userId);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null));
  }

  protected MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
    return post(url)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }
}
