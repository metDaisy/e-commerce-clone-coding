package io.github.metdaisy.amaazon.auth.presentation.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.JwtTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.JwtCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class FormLoginSuccessHandler extends AbstractLoginSuccessHandler {

  private final ObjectMapper mapper;

  public FormLoginSuccessHandler(JwtTokenService jwtTokenService,
          JwtCookieProvider jwtCookieProvider, ObjectMapper mapper) {
    super(jwtTokenService, jwtCookieProvider);
    this.mapper = mapper;
  }

  @Override
  protected String getDeviceId(HttpServletRequest request) {
    return request.getHeader("X-Device-Id");
  }

  @Override
  protected void onSuccess(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication, JwtLoginDto loginDto) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(response.getWriter(), loginDto);
  }
}
